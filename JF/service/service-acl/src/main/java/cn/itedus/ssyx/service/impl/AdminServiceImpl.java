package cn.itedus.ssyx.service.impl;

import cn.itedus.ssyx.mapper.AdminMapper;
import cn.itedus.ssyx.mapper.AdminRoleMapper;
import cn.itedus.ssyx.mapper.RoleMapper;
import cn.itedus.ssyx.model.acl.Admin;
import cn.itedus.ssyx.model.acl.AdminRole;
import cn.itedus.ssyx.model.acl.Role;
import cn.itedus.ssyx.service.AdminService;
import cn.itedus.ssyx.vo.acl.AdminQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Guanghao Wei
 * @date: 2023-06-08 16:49
 * @description: 用户管理Service实现类
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private AdminRoleMapper adminRoleMapper;

    @Override
    public IPage<Admin> selectPage(Page<Admin> pageParam, AdminQueryVo adminQueryVo) {
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        if (!StringUtils.isEmpty(adminQueryVo.getUsername())) {
            queryWrapper.like(Admin::getName, adminQueryVo.getUsername());
        }

        IPage<Admin> ipageResult = adminMapper.selectPage(pageParam, queryWrapper);
        return ipageResult;
    }

    @Override
    public Map<String, Object> getRolesByAdminId(Long adminId) {
        List<Role> allRolesList = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .orderByAsc(Role::getId));

        List<AdminRole> adminRoleList = adminRoleMapper.selectList(new LambdaQueryWrapper<AdminRole>()
                .eq(AdminRole::getAdminId, adminId));
        List<Long> roleIdList = new ArrayList<>();
        for (AdminRole adminRole : adminRoleList) {
            roleIdList.add(adminRole.getRoleId());
        }

        List<Role> assignRoles = new ArrayList<>();
        if (!roleIdList.isEmpty()) {
            assignRoles = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                    .in(Role::getId, roleIdList)
                    .orderByAsc(Role::getId));
        }

        Map<String, Object> roleMap = new HashMap<>();
        roleMap.put("allRolesList", allRolesList);
        roleMap.put("assignRoles", assignRoles);
        return roleMap;
    }

    @Override
    public void assignRoles(Long adminId, String roleId) {
        adminRoleMapper.delete(new LambdaQueryWrapper<AdminRole>()
                .eq(AdminRole::getAdminId, adminId));

        if (!StringUtils.hasText(roleId)) {
            return;
        }

        String[] roleIds = roleId.split(",");
        for (String idText : roleIds) {
            if (!StringUtils.hasText(idText)) {
                continue;
            }
            AdminRole adminRole = new AdminRole();
            adminRole.setAdminId(adminId);
            adminRole.setRoleId(Long.valueOf(idText.trim()));
            adminRoleMapper.insert(adminRole);
        }
    }
}
