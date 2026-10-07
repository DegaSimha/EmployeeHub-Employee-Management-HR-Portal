(() => {
  const $ = selector => document.querySelector(selector);
  const content = $("#page-content");
  const modal = $("#app-modal");
  const modalContent = $("#modal-content");
  const labels = { dashboard: "Overview", employees: "People", departments: "Departments", attendance: "Attendance", leaves: "Time off", holidays: "Company calendar", reports: "Reports", profile: "My profile", settings: "Settings" };
  let employeePage = 0;
  let currentEmployeeFilters = { keyword: "", departmentId: "", status: "", designation: "", sort: "createdAt", direction: "desc" };
  let attendanceFilters = { from: "", to: "", employeeId: "", status: "" };

  function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
  }
  function initials(name) { return String(name || "EH").split(/\s+/).filter(Boolean).slice(0, 2).map(part => part[0]).join("").toUpperCase(); }
  function date(value) {
    if (!value) return "—";
    return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
  }
  function localDateValue(value = new Date()) {
    const offset = value.getTimezoneOffset() * 60000;
    return new Date(value.getTime() - offset).toISOString().slice(0, 10);
  }
  function money(value) { return new Intl.NumberFormat(undefined, { style: "currency", currency: "USD", maximumFractionDigits: 0 }).format(Number(value || 0)); }
  function toast(message, error = false) {
    const item = document.createElement("div");
    item.className = `toast${error ? " error" : ""}`;
    item.textContent = message;
    $("#toast-region").append(item);
    setTimeout(() => item.remove(), 4000);
  }
  function badge(value) {
    const normalized = String(value || "unknown").toLowerCase();
    return `<span class="badge badge-${escapeHtml(normalized)}">${escapeHtml(String(value || "—").replaceAll("_", " "))}</span>`;
  }
  function pageHeading(kicker, title, description, action = "") {
    return `<div class="page-heading"><div><span class="eyebrow">${escapeHtml(kicker)}</span><h1>${escapeHtml(title)}</h1><p>${escapeHtml(description)}</p></div>${action ? `<div class="heading-actions">${action}</div>` : ""}</div>`;
  }
  function setNav(page) {
    $("#current-section").textContent = labels[page] || "Overview";
    const user = Api.getUser();
    document.querySelectorAll("[data-page]").forEach(link => {
      link.classList.toggle("active", link.dataset.page === page);
      if (link.hasAttribute("data-admin")) link.hidden = user?.role !== "ADMIN";
    });
    $("#side-name").textContent = user?.name || "EmployeeHub";
    $("#side-role").textContent = user?.role === "ADMIN" ? "Administrator" : "Team member";
    $("#side-avatar").textContent = initials(user?.name);
    $("#top-avatar").textContent = initials(user?.name);
    $("#today-label").textContent = new Intl.DateTimeFormat(undefined, { weekday: "short", month: "short", day: "numeric" }).format(new Date());
    const mobile = $("#sidebar");
    mobile.classList.remove("open");
  }
  async function load(page) {
    if (!Api.isAuthenticated()) {
      $("#workspace").hidden = true;
      $("#login-view").hidden = false;
      return;
    }
    $("#login-view").hidden = true;
    $("#workspace").hidden = false;
    if (!labels[page]) page = "dashboard";
    setNav(page);
    content.innerHTML = '<div class="loading-state">Loading your workspace…</div>';
    try {
      if (page === "dashboard") await renderDashboard();
      if (page === "employees") await renderEmployees();
      if (page === "departments") await renderDepartments();
      if (page === "attendance") await renderAttendance();
      if (page === "leaves") await renderLeaves();
      if (page === "holidays") await renderHolidays();
      if (page === "reports") await renderReports();
      if (page === "profile") await renderProfile();
      if (page === "settings") renderSettings();
    } catch (error) {
      content.innerHTML = `<section class="panel empty-state"><h3>We couldn’t load this page</h3><p>${escapeHtml(error.message)}</p><button class="button button-secondary" data-action="retry">Try again</button></section>`;
    }
  }
  function currentPage() { return window.location.hash.replace(/^#\/?/, "") || "dashboard"; }
  function greeting() {
    const hour = new Date().getHours();
    return hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
  }

  async function renderDashboard() {
    const user = Api.getUser();
    if (user.role !== "ADMIN") {
      const [profile, attendance, leaves, departments] = await Promise.all([
        Api.get("profile"), Api.get("attendance/mine"), Api.get("leaves/mine"), Api.get("departments")
      ]);
      const today = localDateValue();
      const todayRecord = attendance.find(item => item.attendanceDate === today);
      const pending = leaves.filter(item => item.status === "PENDING").length;
      const attendanceAction = !todayRecord?.checkIn ? '<button class="button button-primary" data-action="check-in">Check in</button>'
        : !todayRecord.checkOut ? '<button class="button button-secondary" data-action="check-out">Check out</button>'
          : '<span class="badge badge-approved">DAY COMPLETE</span>';
      content.innerHTML = `${pageHeading("YOUR WORKSPACE", `Welcome back, ${profile.firstName}`, "Here’s your personal work snapshot.", attendanceAction)}<section class="stat-grid">
        ${stat("My attendance", todayRecord ? todayRecord.status.replaceAll("_", " ") : "Not recorded", todayRecord ? `Today · ${todayRecord.checkIn || "No check-in"}` : "Nothing recorded today", "◷", "blue")}
        ${stat("Time-off requests", leaves.length, `${pending} waiting for a decision`, "▤", "amber")}
        ${stat("Departments", departments.length, "Across the organization", "▦", "violet")}
        ${stat("My role", "Team member", profile.designation || "Employee", "◉", "green")}
      </section><section class="dashboard-grid"><div class="panel"><div class="panel-head"><div><h3>My recent attendance</h3><p>Your attendance history</p></div><a class="text-link" href="#attendance">View all →</a></div>${attendanceTable(attendance.slice(0, 6))}</div><div class="panel"><div class="panel-head"><div><h3>My time off</h3><p>Latest requests and updates</p></div><a class="text-link" href="#leaves">View all →</a></div>${leaveMiniList(leaves.slice(0, 5))}</div></section>`;
      return;
    }
    const [stats, recent, leaves] = await Promise.all([
      Api.get("dashboard/statistics"), Api.get("dashboard/recent-employees"), Api.get("dashboard/recent-leaves")
    ]);
    $("#employee-nav-count").textContent = stats.totalEmployees;
    $("#leave-nav-dot").hidden = stats.pendingLeaves === 0;
    const activeDepartments = stats.departmentDistribution.filter(item => item.count > 0);
    const max = Math.max(1, ...activeDepartments.map(item => item.count));
    const departments = activeDepartments.length
      ? activeDepartments.map(item => `<div class="distribution-row"><span class="distribution-name">${escapeHtml(item.department)}</span><div class="progress-track"><div class="progress-fill" style="width:${Math.max(4, item.count / max * 100)}%"></div></div><span class="distribution-value">${item.count}</span></div>`).join("")
      : '<div class="empty-state">No employees to distribute yet. Add your first team member to get started.</div>';
    const todayLabel = `${new Intl.DateTimeFormat(undefined, { weekday: "long" }).format(new Date()).toUpperCase()}, YOUR WAY`;
    content.innerHTML = `${pageHeading(todayLabel, greeting(), "Here’s what’s happening across your team today.", '<button class="button button-primary" data-action="new-employee">＋ Add a person</button>')}
      <section class="stat-grid">${stat("Total people", stats.totalEmployees, "In your organization", "♙", "blue")}${stat("Active people", stats.activeEmployees, `${stats.inactiveEmployees} inactive`, "◉", "green")}${stat("Departments", stats.departments, "Teams working together", "▦", "violet")}${stat("Pending time off", stats.pendingLeaves, stats.pendingLeaves ? "Requests need your review" : "You’re all caught up", "▤", "amber")}</section>
      <section class="dashboard-grid"><div class="panel"><div class="panel-head"><div><h3>Team distribution</h3><p>People across your departments</p></div><a class="text-link" href="#departments">Departments →</a></div><div class="distribution">${departments}</div></div>
      <div class="panel"><div class="panel-head"><div><h3>Recently added</h3><p>New faces on the team</p></div><a class="text-link" href="#employees">All people →</a></div><div class="recent-list">${recent.length ? recent.map(person => `<div class="recent-person"><div class="mini-avatar">${initials(`${person.firstName} ${person.lastName}`)}</div><div class="recent-person-info"><strong>${escapeHtml(person.firstName)} ${escapeHtml(person.lastName)}</strong><small>${escapeHtml(person.designation)}</small></div><span class="recent-date">${date(person.joiningDate)}</span></div>`).join("") : '<div class="empty-state">No people added yet.</div>'}</div></div></section>
      <section class="panel table-panel"><div class="panel-head"><div><h3>Time-off requests</h3><p>Latest requests from your team</p></div><a class="text-link" href="#leaves">Review requests →</a></div>${leaveMiniList(leaves)}</section>`;
  }
  function stat(label, value, note, icon, tone) {
    return `<article class="stat-card"><div class="stat-top">${escapeHtml(label)}<span class="stat-icon tone-${tone}">${icon}</span></div><div class="stat-value">${escapeHtml(value)}</div><div class="stat-foot">${escapeHtml(note)}</div></article>`;
  }
  function leaveMiniList(items) {
    if (!items.length) return '<div class="empty-state">No time-off requests yet.</div>';
    return `<div class="table-wrap"><table><thead><tr><th>Person</th><th>Dates</th><th>Type</th><th>Status</th></tr></thead><tbody>${items.map(item => `<tr><td><span class="person-cell"><span class="mini-avatar">${initials(item.employeeName)}</span>${escapeHtml(item.employeeName)}</span></td><td>${date(item.startDate)} – ${date(item.endDate)}</td><td>${escapeHtml(item.leaveType)}</td><td>${badge(item.status)}</td></tr>`).join("")}</tbody></table></div>`;
  }

  async function renderEmployees() {
    const user = Api.getUser();
    if (user.role !== "ADMIN") throw new Error("You don’t have permission to view the people directory.");
    const params = new URLSearchParams({ page: String(employeePage), size: "10", sort: currentEmployeeFilters.sort, direction: currentEmployeeFilters.direction });
    if (currentEmployeeFilters.keyword) params.set("keyword", currentEmployeeFilters.keyword);
    if (currentEmployeeFilters.departmentId) params.set("departmentId", currentEmployeeFilters.departmentId);
    if (currentEmployeeFilters.status) params.set("status", currentEmployeeFilters.status);
    if (currentEmployeeFilters.designation) params.set("designation", currentEmployeeFilters.designation);
    const [page, departments] = await Promise.all([Api.get(`employees?${params}`), Api.get("departments")]);
    const rows = page.content.length ? page.content.map(employee => `<tr><td><strong>${escapeHtml(employee.employeeCode)}</strong></td><td><span class="person-cell"><span class="mini-avatar">${initials(`${employee.firstName} ${employee.lastName}`)}</span><span>${escapeHtml(employee.firstName)} ${escapeHtml(employee.lastName)}<span class="sub-cell">${escapeHtml(employee.email)}</span></span></span></td><td>${escapeHtml(employee.phone)}</td><td>${escapeHtml(employee.departmentName)}</td><td>${escapeHtml(employee.designation)}</td><td>${date(employee.joiningDate)}</td><td>${badge(employee.status)}</td><td><div class="actions-cell"><button class="row-action" data-action="view-employee" data-id="${employee.id}" aria-label="View employee">View</button><button class="row-action" data-action="edit-employee" data-id="${employee.id}" aria-label="Edit employee">Edit</button><button class="row-action delete" data-action="delete-employee" data-id="${employee.id}" data-name="${escapeHtml(employee.firstName)} ${escapeHtml(employee.lastName)}" aria-label="Delete employee">Delete</button></div></td></tr>`).join("") : "";
    content.innerHTML = `${pageHeading("YOUR PEOPLE", "People directory", "Find, support and grow the people on your team.", '<button class="button button-primary" data-action="new-employee">＋ Add a person</button>')}
      <section class="panel table-panel"><div class="panel-head"><div><h3>All people <span class="sub-cell" style="display:inline">· ${page.totalElements} total</span></h3></div></div>
      <form id="employee-filters" class="toolbar"><label class="search-box"><span>⌕</span><input name="keyword" value="${escapeHtml(currentEmployeeFilters.keyword)}" placeholder="Search name, email, team…" aria-label="Search people"></label>
      <select class="filter-control" name="departmentId"><option value="">All departments</option>${departments.map(d => `<option value="${d.id}" ${String(d.id) === currentEmployeeFilters.departmentId ? "selected" : ""}>${escapeHtml(d.name)}</option>`).join("")}</select>
      <select class="filter-control" name="status"><option value="">All statuses</option>${["ACTIVE","INACTIVE","ON_LEAVE"].map(status => `<option ${status === currentEmployeeFilters.status ? "selected" : ""} value="${status}">${status.replaceAll("_", " ")}</option>`).join("")}</select>
      <input class="filter-control" name="designation" value="${escapeHtml(currentEmployeeFilters.designation)}" placeholder="Job title">
      <select class="filter-control" name="sort"><option value="createdAt" ${currentEmployeeFilters.sort === "createdAt" ? "selected" : ""}>Recently added</option><option value="firstName" ${currentEmployeeFilters.sort === "firstName" ? "selected" : ""}>Name</option><option value="joiningDate" ${currentEmployeeFilters.sort === "joiningDate" ? "selected" : ""}>Joining date</option><option value="department.name" ${currentEmployeeFilters.sort === "department.name" ? "selected" : ""}>Department</option><option value="salary" ${currentEmployeeFilters.sort === "salary" ? "selected" : ""}>Salary</option><option value="status" ${currentEmployeeFilters.sort === "status" ? "selected" : ""}>Status</option></select>
      <select class="filter-control" name="direction"><option value="desc" ${currentEmployeeFilters.direction === "desc" ? "selected" : ""}>Descending</option><option value="asc" ${currentEmployeeFilters.direction === "asc" ? "selected" : ""}>Ascending</option></select><button class="button button-secondary button-small" type="submit">Apply</button></form>
      <div class="table-wrap"><table><thead><tr><th>Employee</th><th>Name & email</th><th>Phone</th><th>Department</th><th>Designation</th><th>Joined</th><th>Status</th><th>Actions</th></tr></thead><tbody>${rows || '<tr><td colspan="8" class="empty-state">No people match your search.</td></tr>'}</tbody></table></div>
      ${pagination(page)}</section>`;
    $("#employee-nav-count").textContent = page.totalElements;
  }
  function pagination(page) {
    return `<div class="pagination"><span>Showing ${page.totalElements ? page.number * page.size + 1 : 0}–${Math.min((page.number + 1) * page.size, page.totalElements)} of ${page.totalElements}</span><div class="page-buttons"><button class="page-button" data-action="page" data-page="${Math.max(0, page.number - 1)}" ${page.first ? "disabled" : ""}>← Previous</button>${Array.from({ length: page.totalPages }, (_, i) => `<button class="page-button ${i === page.number ? "current" : ""}" data-action="page" data-page="${i}">${i + 1}</button>`).join("")}<button class="page-button" data-action="page" data-page="${page.number + 1}" ${page.last ? "disabled" : ""}>Next →</button></div></div>`;
  }
  async function openEmployeeForm(id) {
    const [departments, employee] = await Promise.all([
      Api.get("departments"), id ? Api.get(`employees/${id}`) : Promise.resolve(null)
    ]);
    modalContent.innerHTML = `<span class="eyebrow">${id ? "PEOPLE DIRECTORY" : "GROW YOUR TEAM"}</span><h2>${id ? "Update person" : "Add a person"}</h2><p class="modal-description">Keep their information accurate and up to date.</p>
      <form id="employee-form" data-id="${id || ""}"><div class="form-grid">
      ${field("firstName", "First name", "text", employee?.firstName, true)}${field("lastName", "Last name", "text", employee?.lastName, true)}
      ${field("email", "Work email", "email", employee?.email, true)}${field("phone", "Phone", "tel", employee?.phone, true)}
      ${field("dateOfBirth", "Date of birth", "date", employee?.dateOfBirth)}<label class="field">Gender<select name="gender">${selectOptions(["FEMALE","MALE","NON_BINARY","PREFER_NOT_TO_SAY"], employee?.gender)}</select></label>
      <label class="field">Department<select name="departmentId" required>${departments.map(d => `<option value="${d.id}" ${employee?.departmentId === d.id ? "selected" : ""}>${escapeHtml(d.name)}</option>`).join("")}</select></label>
      ${field("designation", "Job title", "text", employee?.designation, true)}${field("salary", "Annual salary", "number", employee?.salary, true, 'min="0" step="0.01"')}
      ${field("initialPassword", id ? "Reset sign-in password (leave blank to keep current)" : "Initial sign-in password", "password", "", !id, 'minlength="8" autocomplete="new-password"')}
      ${field("joiningDate", "Joining date", "date", employee?.joiningDate, true)}<label class="field">Employment type<select name="employmentType">${selectOptions(["FULL_TIME","PART_TIME","CONTRACT","INTERN"], employee?.employmentType || "FULL_TIME")}</select></label>
      <label class="field">Status<select name="status">${selectOptions(["ACTIVE","INACTIVE","ON_LEAVE"], employee?.status || "ACTIVE")}</select></label>
      ${field("address", "Street address", "text", employee?.address)}${field("city", "City", "text", employee?.city)}${field("state", "State / region", "text", employee?.state)}${field("country", "Country", "text", employee?.country)}
      </div><div class="field-actions"><button type="button" class="button button-quiet" data-action="close-modal">Cancel</button><button class="button button-primary" type="submit">${id ? "Save changes" : "Add person"}</button></div></form>`;
    modal.showModal();
  }
  async function openEmployeeDetails(id) {
    const employee = await Api.get(`employees/${id}`);
    const value = (label, content) => `<div class="meta-row"><span>${escapeHtml(label)}</span><strong>${escapeHtml(content || "—")}</strong></div>`;
    modalContent.innerHTML = `<span class="eyebrow">PEOPLE DIRECTORY · ${escapeHtml(employee.employeeCode)}</span><h2>${escapeHtml(employee.firstName)} ${escapeHtml(employee.lastName)}</h2><p class="modal-description">${escapeHtml(employee.designation)} · ${escapeHtml(employee.departmentName)}</p>
      <div class="dashboard-grid"><section class="panel profile-form"><h3>Personal information</h3>${value("Email", employee.email)}${value("Phone", employee.phone)}${value("Date of birth", date(employee.dateOfBirth))}${value("Gender", employee.gender?.replaceAll("_", " "))}</section>
      <section class="panel profile-form"><h3>Employment information</h3>${value("Employee ID", employee.employeeCode)}${value("Department", employee.departmentName)}${value("Employment type", employee.employmentType?.replaceAll("_", " "))}${value("Joining date", date(employee.joiningDate))}${value("Status", employee.status)}${value("Salary", money(employee.salary))}</section>
      <section class="panel profile-form field full"><h3>Address</h3>${value("Street", employee.address)}${value("City", employee.city)}${value("State", employee.state)}${value("Country", employee.country)}</section></div>
      <div class="field-actions" style="margin-top:18px"><button type="button" class="button button-quiet" data-action="close-modal">Back to people</button><button type="button" class="button button-primary" data-action="edit-from-details" data-id="${employee.id}">Edit details</button></div>`;
    modal.showModal();
  }
  function field(name, title, type = "text", value = "", required = false, extra = "") {
    return `<label class="field">${escapeHtml(title)}<input name="${name}" type="${type}" value="${escapeHtml(value)}" ${required ? "required" : ""} ${extra}></label>`;
  }
  function selectOptions(values, selected) {
    return values.map(value => `<option value="${value}" ${selected === value ? "selected" : ""}>${value.replaceAll("_", " ")}</option>`).join("");
  }

  async function renderDepartments() {
    const departments = await Api.get("departments");
    const isAdmin = Api.getUser()?.role === "ADMIN";
    content.innerHTML = `${pageHeading("HOW WE WORK", "Departments", "A look at the teams that make things happen.", isAdmin ? '<button class="button button-primary" data-action="new-department">＋ Add department</button>' : "")}
    <section class="panel table-panel"><div class="panel-head"><div><h3>All departments <span class="sub-cell" style="display:inline">· ${departments.length} teams</span></h3></div></div><div class="table-wrap"><table><thead><tr><th>Department</th><th>Description</th><th>Manager</th><th>People</th><th>Status</th>${isAdmin ? "<th>Actions</th>" : ""}</tr></thead><tbody>${departments.length ? departments.map(d => `<tr><td><strong>${escapeHtml(d.name)}</strong><span class="sub-cell">Team ${String(d.id).padStart(2, "0")}</span></td><td>${escapeHtml(d.description || "—")}</td><td>${escapeHtml(d.managerName || "—")}</td><td>${d.employeeCount}</td><td>${badge(d.active ? "ACTIVE" : "INACTIVE")}</td>${isAdmin ? `<td><div class="actions-cell"><button class="row-action" data-action="edit-department" data-id="${d.id}">Edit</button><button class="row-action delete" data-action="delete-department" data-id="${d.id}" data-name="${escapeHtml(d.name)}">Delete</button></div></td>` : ""}</tr>`).join("") : '<tr><td colspan="6" class="empty-state">No departments yet.</td></tr>'}</tbody></table></div></section>`;
  }
  async function openDepartmentForm(id) {
    const department = id ? await Api.get(`departments/${id}`) : null;
    modalContent.innerHTML = `<span class="eyebrow">TEAM SETUP</span><h2>${id ? "Edit department" : "Create a department"}</h2><p class="modal-description">Give your team a clear name and point of contact.</p><form id="department-form" data-id="${id || ""}">
      ${field("name", "Department name", "text", department?.name, true)}<label class="field">Description<textarea name="description" maxlength="500">${escapeHtml(department?.description || "")}</textarea></label>${field("managerName", "Manager", "text", department?.managerName)}
      <label class="field">Status<select name="active"><option value="true" ${department?.active !== false ? "selected" : ""}>Active</option><option value="false" ${department?.active === false ? "selected" : ""}>Inactive</option></select></label>
      <div class="field-actions"><button type="button" class="button button-quiet" data-action="close-modal">Cancel</button><button class="button button-primary">${id ? "Save department" : "Create department"}</button></div></form>`;
    modal.showModal();
  }

  async function renderAttendance() {
    const admin = Api.getUser()?.role === "ADMIN";
    let rows;
    let employees = [];
    if (admin) {
      const params = new URLSearchParams();
      for (const [key, value] of Object.entries(attendanceFilters)) if (value) params.set(key === "from" ? "from" : key === "to" ? "to" : key, value);
      [rows, employees] = await Promise.all([Api.get(`attendance${params.size ? `?${params}` : ""}`), Api.get("employees?size=100")]);
      employees = employees.content;
    } else rows = await Api.get("attendance/mine");
    const todayRecord = admin ? null : rows.find(item => item.attendanceDate === localDateValue());
    const selfAction = !admin
      ? !todayRecord?.checkIn ? '<button class="button button-primary" data-action="check-in">Check in</button>'
        : !todayRecord.checkOut ? '<button class="button button-secondary" data-action="check-out">Check out</button>'
          : '<span class="badge badge-approved">DAY COMPLETE</span>'
      : "";
    const adminAction = admin ? '<button class="button button-primary" data-action="new-attendance">＋ Record attendance</button>' : "";
    content.innerHTML = `${pageHeading("SHOWING UP", admin ? "Attendance" : "My attendance", admin ? "Daily check-ins and time worked across your team." : "Check in for today and review your attendance history.", admin ? adminAction : selfAction)}
    ${!admin ? `<section class="panel" style="padding:17px 20px;margin-bottom:16px"><h3>Today · ${date(localDateValue())}</h3><p class="muted" style="margin:0">${todayRecord?.checkIn ? `Checked in at ${escapeHtml(todayRecord.checkIn.slice(0, 5))}${todayRecord.checkOut ? ` · Checked out at ${escapeHtml(todayRecord.checkOut.slice(0, 5))} · ${escapeHtml(todayRecord.workingHours)}` : " · Remember to check out at the end of your day."}` : "You have not checked in yet today."}</p></section>` : ""}
    <section class="panel table-panel"><div class="panel-head"><div><h3>${admin ? "Attendance records" : "Attendance history"}</h3><p>${rows.length} records</p></div></div>${admin ? `<form id="attendance-filters" class="toolbar"><label class="field" style="margin:0">From<input name="from" type="date" value="${attendanceFilters.from}"></label><label class="field" style="margin:0">To<input name="to" type="date" value="${attendanceFilters.to}"></label><select class="filter-control" name="employeeId"><option value="">All people</option>${employees.map(e => `<option value="${e.id}" ${String(e.id) === attendanceFilters.employeeId ? "selected" : ""}>${escapeHtml(e.firstName)} ${escapeHtml(e.lastName)}</option>`).join("")}</select><select class="filter-control" name="status"><option value="">All statuses</option>${selectOptions(["PRESENT","ABSENT","LATE","HALF_DAY"], attendanceFilters.status)}</select><button class="button button-secondary button-small">Apply</button></form>` : ""}${attendanceTable(rows, admin)}</section>`;
  }
  function attendanceTable(rows, admin = false) {
    if (!rows.length) return '<div class="empty-state">No attendance records found.</div>';
    return `<div class="table-wrap"><table><thead><tr>${admin ? "<th>Person</th>" : ""}<th>Date</th><th>Check in</th><th>Check out</th><th>Hours</th><th>Status</th>${admin ? "<th>Actions</th>" : ""}</tr></thead><tbody>${rows.map(row => `<tr>${admin ? `<td><span class="person-cell"><span class="mini-avatar">${initials(row.employeeName)}</span>${escapeHtml(row.employeeName)}</span></td>` : ""}<td>${date(row.attendanceDate)}</td><td>${row.checkIn ? escapeHtml(row.checkIn.slice(0, 5)) : "—"}</td><td>${row.checkOut ? escapeHtml(row.checkOut.slice(0, 5)) : "—"}</td><td>${escapeHtml(row.workingHours)}</td><td>${badge(row.status)}</td>${admin ? `<td><button class="row-action" data-action="edit-attendance" data-id="${row.id}">Edit</button></td>` : ""}</tr>`).join("")}</tbody></table></div>`;
  }
  async function openAttendanceForm(id) {
    const employees = await Api.get("employees?size=100");
    const record = id ? await Api.get(`attendance/${id}`) : null;
    modalContent.innerHTML = `<span class="eyebrow">DAILY CHECK-IN</span><h2>${id ? "Edit attendance" : "Record attendance"}</h2><p class="modal-description">Keep the team's daily attendance up to date.</p><form id="attendance-form" data-id="${id || ""}"><label class="field">Person<select name="employeeId" required>${employees.content.map(e => `<option value="${e.id}" ${record?.employeeId === e.id ? "selected" : ""}>${escapeHtml(e.firstName)} ${escapeHtml(e.lastName)} · ${escapeHtml(e.employeeCode)}</option>`).join("")}</select></label>${field("attendanceDate", "Date", "date", record?.attendanceDate || new Date().toISOString().slice(0, 10), true)}${field("checkIn", "Check in", "time", record?.checkIn?.slice(0, 5))}${field("checkOut", "Check out", "time", record?.checkOut?.slice(0, 5))}<label class="field">Status<select name="status">${selectOptions(["PRESENT","ABSENT","LATE","HALF_DAY"], record?.status || "PRESENT")}</select></label><div class="field-actions"><button type="button" class="button button-quiet" data-action="close-modal">Cancel</button><button class="button button-primary">${id ? "Save changes" : "Save record"}</button></div></form>`;
    modal.showModal();
  }

  async function renderLeaves() {
    const admin = Api.getUser()?.role === "ADMIN";
    const leaves = await Api.get(admin ? "leaves" : "leaves/mine");
    if (admin) $("#leave-nav-dot").hidden = !leaves.some(item => item.status === "PENDING");
    content.innerHTML = `${pageHeading("TIME TO RECHARGE", admin ? "Time-off requests" : "My time off", admin ? "Review requests and help your team plan ahead." : "Request time away and keep track of your leave requests.", admin ? "" : '<button class="button button-primary" data-action="new-leave">＋ Request time off</button>')}
    <section class="panel table-panel"><div class="panel-head"><div><h3>${admin ? "All requests" : "My requests"}</h3><p>${leaves.filter(item => item.status === "PENDING").length} pending request(s)</p></div></div><div class="table-wrap"><table><thead><tr>${admin ? "<th>Person</th>" : "<th>Request</th>"}<th>Type</th><th>Dates</th><th>Days</th><th>Reason</th><th>Status</th><th>Actions</th></tr></thead><tbody>${leaves.length ? leaves.map(item => `<tr>${admin ? `<td><span class="person-cell"><span class="mini-avatar">${initials(item.employeeName)}</span>${escapeHtml(item.employeeName)}</span><span class="sub-cell">${escapeHtml(item.employeeCode)}</span></td>` : `<td>REQ-${String(item.id).padStart(4, "0")}</td>`}<td>${escapeHtml(item.leaveType)}</td><td>${date(item.startDate)} – ${date(item.endDate)}</td><td>${item.days}</td><td>${escapeHtml(item.reason)}${item.adminComment ? `<span class="sub-cell">${escapeHtml(item.adminComment)}</span>` : ""}</td><td>${badge(item.status)}</td><td>${admin ? item.status === "PENDING" ? `<div class="actions-cell"><button class="row-action" data-action="approve-leave" data-id="${item.id}">Approve</button><button class="row-action delete" data-action="reject-leave" data-id="${item.id}">Decline</button></div>` : "—" : item.status === "PENDING" ? `<button class="row-action delete" data-action="withdraw-leave" data-id="${item.id}">Withdraw</button>` : "—"}</td></tr>`).join("") : `<tr><td colspan="7" class="empty-state">No leave requests yet.</td></tr>`}</tbody></table></div></section>`;
  }
  function openLeaveForm() {
    modalContent.innerHTML = `<span class="eyebrow">MAKE SPACE FOR LIFE</span><h2>Request time off</h2><p class="modal-description">Your manager will review this request and get back to you.</p><form id="leave-form"><label class="field">Leave type<select name="leaveType">${selectOptions(["CASUAL","SICK","ANNUAL","MATERNITY","PATERNITY","UNPAID"], "ANNUAL")}</select></label>${field("startDate", "First day", "date", "", true, `min="${new Date().toISOString().slice(0, 10)}"`)}${field("endDate", "Last day", "date", "", true, `min="${new Date().toISOString().slice(0, 10)}"`)}<label class="field">Reason<textarea name="reason" maxlength="1000" required></textarea></label><div class="field-actions"><button type="button" class="button button-quiet" data-action="close-modal">Cancel</button><button class="button button-primary">Send request</button></div></form>`;
    modal.showModal();
  }

  async function renderHolidays() {
    const [holidays, departments] = await Promise.all([Api.get("holidays"), Api.get("departments")]);
    const admin = Api.getUser()?.role === "ADMIN";
    const next = holidays.filter(holiday => holiday.holidayDate >= new Date().toISOString().slice(0, 10));
    const upcoming = next.slice(0, 3).map(item => `<article class="stat-card"><div class="stat-top">${escapeHtml(item.name)}<span class="stat-icon tone-violet">✳</span></div><div class="stat-value" style="font-size:1.2rem">${date(item.holidayDate)}</div><div class="stat-foot">${item.paid ? "Paid company holiday" : "Unpaid closure"}</div></article>`).join("");
    content.innerHTML = `${pageHeading("PLAN AHEAD", "Company calendar", "Shared holidays and important dates for the whole team.", admin ? '<button class="button button-primary" data-action="new-holiday">＋ Add a holiday</button>' : "")}
      ${upcoming ? `<section class="stat-grid holiday-upcoming">${upcoming}</section>` : ""}
      <section class="panel table-panel"><div class="panel-head"><div><h3>Company holidays</h3><p>${holidays.length} scheduled date(s)</p></div></div><div class="table-wrap"><table><thead><tr><th>Date</th><th>Holiday</th><th>Details</th><th>Paid</th>${admin ? "<th>Actions</th>" : ""}</tr></thead><tbody>${holidays.length ? holidays.map(item => `<tr><td>${date(item.holidayDate)}</td><td><strong>${escapeHtml(item.name)}</strong></td><td>${escapeHtml(item.description || "—")}</td><td>${badge(item.paid ? "PAID" : "UNPAID")}</td>${admin ? `<td><div class="actions-cell"><button class="row-action" data-action="edit-holiday" data-id="${item.id}">Edit</button><button class="row-action delete" data-action="delete-holiday" data-id="${item.id}" data-name="${escapeHtml(item.name)}">Delete</button></div></td>` : ""}</tr>`).join("") : `<tr><td colspan="${admin ? 5 : 4}" class="empty-state">No holidays have been added yet.${admin ? " Add your first company date to help everyone plan ahead." : ""}</td></tr>`}</tbody></table></div></section>
      <p class="muted" style="font-size:.74rem;margin:12px 3px">The calendar is shared with ${departments.length} departments.</p>`;
  }
  async function openHolidayForm(id) {
    const holiday = id ? await Api.get(`holidays/${id}`) : null;
    modalContent.innerHTML = `<span class="eyebrow">COMPANY CALENDAR</span><h2>${id ? "Edit holiday" : "Add a holiday"}</h2><p class="modal-description">Keep the team informed about company-wide dates.</p><form id="holiday-form" data-id="${id || ""}">${field("name", "Holiday name", "text", holiday?.name, true)}${field("holidayDate", "Date", "date", holiday?.holidayDate, true)}<label class="field">Details<textarea name="description" maxlength="500">${escapeHtml(holiday?.description || "")}</textarea></label><label class="field">Pay treatment<select name="paid"><option value="true" ${holiday?.paid !== false ? "selected" : ""}>Paid company holiday</option><option value="false" ${holiday?.paid === false ? "selected" : ""}>Unpaid closure</option></select></label><div class="field-actions"><button type="button" class="button button-quiet" data-action="close-modal">Cancel</button><button class="button button-primary">${id ? "Save holiday" : "Add holiday"}</button></div></form>`;
    modal.showModal();
  }

  async function renderProfile() {
    const profile = await Api.get("profile");
    const fullName = `${profile.firstName} ${profile.lastName}`;
    content.innerHTML = `${pageHeading("THE PERSON BEHIND THE WORK", "My profile", "A little about you and the details we keep on file.")}
    <section class="profile-layout"><div class="panel profile-card"><div class="profile-avatar">${initials(fullName)}</div><h3>${escapeHtml(fullName)}</h3><p>${escapeHtml(profile.designation || profile.role)}</p><div class="profile-meta"><div class="meta-row"><span>Employee ID</span><strong>${escapeHtml(profile.employeeCode || "HR Admin")}</strong></div><div class="meta-row"><span>Team</span><strong>${escapeHtml(profile.departmentName || "People & Culture")}</strong></div><div class="meta-row"><span>Joined</span><strong>${date(profile.joiningDate)}</strong></div></div></div>
    <form id="profile-form" class="panel profile-form"><div class="panel-head" style="padding:0 0 17px"><div><h3>Personal details</h3><p>Update the contact information we use to reach you.</p></div></div><div class="form-grid">${field("firstName", "First name", "text", profile.firstName, false)}${field("lastName", "Last name", "text", profile.lastName, false)}${field("email", "Work email", "email", profile.email, false)}${field("phone", "Phone", "tel", profile.phone)}${field("address", "Street address", "text", profile.address)}${field("city", "City", "text", profile.city)}${field("state", "State / region", "text", profile.state)}${field("country", "Country", "text", profile.country)}</div><p class="muted" style="font-size:.72rem">Name and email changes are managed by your HR administrator.</p><div class="field-actions"><button class="button button-primary">Save changes</button></div></form></section>
      <form id="password-form" class="panel profile-form" style="margin-top:17px"><div class="panel-head" style="padding:0 0 17px"><div><h3>Sign-in security</h3><p>Choose a unique password with at least 8 characters.</p></div></div><div class="form-grid">${field("currentPassword", "Current password", "password", "", true, 'autocomplete="current-password" minlength="8"')}${field("newPassword", "New password", "password", "", true, 'autocomplete="new-password" minlength="8"')}${field("confirmPassword", "Confirm new password", "password", "", true, 'autocomplete="new-password" minlength="8"')}</div><div class="field-actions"><button class="button button-primary">Update password</button></div></form>`;
    $("#profile-form [name=firstName]").readOnly = true;
    $("#profile-form [name=lastName]").readOnly = true;
    $("#profile-form [name=email]").readOnly = true;
  }

  async function renderReports() {
    const [employees, departments, attendance, leaves] = await Promise.all([
      Api.get("reports/employees"), Api.get("reports/departments"), Api.get("reports/attendance"), Api.get("reports/leaves")
    ]);
    content.innerHTML = `${pageHeading("THE BIG PICTURE", "People reports", "A clear view of the health and rhythm of your organization.", '<button class="button button-secondary" data-action="export-report">↓ Export CSV</button>')}
    <section class="report-grid"><article class="panel report-card"><h3>People</h3>${reportRow("Total people", employees.total)}${reportRow("Active", employees.active)}${reportRow("Inactive", employees.inactive)}</article>
    <article class="panel report-card"><h3>Attendance</h3>${Object.entries(attendance).map(([key, value]) => reportRow(key.replaceAll("_", " "), value)).join("")}</article>
    <article class="panel report-card"><h3>Time off</h3>${Object.entries(leaves).map(([key, value]) => reportRow(key, value)).join("")}</article>
    <article class="panel report-card"><h3>Departments</h3>${departments.length ? departments.map(row => reportRow(row.department, row.count)).join("") : '<div class="empty-state">No departments yet.</div>'}</article></section>`;
    window.reportExportData = { employees, departments, attendance, leaves };
  }
  function reportRow(label, value) { return `<div class="report-row"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`; }

  function renderSettings() {
    content.innerHTML = `${pageHeading("MAKE IT YOURS", "Settings", "A few small ways to make EmployeeHub feel like your space.")}
      <section class="panel"><div class="panel-head"><div><h3>Preferences</h3><p>These settings are saved in this browser.</p></div></div>
      <div class="settings-row"><div><strong>Dark appearance</strong><small>Use a darker color palette in your current browser.</small></div><button class="toggle ${localStorage.getItem("employeehub.dark") === "true" ? "on" : ""}" data-action="toggle-dark" role="switch" aria-checked="${localStorage.getItem("employeehub.dark") === "true"}" aria-label="Toggle dark appearance"></button></div>
      <div class="settings-row"><div><strong>Account security</strong><small>Signed in as ${escapeHtml(Api.getUser()?.email)} · Your session ends when you sign out.</small></div><button class="button button-quiet button-small" data-action="logout">Sign out</button></div></section>`;
  }

  document.addEventListener("click", async event => {
    const button = event.target.closest("[data-action]");
    const pageLink = event.target.closest("[data-page]");
    if (pageLink) {
      event.preventDefault();
      window.location.hash = pageLink.dataset.page;
      return;
    }
    if (!button) return;
    const action = button.dataset.action;
    try {
      if (action === "retry") await load(currentPage());
      if (action === "new-employee") await openEmployeeForm();
      if (action === "edit-employee") await openEmployeeForm(button.dataset.id);
      if (action === "view-employee") await openEmployeeDetails(button.dataset.id);
      if (action === "edit-from-details") { modal.close(); await openEmployeeForm(button.dataset.id); }
      if (action === "delete-employee" && confirm(`Delete ${button.dataset.name}? Their attendance and leave history will also be deleted.`)) {
        await Api.delete(`employees/${button.dataset.id}`); toast("Person and associated records deleted."); await load(currentPage());
      }
      if (action === "page") { employeePage = Number(button.dataset.page); await renderEmployees(); }
      if (action === "new-department") await openDepartmentForm();
      if (action === "edit-department") await openDepartmentForm(button.dataset.id);
      if (action === "delete-department" && confirm(`Delete the ${button.dataset.name} department?`)) {
        await Api.delete(`departments/${button.dataset.id}`); toast("Department deleted."); await load(currentPage());
      }
      if (action === "new-attendance") await openAttendanceForm();
      if (action === "edit-attendance") await openAttendanceForm(button.dataset.id);
      if (action === "new-leave") openLeaveForm();
      if (action === "check-in") {
        await Api.post("attendance/mine/check-in", {});
        toast("You’re checked in. Have a great day."); await load(currentPage());
      }
      if (action === "check-out") {
        await Api.post("attendance/mine/check-out", {});
        toast("You’re checked out for the day."); await load(currentPage());
      }
      if (action === "withdraw-leave" && confirm("Withdraw this pending time-off request?")) {
        await Api.delete(`leaves/mine/${button.dataset.id}`);
        toast("Your request has been withdrawn."); await load("leaves");
      }
      if (action === "new-holiday") await openHolidayForm();
      if (action === "edit-holiday") await openHolidayForm(button.dataset.id);
      if (action === "delete-holiday" && confirm(`Remove ${button.dataset.name} from the company calendar?`)) {
        await Api.delete(`holidays/${button.dataset.id}`); toast("Company holiday removed."); await load("holidays");
      }
      if (action === "approve-leave") {
        if (confirm("Approve this time-off request?")) { await Api.put(`leaves/${button.dataset.id}/approve`, {}); toast("Request approved."); await load(currentPage()); }
      }
      if (action === "reject-leave") {
        const comment = prompt("Add a short reason for declining this request:");
        if (comment?.trim()) { await Api.put(`leaves/${button.dataset.id}/reject`, { comment: comment.trim() }); toast("Request declined."); await load(currentPage()); }
        else if (comment !== null) toast("A reason is required to decline a request.", true);
      }
      if (action === "close-modal") modal.close();
      if (action === "logout") logout();
      if (action === "toggle-dark") {
        const enabled = localStorage.getItem("employeehub.dark") !== "true";
        localStorage.setItem("employeehub.dark", String(enabled));
        document.body.classList.toggle("dark-mode", enabled);
        button.classList.toggle("on", enabled);
        button.setAttribute("aria-checked", String(enabled));
      }
      if (action === "export-report") exportReport(window.reportExportData);
    } catch (error) { toast(error.message, true); }
  });

  document.addEventListener("submit", async event => {
    const form = event.target;
    if (form.id === "employee-filters") {
      event.preventDefault();
      const data = new FormData(form);
      currentEmployeeFilters = { keyword: data.get("keyword").trim(), departmentId: data.get("departmentId"), status: data.get("status") };
      currentEmployeeFilters.designation = data.get("designation").trim();
      currentEmployeeFilters.sort = data.get("sort");
      currentEmployeeFilters.direction = data.get("direction");
      employeePage = 0;
      try { await renderEmployees(); } catch (error) { toast(error.message, true); }
      return;
    }
    if (form.id === "attendance-filters") {
      event.preventDefault();
      const data = new FormData(form);
      attendanceFilters = Object.fromEntries(["from", "to", "employeeId", "status"].map(key => [key, data.get(key)]));
      try { await renderAttendance(); } catch (error) { toast(error.message, true); }
      return;
    }
    if (!["employee-form", "department-form", "attendance-form", "leave-form", "holiday-form", "profile-form", "password-form"].includes(form.id)) return;
    event.preventDefault();
    const data = new FormData(form);
    const submit = form.querySelector('button[type="submit"],button:not([type])');
    if (submit) { submit.disabled = true; submit.textContent = "Saving…"; }
    try {
      if (form.id === "employee-form") {
        const payload = Object.fromEntries(data.entries());
        payload.departmentId = Number(payload.departmentId);
        payload.salary = Number(payload.salary);
        for (const fieldName of ["dateOfBirth", "gender", "address", "city", "state", "country", "initialPassword"]) if (!payload[fieldName]) payload[fieldName] = null;
        if (form.dataset.id) await Api.put(`employees/${form.dataset.id}`, payload);
        else await Api.post("employees", payload);
        modal.close(); toast(form.dataset.id ? "Person details updated." : "Person added to your team."); await load("employees");
      }
      if (form.id === "department-form") {
        const payload = { name: data.get("name"), description: data.get("description") || null, managerName: data.get("managerName") || null, active: data.get("active") === "true" };
        if (form.dataset.id) await Api.put(`departments/${form.dataset.id}`, payload);
        else await Api.post("departments", payload);
        modal.close(); toast(form.dataset.id ? "Department updated." : "Department created."); await load("departments");
      }
      if (form.id === "attendance-form") {
        const payload = Object.fromEntries(data.entries());
        payload.employeeId = Number(payload.employeeId);
        payload.checkIn ||= null; payload.checkOut ||= null;
        if (form.dataset.id) await Api.put(`attendance/${form.dataset.id}`, payload);
        else await Api.post("attendance", payload);
        modal.close(); toast(form.dataset.id ? "Attendance updated." : "Attendance recorded."); await load("attendance");
      }
      if (form.id === "leave-form") {
        await Api.post("leaves", Object.fromEntries(data.entries())); modal.close(); toast("Your time-off request has been sent."); await load("leaves");
      }
      if (form.id === "holiday-form") {
        const payload = { name: data.get("name"), holidayDate: data.get("holidayDate"), description: data.get("description") || null, paid: data.get("paid") === "true" };
        if (form.dataset.id) await Api.put(`holidays/${form.dataset.id}`, payload);
        else await Api.post("holidays", payload);
        modal.close(); toast(form.dataset.id ? "Company holiday updated." : "Company holiday added."); await load("holidays");
      }
      if (form.id === "profile-form") {
        await Api.put("profile", { phone: data.get("phone") || null, address: data.get("address"), city: data.get("city"), state: data.get("state"), country: data.get("country") });
        toast("Your profile is up to date."); await load("profile");
      }
      if (form.id === "password-form") {
        if (data.get("newPassword") !== data.get("confirmPassword")) throw new Error("The new password and confirmation do not match.");
        await Api.put("profile/password", { currentPassword: data.get("currentPassword"), newPassword: data.get("newPassword") });
        toast("Your password has been changed.");
        form.reset();
      }
    } catch (error) { toast(error.message, true); if (submit) { submit.disabled = false; submit.textContent = "Try again"; } }
  });

  function exportReport(data) {
    if (!data) { toast("Load the report before exporting.", true); return; }
    const lines = [["Report", "Metric", "Value"]];
    for (const [name, report] of Object.entries(data)) {
      if (Array.isArray(report)) report.forEach(item => lines.push([name, item.department, item.count]));
      else Object.entries(report).forEach(([key, value]) => lines.push([name, key, value]));
    }
    const csv = lines.map(row => row.map(value => `"${String(value).replaceAll('"', '""')}"`).join(",")).join("\r\n");
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url; link.download = "employeehub-report.csv"; link.click();
    URL.revokeObjectURL(url);
    toast("Report exported.");
  }
  function logout() {
    Api.clearSession();
    window.location.hash = "";
    $("#workspace").hidden = true;
    $("#login-view").hidden = false;
    $("#login-form").reset();
  }
  $("#logout-button").addEventListener("click", logout);
  $("#top-avatar").addEventListener("click", () => { window.location.hash = "profile"; });
  $("#menu-button").addEventListener("click", () => $("#sidebar").classList.toggle("open"));
  window.addEventListener("hashchange", () => load(currentPage()));
  window.addEventListener("employeehub:authenticated", () => load(currentPage()));
  window.addEventListener("employeehub:session-expired", () => {
    $("#workspace").hidden = true; $("#login-view").hidden = false;
    $("#login-error").textContent = "Your session has expired. Please sign in again.";
  });
  document.body.classList.toggle("dark-mode", localStorage.getItem("employeehub.dark") === "true");
  if (Api.isAuthenticated()) load(currentPage());
  else { $("#login-view").hidden = false; $("#workspace").hidden = true; }
})();
