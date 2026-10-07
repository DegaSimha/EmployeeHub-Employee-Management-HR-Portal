package com.employeehub;

import com.employeehub.entity.*;
import com.employeehub.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class EmployeeHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmployeeHubApplication.class, args);
    }

    @Bean
    CommandLineRunner seedData(DepartmentRepository departments, EmployeeRepository employees,
                               UserRepository users, AttendanceRepository attendance,
                               LeaveRepository leaves, com.employeehub.repository.CompanyHolidayRepository holidays,
                               PasswordEncoder encoder,
                               PlatformTransactionManager transactionManager,
                               @Value("${app.seed.demo-data:true}") boolean seedDemoData,
                               @Value("${app.seed.admin-email:admin@employeehub.com}") String adminEmail,
                               @Value("${app.seed.admin-password:Admin@123}") String adminPassword,
                               @Value("${app.seed.employee-password:Employee@123}") String employeePassword) {
        return args -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            seedDepartment(departments, "Engineering", "Product engineering and technology", "Arjun Reddy");
            seedDepartment(departments, "People & Culture", "People operations and talent", "Priya Sharma");
            seedDepartment(departments, "Finance", "Financial planning and operations", "Kiran Rao");
            seedDepartment(departments, "Sales", "Customer growth and partnerships", "Rohit Verma");
            seedDepartment(departments, "Marketing", "Brand, communications and growth", "Neha Gupta");
            seedDepartment(departments, "Operations", "Business operations and workplace", "Ananya Singh");
            seedDepartment(departments, "Customer Support", "Customer care and product support", "Vikram Das");
            seedDepartment(departments, "Administration", "Company administration and facilities", "Pooja Nair");
            seedHoliday(holidays, "New Year's Day", LocalDate.of(LocalDate.now().getYear(), Month.JANUARY, 1), "Start of the calendar year.");
            seedHoliday(holidays, "Independence Day", LocalDate.of(LocalDate.now().getYear(), Month.JULY, 4), "Company holiday.");
            seedHoliday(holidays, "Labor Day", LocalDate.of(LocalDate.now().getYear(), Month.SEPTEMBER, 7), "Company holiday.");
            seedHoliday(holidays, "Winter Break", LocalDate.of(LocalDate.now().getYear(), Month.DECEMBER, 25), "Company winter break.");
            if (users.existsByEmail(adminEmail)) return;
            if (!seedDemoData) {
                if (adminPassword.length() < 16) {
                    throw new IllegalStateException("ADMIN_PASSWORD must contain at least 16 characters when SEED_DEMO_DATA is false.");
                }
                users.save(new UserAccount(adminEmail, encoder.encode(adminPassword), Role.ADMIN, null));
                return;
            }
            Department engineering = departments.findByNameIgnoreCase("Engineering").orElseThrow();
            Department people = departments.findByNameIgnoreCase("People & Culture").orElseThrow();
            Department finance = departments.findByNameIgnoreCase("Finance").orElseThrow();
            Department sales = departments.findByNameIgnoreCase("Sales").orElseThrow();
            String[][] data = {
                    {"EMP001","Rahul","Kumar","rahul.kumar@employeehub.com","Senior Software Engineer",engineering.getId().toString()},
                    {"EMP002","Priya","Sharma","priya.sharma@employeehub.com","People Operations Manager",people.getId().toString()},
                    {"EMP003","Arjun","Reddy","arjun.reddy@employeehub.com","Engineering Manager",engineering.getId().toString()},
                    {"EMP004","Sneha","Patel","sneha.patel@employeehub.com","Product Designer",engineering.getId().toString()},
                    {"EMP005","Kiran","Rao","kiran.rao@employeehub.com","Financial Analyst",finance.getId().toString()},
                    {"EMP006","Ananya","Singh","ananya.singh@employeehub.com","HR Specialist",people.getId().toString()},
                    {"EMP007","Rohit","Verma","rohit.verma@employeehub.com","Account Executive",sales.getId().toString()},
                    {"EMP008","Neha","Gupta","neha.gupta@employeehub.com","QA Engineer",engineering.getId().toString()},
                    {"EMP009","Vikram","Das","vikram.das@employeehub.com","Sales Associate",sales.getId().toString()},
                    {"EMP010","Pooja","Nair","pooja.nair@employeehub.com","Payroll Specialist",finance.getId().toString()}
            };
            Employee[] seeded = new Employee[data.length];
            for (int i = 0; i < data.length; i++) {
                String[] row = data[i];
                Department department = departments.findById(Long.valueOf(row[5])).orElseThrow();
                Employee employee = new Employee(row[0], row[1], row[2], row[3],
                        "+1-555-010" + (i + 1), LocalDate.of(1990 + i % 8, 2 + i % 9, 5 + i % 20),
                        Gender.values()[i % Gender.values().length], department, row[4],
                        new BigDecimal(68000 + i * 2500), LocalDate.now().minusMonths(4L + i * 3L),
                        EmploymentType.FULL_TIME, EmployeeStatus.ACTIVE,
                        (100 + i) + " Market Street", "San Francisco", "California", "United States");
                seeded[i] = employees.save(employee);
                users.save(new UserAccount(row[3], encoder.encode(employeePassword), Role.EMPLOYEE, employee));
            }
            users.save(new UserAccount(adminEmail, encoder.encode(adminPassword), Role.ADMIN, null));
            for (int i = 0; i < seeded.length; i++) {
                attendance.save(new AttendanceRecord(seeded[i], LocalDate.now().minusDays(i % 4),
                        LocalTime.of(9, i % 3 * 5), LocalTime.of(17, 30 + i % 3 * 10),
                        AttendanceStatus.values()[i % 4]));
            }
            leaves.save(new LeaveRequest(seeded[0], LeaveType.ANNUAL, LocalDate.now().plusDays(12),
                    LocalDate.now().plusDays(14), "Family trip"));
            leaves.save(new LeaveRequest(seeded[3], LeaveType.SICK, LocalDate.now().minusDays(1),
                    LocalDate.now(), "Recovery and rest"));
            leaves.save(new LeaveRequest(seeded[6], LeaveType.CASUAL, LocalDate.now().plusDays(20),
                    LocalDate.now().plusDays(20), "Personal appointment"));
        });
    }

    private void seedHoliday(com.employeehub.repository.CompanyHolidayRepository holidays,
                             String name, LocalDate date, String description) {
        if (!holidays.existsByNameIgnoreCase(name)) {
            holidays.save(new CompanyHoliday(name, date, description, true));
        }
    }

    private void seedDepartment(DepartmentRepository departments, String name, String description, String manager) {
        if (!departments.existsByNameIgnoreCase(name)) {
            departments.save(new Department(name, description, manager));
        }
    }
}
