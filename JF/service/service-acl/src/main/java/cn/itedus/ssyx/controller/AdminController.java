package cn.itedus.ssyx.controller;

import cn.itedus.ssyx.common.result.Result;
import cn.itedus.ssyx.common.utils.MD5;
import cn.itedus.ssyx.model.acl.Admin;
import cn.itedus.ssyx.service.AdminService;
import cn.itedus.ssyx.vo.acl.AdminQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


/**
 * @author: Guanghao Wei
 * @date: 2023-06-08 16:46
 * @description: 用户管理控制器
 */
@Api(tags = "用户管理")
@RestController
@RequestMapping("/admin/acl/user")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @ApiOperation("获取管理用户分页列表")
    @GetMapping("{page}/{limit}")
    public Result IndexPageInfo(@PathVariable("page") Long page,
                                @PathVariable("limit") Long limit,
                                AdminQueryVo adminQueryVo) {
        Page<Admin> pageParam = new Page<>(page, limit);
        IPage<Admin> iPageModel = adminService.selectPage(pageParam, adminQueryVo);
        return Result.ok(iPageModel);
    }

    @ApiOperation("获取管理用户")
    @GetMapping("get/{id}")
    public Result getAdminById(@PathVariable("id") Long id) {
        Admin admin = adminService.getById(id);
        return Result.ok(admin);
    }

    @ApiOperation("新增管理用户")
    @PostMapping("save")
    public Result aveAdmin(@RequestBody Admin admin) {
        admin.setPassword(MD5.encrypt(admin.getPassword()));
        adminService.save(admin);
        return Result.ok();
    }

    @ApiOperation("修改管理用户")
    @PutMapping("update")
    public Result updateAdmin(@RequestBody Admin admin) {
        adminService.updateById(admin);
        return Result.ok();
    }

    @ApiOperation("删除管理用户")
    @DeleteMapping("remove/{id}")
    public Result removeAdminById(@PathVariable("id") Long id) {
        adminService.removeById(id);
        return Result.ok();
    }

    @ApiOperation("批量删除管理用户")
    @DeleteMapping("batchRemove")
    public Result batchRemoveAdmin(@RequestBody List<Long> idList) {
        adminService.removeByIds(idList);
        return Result.ok();
    }

    @ApiOperation("鑾峰彇鐢ㄦ埛瑙掕壊")
    @GetMapping("toAssign/{adminId}")
    public Result toAssign(@PathVariable("adminId") Long adminId) {
        Map<String, Object> roleMap = adminService.getRolesByAdminId(adminId);
        return Result.ok(roleMap);
    }

    @ApiOperation("缁欑敤鎴峰垎閰嶈鑹?")
    @PostMapping("doAssign")
    public Result doAssign(@RequestParam("adminId") Long adminId,
                           @RequestParam(value = "roleId", required = false) String roleId) {
        adminService.assignRoles(adminId, roleId);
        return Result.ok();
    }
}
