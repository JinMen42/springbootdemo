package com.example.springbootdemo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.persistence.Column;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.ExcelIgnore;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ExcelProperty("用户ID") // 生成的 Excel 表头名字
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "用户名不能为空")
    @ExcelProperty("用户名")
    private String username;

    @NotBlank(message = "密码不能为空")
    @ExcelIgnore // ⚠️ 密码绝对不能泄露，忽略此字段不导出！
    private String password;

    @Min(value = 0, message = "年龄不能小于0")
    @ExcelProperty("年龄")
    private Integer age;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    @ExcelProperty("邮箱")
    private String email;
    // 用户的角色：ADMIN 或 USER
    private String role;

    @ExcelProperty("头像链接")
    private String avatarUrl;

    public User() {
    }

    public User(String username, String password, Integer age, String email) {
        this.username = username;
        this.password = password;
        this.age = age;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // 【新增】头像的 Getter 和 Setter
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

}