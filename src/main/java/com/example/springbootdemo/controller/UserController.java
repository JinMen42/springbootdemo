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
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import com.alibaba.excel.EasyExcel;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
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
    public Result<User> profile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null); // 安全脱敏：不返回密码
        }
        return Result.success(user);
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
    @GetMapping("/export")
    public void exportUsers(HttpServletResponse response) throws Exception {
        // 1. 设置响应头，告诉浏览器我们要返回的是一个 Excel 文件
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 这里设置下载的文件名
        String fileName = URLEncoder.encode("用户列表数据", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 2. 从数据库查出所有用户 (这里直接复用你之前写好的 getAll 方法，它还自带 Redis 缓存！)
        List<User> userList = userService.getAll();

        // 3. 将数据写入到响应流中，直接发给前端
        EasyExcel.write(response.getOutputStream(), User.class)
                .sheet("用户信息表")
                .doWrite(userList);
    }


    // 【修改点】：去掉了 @Valid，允许在更新时不传密码等受限字段
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
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
    @PostMapping("/import")
    public Result<String> importUsers(@RequestParam("file") MultipartFile file) {
        try {
            // EasyExcel 神奇的一行代码：直接把 Excel 文件的数据读取并转换成 User 对象的 List！
            List<User> userList = EasyExcel.read(file.getInputStream())
                    .head(User.class)
                    .sheet()
                    .doReadSync();

            // 调用 Service 层保存到数据库
            userService.importUsers(userList);

            return Result.success("成功导入 " + userList.size() + " 条数据", null);
        } catch (Exception e) {
            return Result.fail(500, "Excel 导入失败：" + e.getMessage());
        }
    }

}