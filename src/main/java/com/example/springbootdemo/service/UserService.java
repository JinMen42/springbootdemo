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
}