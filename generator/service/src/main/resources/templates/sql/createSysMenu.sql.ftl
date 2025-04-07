-- 创建系统菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `title`, `name`, `path`, `component`, `platform`, `type`, `handle_type`, `outer_url`, `icon`, `icon_type`, `order`, `cache`, `enabled`, `create_time`, `update_time`, `create_by`, `update_by`, `deleted`)
VALUES
    (${menuId}, NULL, '${name}', '${permissionPrefix}', 'menu', '${indexViewPath}', 'web', 'menu', 'route', NULL, NULL, NULL, NULL, 1, 1, now(), now(), ${userId}, ${userId}, 0),
<#if hasImport>
    (NULL, ${menuId}, '导入', '${permissionPrefix}:import', NULL, NULL, 'web', 'btn', NULL, NULL, NULL, NULL, NULL, NULL, 1, now(), now(), ${userId}, ${userId}, 0),
</#if>
    (NULL, ${menuId}, '新增', '${permissionPrefix}:add', NULL, NULL, 'web', 'btn', NULL, NULL, NULL, NULL, NULL, NULL, 1, now(), now(), ${userId}, ${userId}, 0),
    (NULL, ${menuId}, '编辑', '${permissionPrefix}:edit', NULL, NULL, 'web', 'btn', NULL, NULL, NULL, NULL, NULL, NULL, 1, now(), now(), ${userId}, ${userId}, 0),
    (NULL, ${menuId}, '明细', '${permissionPrefix}:detail', NULL, NULL, 'web', 'btn', NULL, NULL, NULL, NULL, NULL, NULL, 1, now(), now(), ${userId}, ${userId}, 0),
    (NULL, ${menuId}, '删除', '${permissionPrefix}:del', NULL, NULL, 'web', 'btn', NULL, NULL, NULL, NULL, NULL, NULL, 1, now(), now(), ${userId}, ${userId}, 0);
