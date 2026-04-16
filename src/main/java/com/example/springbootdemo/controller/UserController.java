package com.example.springbootdemo.controller;

import com.example.springbootdemo.common.Result;
import com.example.springbootdemo.entity.LoginRequest;
import com.example.springbootdemo.entity.User;
import com.example.springbootdemo.service.UserService;
import com.example.springbootdemo.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody User user) {
        User savedUser = userService.register(user);
        return Result.success("注册成功", savedUser);
    }

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginRequest loginRequest) {
        User user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());

        if (user == null) {
            return Result.fail(401, "用户名或密码错误");
        }

        String token = JwtUtils.createToken(user.getId(), user.getUsername());
        return Result.success("登录成功", token);
    }

    @GetMapping("/profile")
    public Result<String> profile(HttpServletRequest request) {
        String username = (String) request.getAttribute("currentUsername");
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success("访问成功", "当前登录用户: " + username + "，用户ID: " + userId);
    }

    @GetMapping
    public Result<List<User>> getAll() {
        return Result.success(userService.getAll());
    }

    @GetMapping("/page")
    public Result<Page<User>> getUserPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return Result.success(userService.getUserPage(page, size));
    }

    @GetMapping("/search")
    public Result<Page<User>> searchByUsername(
            @RequestParam String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return Result.success(userService.searchByUsername(username, page, size));
    }

    @GetMapping("/id/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);

        if (user == null) {
            return Result.fail(404, "用户不存在");
        }

        return Result.success(user);
    }

    @GetMapping("/{username}")
    public Result<User> getByUsername(@PathVariable String username) {
        User user = userService.getByUsername(username);

        if (user == null) {
            return Result.fail(404, "用户不存在");
        }

        return Result.success(user);
    }

    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);

        if (updatedUser == null) {
            return Result.fail(404, "用户不存在");
        }

        return Result.success("更新成功", updatedUser);
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteById(@PathVariable Long id) {
        userService.deleteById(id);
        return Result.success("删除成功", null);
    }
}