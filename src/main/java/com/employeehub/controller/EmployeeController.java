package com.employeehub.controller;

import com.employeehub.dto.*;
import com.employeehub.entity.EmployeeStatus;
import com.employeehub.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private static final Set<String> SORT_FIELDS = Set.of("firstName", "joiningDate", "salary", "status", "createdAt", "employeeCode", "department.name");
    private final EmployeeService service;
    public EmployeeController(EmployeeService service) { this.service = service; }
    @GetMapping
    public EmployeePageResponse<EmployeeResponse> list(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId, @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String designation,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort, @RequestParam(defaultValue = "desc") String direction) {
        if (page < 0 || size < 1 || size > 100) throw new com.employeehub.exception.BadRequestException("Page must be non-negative and size must be between 1 and 100.");
        if (!SORT_FIELDS.contains(sort)) throw new com.employeehub.exception.BadRequestException("Unsupported sort field.");
        Sort.Direction order = Sort.Direction.fromString(direction);
        Page<EmployeeResponse> result = service.search(keyword, departmentId, status, designation,
                PageRequest.of(page, size, Sort.by(order, sort)));
        return new EmployeePageResponse<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }
    @GetMapping("/search")
    public EmployeePageResponse<EmployeeResponse> search(@RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return list(keyword, null, null, null, page, size, "createdAt", "desc");
    }
    @GetMapping("/{id}") public EmployeeResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest request) { return service.create(request); }
    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
