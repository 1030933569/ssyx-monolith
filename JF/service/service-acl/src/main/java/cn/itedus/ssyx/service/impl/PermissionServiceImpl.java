package cn.itedus.ssyx.service.impl;

import cn.itedus.ssyx.helper.PermissionHelper;
import cn.itedus.ssyx.mapper.PermissionMapper;
import cn.itedus.ssyx.mapper.RolePermissionMapper;
import cn.itedus.ssyx.model.acl.Permission;
import cn.itedus.ssyx.model.acl.RolePermission;
import cn.itedus.ssyx.service.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: Guanghao Wei
 * @date: 2023-06-09 14:57
 * @description: 菜单接口实现类
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {
    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Override
    public List<Permission> queryAllMenu() {

        //1. 查询所有的菜单数据，方便后续封装
        List<Permission> permissionList = permissionMapper.selectList(null);
        //2. 获取子节点,构建树形结构
        List<Permission> result = PermissionHelper.builder(permissionList);

        return result;
    }

    @Override
    public boolean removeChildById(Long id) {
        List<Long> idList = new ArrayList<>();
        this.selectChildListById(id, idList);
        permissionMapper.deleteBatchIds(idList);
        return true;
    }

    /**
     * 递归获取子节点
     *
     * @param id     当前节点ID
     * @param idList 子节点ID列表
     */
    private void selectChildListById(Long id, List<Long> idList) {
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pid", id);
        queryWrapper.select("id");
        List<Permission> permissionList = permissionMapper.selectList(queryWrapper);
        permissionList.stream().forEach(item -> {
            idList.add(item.getId());
            this.selectChildListById(item.getId(), idList);
        });
    }

    @Override
    public List<Permission> selectAllMenu(Long roleId) {
        List<Permission> permissionList = permissionMapper.selectList(null);
        List<RolePermission> rolePermissionList = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));

        Set<Long> permissionIdSet = new HashSet<>();
        for (RolePermission rolePermission : rolePermissionList) {
            permissionIdSet.add(rolePermission.getPermissionId());
        }

        for (Permission permission : permissionList) {
            permission.setSelect(permissionIdSet.contains(permission.getId()));
        }
        return PermissionHelper.builder(permissionList);
    }

    @Override
    public void saveRolePermission(Long roleId, String permissionId) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId));

        if (!StringUtils.hasText(permissionId)) {
            return;
        }

        String[] permissionIds = permissionId.split(",");
        for (String idText : permissionIds) {
            if (!StringUtils.hasText(idText)) {
                continue;
            }
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(Long.valueOf(idText.trim()));
            rolePermissionMapper.insert(rolePermission);
        }
    }
}
