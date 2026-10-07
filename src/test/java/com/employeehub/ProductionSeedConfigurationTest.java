package com.employeehub;

import com.employeehub.repository.EmployeeRepository;
import com.employeehub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employeehub-production-seed;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seed.demo-data=false",
        "app.seed.admin-email=deployment-admin@example.com",
        "app.seed.admin-password=DeploymentAdminPassword-123!"
})
@AutoConfigureMockMvc
class ProductionSeedConfigurationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired EmployeeRepository employees;

    @Test
    void createsConfiguredAdministratorWithoutDemoEmployees() throws Exception {
        assertEquals(1, users.count());
        assertEquals(0, employees.count());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"deployment-admin@example.com","password":"DeploymentAdminPassword-123!"}
                                """))
                .andExpect(status().isOk());
    }
}
