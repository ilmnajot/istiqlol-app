package org.example.moliyaapp.controller;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.dto.ApiResponse;
import org.example.moliyaapp.dto.UserDto;
import org.example.moliyaapp.entity.User;
import org.example.moliyaapp.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @PutMapping("/updateUser")
    public ApiResponse updateUser(@RequestParam("id") Long id,
                                  @RequestBody UserDto.UpdateUser dto) {
        return this.userService.updateUser(id, dto);
    }

    @GetMapping("/getAllActiveUser")
    public ApiResponse getAllActiveUser(@RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                        @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        return this.userService.getAllActiveUser(PageRequest.of(page, size));
    }

    @GetMapping("/getUserById")
    public ApiResponse getUserById(@RequestParam("id") Long id) {
        return this.userService.getUserById(id);
    }

    @PostMapping("/checkByGoogleEmail")
    public ApiResponse checkByGoogleEmail(@RequestParam("email") String email) {
        return this.userService.checkByGoogleEmail(email);
    }

    @PutMapping("/updateEmail")
    public ApiResponse updateEmail(@RequestParam("email") String email,
                                   @RequestParam("newEmail") String newEmail) {
        return this.userService.updateEmail(email, newEmail);
    }

    @PostMapping("/checkUpdatedEmail")
    public ApiResponse checkUpdatedEmail(@RequestParam("oldEmail") String oldEmail,
                                         @RequestParam("newEmail") String newEmail,
                                         @RequestParam("code") String code) {
        return this.userService.checkUpdatedEmail(oldEmail, newEmail, code);
    }

    @PutMapping("/updatePassword")
    public ApiResponse updatePassword(@RequestParam("email") String email,
                                      @RequestParam("newPassword") String newPassword) {
        return this.userService.updatePassword(email, newPassword);
    }

    @PostMapping("/checkUpdatedPassword")
    public ApiResponse checkUpdatedPassword(@RequestParam("email") String email,
                                            @RequestParam("password") String password,
                                            @RequestParam("code") String code) {
        return this.userService.checkUpdatedPassword(email, password, code);
    }

    @PostMapping("/checkUsername")
    public ApiResponse checkUsername(@RequestParam("username") String username) {
        return this.userService.checkUsername(username);
    }

    @PostMapping("/addRoleToUser")
    public ApiResponse addRoleToUser(@RequestParam("userId") Long userId,
                                     @RequestParam("roleId") Long roleId) {
        return this.userService.addRoleToUser(userId, roleId);
    }

    @DeleteMapping("/deleteRoleFromUser")
    public ApiResponse deleteRoleFromUser(@RequestParam("userId") Long userId,
                                          @RequestParam("roleId") Long roleId) {
        return this.userService.deleteRoleFromUser(userId, roleId);
    }

    @GetMapping("/getUserByToken")
    public ApiResponse getUserByToken(@RequestParam("token") String token) {
        return this.userService.getUserByToken(token);
    }
}
