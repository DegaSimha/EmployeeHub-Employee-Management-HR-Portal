package com.employeehub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.employeehub.entity.Department;
import com.employeehub.repository.DepartmentRepository;
import com.employeehub.repository.CompanyHolidayRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeHubIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired DepartmentRepository departments;
    @Autowired CompanyHolidayRepository holidays;

    @Test
    void seedsDemoDataAndProtectsEmployeeDirectory() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<!doctype html>")));
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
        String admin = login("admin@employeehub.com", "Admin@123");
        mvc.perform(get("/api/employees").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(10));
        mvc.perform(get("/api/employees").param("designation", "software").param("sort", "department.name")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        String employee = login("rahul.kumar@employeehub.com", "Employee@123");
        mvc.perform(get("/api/employees").header("Authorization", "Bearer " + employee))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/attendance/mine").header("Authorization", "Bearer " + employee))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/holidays").header("Authorization", "Bearer " + employee))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)));
        mvc.perform(post("/api/holidays").header("Authorization", "Bearer " + employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unauthorized Holiday\",\"holidayDate\":\"2026-10-20\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createsAndDeletesEmployeeAndPersistsLeaveDecisions() throws Exception {
        String admin = login("admin@employeehub.com", "Admin@123");
        Department department = departments.findAll().get(0);
        String employeeJson = """
                {"firstName":"Mira","lastName":"Test","email":"mira.test@employeehub.com",
                 "phone":"+1-555-0199","departmentId":%d,"designation":"People Partner",
                 "salary":72000,"joiningDate":"%s","employmentType":"FULL_TIME",
                 "status":"ACTIVE","initialPassword":"MiraSecret123"}
                """.formatted(department.getId(), LocalDate.now());
        MvcResult created = mvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(employeeJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeCode").exists())
                .andReturn();
        long employeeId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        String updateJson = """
                {"firstName":"Mira","lastName":"Test","email":"mira.test@employeehub.com",
                 "phone":"+1-555-0199","departmentId":%d,"designation":"Senior People Partner",
                 "salary":74000,"joiningDate":"%s","employmentType":"FULL_TIME","status":"ACTIVE",
                 "initialPassword":"MiraReset123"}
                """.formatted(department.getId(), LocalDate.now());
        mvc.perform(put("/api/employees/" + employeeId).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(updateJson))
                .andExpect(status().isOk()).andExpect(jsonPath("$.designation").value("Senior People Partner"));
        String attendanceJson = """
                {"employeeId":%d,"attendanceDate":"%s","checkIn":"09:00:00","checkOut":"17:00:00","status":"PRESENT"}
                """.formatted(employeeId, LocalDate.now().plusDays(1));
        mvc.perform(post("/api/attendance").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(attendanceJson))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.workingHours").value("8h 0m"));
        String departmentJson = """
                {"name":"Integration Team","description":"Test department","managerName":"Test Manager","active":true}
                """;
        MvcResult departmentResult = mvc.perform(post("/api/departments").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(departmentJson))
                .andExpect(status().isCreated()).andReturn();
        long testDepartmentId = mapper.readTree(departmentResult.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(put("/api/departments/" + testDepartmentId).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Integration Team Updated\",\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        String employeeToken = login("mira.test@employeehub.com", "MiraReset123");
        mvc.perform(post("/api/attendance/mine/check-in").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value(employeeId));
        mvc.perform(post("/api/attendance/mine/check-in").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/attendance/mine/check-out").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.checkOut").isNotEmpty());
        mvc.perform(post("/api/attendance/mine/check-out").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/attendance/mine/check-in").header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
        String withdrawJson = """
                {"leaveType":"CASUAL","startDate":"%s","endDate":"%s","reason":"Schedule changed"}
                """.formatted(LocalDate.now().plusDays(40), LocalDate.now().plusDays(40));
        MvcResult withdrawResult = mvc.perform(post("/api/leaves").header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON).content(withdrawJson))
                .andExpect(status().isCreated()).andReturn();
        long withdrawId = mapper.readTree(withdrawResult.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(delete("/api/leaves/mine/" + withdrawId).header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/leaves/mine/" + withdrawId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
        String leaveJson = """
                {"leaveType":"ANNUAL","startDate":"%s","endDate":"%s","reason":"Planned break"}
                """.formatted(LocalDate.now().plusDays(20), LocalDate.now().plusDays(22));
        MvcResult leaveResult = mvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON).content(leaveJson))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.days").value(3))
                .andExpect(jsonPath("$.status").value("PENDING")).andReturn();
        JsonNode leave = mapper.readTree(leaveResult.getResponse().getContentAsString());
        mvc.perform(post("/api/leaves").header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON).content(leaveJson))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/leaves/" + leave.get("id").asLong() + "/approve")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(put("/api/profile/password").header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"MiraReset123\",\"newPassword\":\"MiraUpdated123\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mira.test@employeehub.com\",\"password\":\"MiraUpdated123\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mira.test@employeehub.com\",\"password\":\"MiraSecret123\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/employees/" + employeeId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/departments/" + testDepartmentId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/holidays").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Integration Holiday\",\"holidayDate\":\"2026-10-20\",\"paid\":true}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.paid").value(true));
        long holidayId = holidays.findAll().stream().filter(h -> h.getName().equals("Integration Holiday"))
                .findFirst().orElseThrow().getId();
        mvc.perform(put("/api/holidays/" + holidayId).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Integration Holiday Updated\",\"holidayDate\":\"2026-10-21\",\"paid\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paid").value(false));
        mvc.perform(delete("/api/holidays/" + holidayId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/employees/" + employeeId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
