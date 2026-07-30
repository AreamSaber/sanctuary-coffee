package com.coffee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.coffee.dto.UserUpdateDTO;
import com.coffee.entity.User;
import com.coffee.vo.UserVO;

/**
 * 用户服务接口
 * 
 * @author Coffee Shop Team
 */
public interface UserService extends IService<User> {
    
    /**
     * 获取用户信息
     */
    UserVO getUserInfo(Long userId);
    
    /**
     * 更新用户信息
     */
    void updateUserInfo(Long userId, UserUpdateDTO updateDTO);
    
    /**
     * 修改密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
