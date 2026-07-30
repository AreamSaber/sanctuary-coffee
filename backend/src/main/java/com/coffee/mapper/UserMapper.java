package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户Mapper
 * 
 * @author Coffee Shop Team
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户ID查询角色编码列表
     */
    @Select("SELECT r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(Long userId);

    @Select("SELECT id FROM sys_role WHERE role_code = #{roleCode} AND status = 1 AND deleted = 0 LIMIT 1")
    Long selectRoleIdByCode(String roleCode);

    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("SELECT id, username, nickname FROM sys_user " +
            "WHERE deleted = 0 AND (password IS NULL OR TRIM(password) = '') ORDER BY id ASC")
    List<User> selectUsersWithBlankPassword();

    @Update("UPDATE sys_user SET password = #{password}, update_time = NOW() " +
            "WHERE deleted = 0 AND (password IS NULL OR TRIM(password) = '')")
    int initializeBlankPasswords(@Param("password") String password);
}
