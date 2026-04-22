package com.example.springbootdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper; // 注意这里的包名
import tools.jackson.core.type.TypeReference;

import java.util.concurrent.TimeUnit;

@Service
public class RedisCacheService {

    // 【学习新知识：引入专业日志 Logger】
    // 以后在这个类里，我们用 log.info() 打印正常信息，用 log.error() 打印报错
    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 存入缓存
     * @param key 缓存的键
     * @param value 缓存的值（对象或集合）
     * @param timeoutMinutes 过期时间（分钟）
     */
    public void set(String key, Object value, long timeoutMinutes) {
        try {
            // 将 Java 对象转成 JSON 字符串存入 Redis
            String jsonStr = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, jsonStr, timeoutMinutes, TimeUnit.MINUTES);

            // 打印一条好看的成功日志，大括号 {} 是占位符，会自动替换成后面的变量
            log.info("✅ Redis 写入缓存成功, Key: {}, 过期时间: {}分钟", key, timeoutMinutes);
        } catch (Exception e) {
            // 使用 error 级别记录日志，并传入 e，这样控制台会打印出极其详细的报错原因
            log.error("❌ Redis 写入缓存失败, Key: {}", key, e);
        }
    }

    /**
     * 读取缓存并转为 List 等复杂泛型
     * @param key 缓存的键
     * @param typeReference 类型引用
     * @return 还原后的 Java 对象，如果缓存没有则返回 null
     */
    public <T> T get(String key, TypeReference<T> typeReference) {
        try {
            String jsonStr = stringRedisTemplate.opsForValue().get(key);
            if (jsonStr != null) {
                log.info("⚡ Redis 命中缓存, 极速返回数据! Key: {}", key);
                // 将 JSON 字符串还原成 Java 对象
                return objectMapper.readValue(jsonStr, typeReference);
            } else {
                log.info("🐌 Redis 缓存未命中 (或已过期), 准备去查数据库... Key: {}", key);
            }
        } catch (Exception e) {
            log.error("❌ Redis 读取缓存失败, Key: {}", key, e);
        }
        return null;
    }

    /**
     * 删除缓存（当数据库数据更新时调用）
     * @param key 缓存的键
     */
    public void delete(String key) {
        try {
            stringRedisTemplate.delete(key);
            log.info("🗑️ Redis 删除缓存成功 (数据已更新), Key: {}", key);
        } catch (Exception e) {
            log.error("❌ Redis 删除缓存失败, Key: {}", key, e);
        }
    }
}