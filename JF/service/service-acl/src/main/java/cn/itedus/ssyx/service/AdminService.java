package cn.itedus.ssyx.service;

import cn.itedus.ssyx.model.acl.Admin;
import cn.itedus.ssyx.vo.acl.AdminQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * @author: Guanghao Wei
 * @date: 2023-06-08 16:48
 * @description: 用户管理Service
 */
public interface AdminService extends IService<Admin> {
    /**
     * 分页查询用户信息
     * @param pageParam 分页信息
     * @param adminQueryVo 查询条件
     * @return 分页结果
     */
    IPage<Admin> selectPage(Page<Admin> pageParam, AdminQueryVo adminQueryVo);

    /**
     * Query all roles and roles currently assigned to an admin.
     *
     * @param adminId admin user id
     * @return map containing allRolesList and assignRoles
     */
    Map<String, Object> getRolesByAdminId(Long adminId);

    /**
     * Replace role assignments for an admin user.
     *
     * @param adminId admin user id
     * @param roleId comma-separated role ids
     */
    void assignRoles(Long adminId, String roleId);
}
