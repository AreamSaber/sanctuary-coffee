package com.coffee.controller;

import com.coffee.common.Result;
import com.coffee.dto.LoginDTO;
import com.coffee.dto.PasswordInitDTO;
import com.coffee.dto.RegisterDTO;
import com.coffee.service.AuthService;
import com.coffee.vo.LoginVO;
import com.coffee.vo.PasswordInitResultVO;
import com.coffee.vo.PasswordInitStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器。
 */
@Tag(name = "认证管理", description = "用户注册、登录、退出与首次密码初始化")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success("注册成功", null);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success("登录成功", authService.login(loginDTO));
    }

    @Operation(summary = "查询是否需要初始化测试账号密码")
    @GetMapping("/setup/status")
    public Result<PasswordInitStatusVO> getPasswordInitStatus() {
        return Result.success(authService.getPasswordInitStatus());
    }

    @Operation(summary = "批量初始化空密码测试账号")
    @PostMapping("/setup/passwords")
    public Result<PasswordInitResultVO> initializeBlankPasswords(@Valid @RequestBody PasswordInitDTO passwordInitDTO) {
        return Result.success("初始化密码成功", authService.initializeBlankPasswords(passwordInitDTO));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success("退出成功", null);
    }
}
