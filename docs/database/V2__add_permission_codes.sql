-- ============================================================
-- 权限码补充脚本（任务1：接口权限码迁移）
-- 执行顺序：在 coffee_shop_complete.sql 之后执行
--
-- 2026-05-01 更新：当前主初始化脚本已经内置统一后的前后端权限码。
-- 本迁移只保留仍未在主脚本中出现的补充权限，避免重新插入
-- 已废弃的旧管理权限码。
-- ============================================================

-- 补充系统管理权限码（假设已有 id 1~57，从 74 起）
INSERT INTO `sys_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `sort_order`, `status`, `deleted`) VALUES
(74, 2, '密码初始化', 'system:password:init', 2, 1, 1, 0);

-- 将新权限码分配给管理员角色（role_id=1）
-- 注意：如果管理员已通过其他方式全量授权，此步可跳过
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 74);
