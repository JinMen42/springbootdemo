package com.example.springbootdemo.service;

import com.example.springbootdemo.entity.User;
import com.example.springbootdemo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    // 1. 注入我们写好的 Redis 缓存服务
    private final RedisCacheService redisCacheService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 统一定义缓存的 Key
    private static final String ALL_USERS_CACHE_KEY = "users:all";

    // 构造器注入
    public UserService(UserRepository userRepository, RedisCacheService redisCacheService) {
        this.userRepository = userRepository;
        this.redisCacheService = redisCacheService;
    }

    public User register(User user) {
        User existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        user.setRole("USER");
        User savedUser = userRepository.save(user);
        // 【新增逻辑】数据库新增了数据，立马删除旧缓存！
        redisCacheService.delete(ALL_USERS_CACHE_KEY);

        return savedUser;
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // ================= 【大改造：查询全部用户接入 Redis】 =================
    public List<User> getAll() {
        // 第一步：先查 Redis 缓存
        List<User> cachedUsers = redisCacheService.get(ALL_USERS_CACHE_KEY, new TypeReference<List<User>>() {});
        if (cachedUsers != null) {
            return cachedUsers; // 缓存命中，直接返回！
        }

        // 第二步：缓存如果没有，查 MySQL 数据库
        List<User> dbUsers = userRepository.findAll();

        // 第三步：查到之后存进 Redis，设置 5 分钟过期
        redisCacheService.set(ALL_USERS_CACHE_KEY, dbUsers, 5);

        return dbUsers;
    }
    // =================================================================

    public User getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User updateUser(Long id, User newUser) {
        User oldUser = userRepository.findById(id).orElse(null);

        if (oldUser == null) {
            return null;
        }

        oldUser.setUsername(newUser.getUsername());

        if (newUser.getPassword() != null && !newUser.getPassword().isBlank()) {
            oldUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        }

        oldUser.setAge(newUser.getAge());
        oldUser.setEmail(newUser.getEmail());

        // 【新增逻辑】如果前端传了新头像，就更新它
        if (newUser.getAvatarUrl() != null) {
            oldUser.setAvatarUrl(newUser.getAvatarUrl());
        }

        User updatedUser = userRepository.save(oldUser);

        // 【新增逻辑】数据库更新了数据，立马删除旧缓存！
        redisCacheService.delete(ALL_USERS_CACHE_KEY);

        return updatedUser;
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return null;
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }

        // 【核心提拔逻辑】：如果登录的是 admin，且他还没有角色，直接升为超级管理员并保存！
        if ("admin".equals(user.getUsername()) && !"ADMIN".equals(user.getRole())) {
            user.setRole("ADMIN");
            userRepository.save(user);
        } else if (user.getRole() == null) {
            // 其他老用户如果没有角色，默认设为普通员工
            user.setRole("USER");
            userRepository.save(user);
        }

        return user;
    }

    public Page<User> getUserPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable);
    }

    public Page<User> searchByUsername(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findByUsernameContaining(username, pageable);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);

        // 【新增逻辑】数据库删除了数据，立马删除旧缓存！
        redisCacheService.delete(ALL_USERS_CACHE_KEY);
    }
    // ================= 【新增：批量导入用户】 =================
    // @Transactional 注解保证事务：要么这批人全导入成功，要么报错全不导入
    @org.springframework.transaction.annotation.Transactional
    public void importUsers(List<User> userList) {
        for (User user : userList) {
            // 简单查重：如果数据库里已经有这个用户名了，就跳过
            if (userRepository.findByUsername(user.getUsername()) == null) {
                // 给导入的用户设置一个默认初始密码，比如 123456
                user.setPassword(passwordEncoder.encode("123456"));

                // 为了防止 Excel 里有些必填字段没填导致报错，给点默认值
                if (user.getAge() == null) user.setAge(18);
                if (user.getEmail() == null) user.setEmail(user.getUsername() + "@example.com");

                userRepository.save(user);
            }
        }
        // 数据库大批量更新了，一定要记得清空 Redis 缓存！
        redisCacheService.delete(ALL_USERS_CACHE_KEY);
    }
}