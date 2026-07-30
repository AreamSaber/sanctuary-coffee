package com.coffee.controller;

import com.coffee.common.Result;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.dto.UserUpdateDTO;
import com.coffee.service.UserService;
import com.coffee.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 
 * @author Coffee Shop Team
 */
@Tag(name = "用户管理", description = "用户信息管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        Long userId = getCurrentUserId();
        UserVO userVO = userService.getUserInfo(userId);
        return Result.success(userVO);
    }
    
    @Operation(summary = "更新用户信息")
    @PutMapping("/info")
    public Result<Void> updateUserInfo(@Valid @RequestBody UserUpdateDTO updateDTO) {
        Long userId = getCurrentUserId();
        userService.updateUserInfo(userId, updateDTO);
        return Result.success("更新成功", null);
    }
    
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestParam String oldPassword, 
                                       @RequestParam String newPassword) {
        Long userId = getCurrentUserId();
        userService.changePassword(userId, oldPassword, newPassword);
        return Result.success("密码修改成功", null);
    }
    
    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
