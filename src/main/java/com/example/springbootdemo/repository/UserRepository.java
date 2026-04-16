package com.example.springbootdemo.repository;

import com.example.springbootdemo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);

    Page<User> findByUsernameContaining(String username, Pageable pageable);
}