package com.example.springbootdemo.controller;

import com.example.springbootdemo.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:5173")
public class FileController {

    // 从配置文件读取刚刚配置的目录
    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    // 1. 文件上传接口
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.fail(400, "文件不能为空");
            }

            // 确保保存文件的目录存在，不存在就自动创建
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 获取原始文件名并提取后缀 (比如 .png, .jpg)
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 【关键点】生成 UUID 作为新文件名，防止别人上传同名文件把你的覆盖掉
            String newFilename = UUID.randomUUID().toString() + extension;

            // 将文件保存到本地磁盘
            Path filePath = Paths.get(uploadDir, newFilename);
            Files.copy(file.getInputStream(), filePath);

            // 拼接出文件的在线访问 URL 并返回
            String fileUrl = "http://localhost:8080/api/files/" + newFilename;
            return Result.success("上传成功", fileUrl);

        } catch (IOException e) {
            return Result.fail(500, "文件上传失败: " + e.getMessage());
        }
    }

    // 2. 文件下载/预览接口
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> download(@PathVariable String fileName) {
        try {
            // 找到本地磁盘上的那个文件
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        // inline 表示在浏览器直接预览，如果是 attachment 就会变成下载弹窗
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}