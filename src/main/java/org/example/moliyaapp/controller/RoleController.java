package org.example.moliyaapp.controller;
import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.RoleDto;
import org.example.moliyaapp.service.RoleService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @PostMapping("/add-role")
    public ApiResponse addRole(@RequestBody RoleDto.CreateRole dto) {
        return this.roleService.addRole(dto);
    }

    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @GetMapping("/get-role/{roleId}")
    public ApiResponse getRole(@PathVariable(name = "roleId") Long roleId) {
        return this.roleService.getRole(roleId);
    }

    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @GetMapping("/get-all-roles")
    public ApiResponse getAllRoles(@RequestParam(name = "page", defaultValue = "0") int page,
                                   @RequestParam(name = "size", defaultValue = "10") int size) {
        return this.roleService.getAllRoles(PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @DeleteMapping("/delete-role/{id}")
    public ApiResponse deleteRole(@PathVariable(name = "id") Long id) {
        return this.roleService.deleteRole(id);
    }

    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @PutMapping("/update-role/{id}")
    public ApiResponse updateUserRole(@PathVariable(name = "id") Long id,
                                      @RequestBody RoleDto.CreateRole dto) {
        return this.roleService.updateRole(id, dto);
    }


}
