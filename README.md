# EmployeeHub — Employee Management & HR Portal

EmployeeHub is a Java full-stack HR portal for managing employee records, departments, attendance, leave requests and people reports. The browser application is served by Spring Boot and uses the same-origin REST API; operational data is stored in MySQL.

## Features

- JWT sign-in with BCrypt-hashed account passwords and ADMIN / EMPLOYEE roles
- Dashboard statistics, department distribution and recently added employees/leave requests
- Employee directory with backend search, status/department filters and pagination
- Employee and department create, read, update and delete flows
- Attendance history, date filtering, admin record management, and employee self-service check-in/check-out
- Employee leave requests, pending-request withdrawal, and admin approval/decline workflow
- Employee self-service profile, company holiday calendar, HR reports and CSV export
- Self-service password change and HR password reset when editing an employee
- Responsive sidebar, mobile layout, loading/error/empty states, and browser theme preference
- Seeded demo accounts, employees, departments, attendance, leave requests and company holidays

## Technology and architecture

- Java 17+, Spring Boot 3.4, Spring Web, Spring Security, Spring Data JPA/Hibernate, Jakarta Validation, Maven
- MySQL 8+ for application data
- HTML, CSS and vanilla JavaScript (no React or frontend build step)
- JUnit 5, Spring Boot Test, MockMvc and an in-memory H2 test database

REST controllers validate and delegate to services; repositories persist JPA entities. DTOs keep entity/password fields out of API responses. JWT bearer tokens are stateless. The static frontend uses a shared `Api` helper for authenticated requests.

Main code is under `src/main/java/com/employeehub` (`controller`, `dto`, `entity`, `exception`, `repository`, `security`, and `service`). Browser assets are in `src/main/resources/static`.

## Database

The app manages six MySQL tables:

| Table | Purpose |
| --- | --- |
| `users` | Email, BCrypt password hash, role and optional employee-account link |
| `employees` | Employee code, contact details, department, job, salary and employment status |
| `departments` | Department name, description, manager and active flag |
| `attendance` | Employee/date check-in, check-out and attendance status |
| `leaves` | Employee leave request, inclusive calendar-day count, decision and HR comment |
| `company_holidays` | Shared holiday name, date, description and paid/unpaid designation |

Employees belong to a department; each attendance and leave record belongs to one employee; a user account may be linked to one employee. The SQL definitions and foreign keys/indexes are in [database/schema.sql](./database/schema.sql). Hibernate creates/updates the entities on application startup (`spring.jpa.hibernate.ddl-auto=update`), so it can create missing application tables after the database exists.

Create the database and application tables by running `database/schema.sql` in MySQL Workbench or from PowerShell:

```powershell
Get-Content database\schema.sql | mysql -u root -p
```

Then create a least-privilege local account:

```sql
CREATE USER 'employeehub'@'localhost' IDENTIFIED BY 'choose-a-local-password';
GRANT ALL PRIVILEGES ON employeehub.* TO 'employeehub'@'localhost';
```

If the MySQL command-line client is unavailable, run the SQL file in MySQL Workbench. On startup, the application also creates any missing application tables from the JPA entity definitions and seeds initial company data. The schema file creates the tables only; it does not contain demo users or passwords.

## Run locally

Prerequisites: JDK 17+, Maven 3.9+, and MySQL 8+ running locally.

1. Create the database and account above.
2. Copy `.env.example` to `.env` in the project root. Edit the local `.env` and set your MySQL account and a unique JWT secret (the `.env` file is ignored by Git):

   ```powershell
   Copy-Item .env.example .env
   ```

   The app imports this local properties file at startup. You can alternatively set these variables in the shell or deployment environment.
3. From the project folder:

   ```powershell
   mvn spring-boot:run
   ```

4. Open [http://localhost:8081](http://localhost:8081). On the first successful database startup, the application creates demo data. It does not overwrite an existing database.

### Demo accounts

| Role | Email | Password |
| --- | --- | --- |
| Administrator | `admin@employeehub.com` | `Admin@123` |
| Employee | `rahul.kumar@employeehub.com` | `Employee@123` |
| Seeded employees | Individual seeded employee email | `Employee@123` |

New employee creation requires HR to set an initial sign-in password. Passwords are stored using BCrypt, never returned by APIs.

## Security and configuration notes

- Set a unique random `JWT_SECRET` with at least 32 bytes for every environment; the application refuses to start without it.
- The seeded demo credentials are for local demonstration only. Disable or replace the seed account/passwords before deployment.
- `JWT_SECRET` is required; the app intentionally has no hard-coded fallback secret.
- JWTs are held in browser `sessionStorage` and sent using the `Authorization: Bearer` header. This is appropriate for a portfolio demo, not a substitute for a production threat review; production deployments should consider an HttpOnly, Secure, SameSite cookie and CSRF strategy.
- CORS defaults to `http://localhost:8081`; if hosting the frontend separately, set `CORS_ALLOWED_ORIGIN` to the exact trusted origin rather than `*`.
- Configure HTTPS, secret management, backups and database permissions before deployment.

## API overview

All APIs are under `/api`; all except login require a bearer token.

| Area | Methods and routes | Access |
| --- | --- | --- |
| Authentication | `POST /auth/login` | Public |
| Employees | `GET /employees?page=0&size=10&keyword=&departmentId=&status=&designation=&sort=&direction=`, `GET /employees/{id}`, `POST /employees`, `PUT /employees/{id}`, `DELETE /employees/{id}` | Admin |
| Departments | `GET /departments`, `GET /departments/{id}`, `POST /departments`, `PUT /departments/{id}`, `DELETE /departments/{id}` | Read: signed-in; changes: admin |
| Attendance | `GET /attendance?from=&to=&employeeId=&status=`, `GET /attendance/{id}`, `GET /attendance/employee/{employeeId}`, `POST /attendance`, `PUT /attendance/{id}`, `GET /attendance/mine`, `POST /attendance/mine/check-in`, `POST /attendance/mine/check-out` | Admin manages records; employees check themselves in/out and read only `/mine` |
| Leave | `GET /leaves`, `GET /leaves/{id}`, `POST /leaves`, `GET /leaves/mine`, `DELETE /leaves/mine/{id}`, `PUT /leaves/{id}/approve`, `PUT /leaves/{id}/reject` | Admin reviews all; employees manage only their own requests |
| Dashboard | `GET /dashboard/statistics`, `/dashboard/recent-employees`, `/dashboard/recent-leaves` | Admin |
| Profile | `GET /profile`, `PUT /profile`, `PUT /profile/password` | Signed-in user; limited contact fields and verified current-password change |
| Company calendar | `GET /holidays`, `GET /holidays/{id}`, `POST /holidays`, `PUT /holidays/{id}`, `DELETE /holidays/{id}` | Read: signed-in; changes: admin |
| Reports | `GET /reports/employees`, `/reports/departments`, `/reports/attendance`, `/reports/leaves` | Admin |

Employee self-service attendance records the server time and date. The default late threshold is 09:15 local server time; configure it using `ATTENDANCE_LATE_AFTER` (24-hour `HH:mm`) if your organization uses another start time.

Successful employee list responses use a stable page shape (`content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last`). Errors return JSON with timestamp, HTTP status, message and request path.

## Tests and API collection

Run the backend build and integration tests:

```powershell
mvn test
```

The tests use H2 and do not need a running MySQL server. Import `postman/EmployeeHub.postman_collection.json` into Postman; the login request saves the admin JWT into the collection's `token` variable.

## Screenshots

Add screenshots here after running the application, for example `docs/screenshots/dashboard.png`.

## Future enhancements

- Account invitation and an administrator-assisted password recovery workflow
- Audit history and soft-delete retention for HR records
- More granular attendance reports, working-day calendars and leave balances
- Deployment profiles, database migrations and automated browser tests
