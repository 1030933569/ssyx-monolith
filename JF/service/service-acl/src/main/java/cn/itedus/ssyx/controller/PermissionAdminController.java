package cn.itedus.ssyx.controller;

import cn.itedus.ssyx.common.result.Result;
import cn.itedus.ssyx.model.acl.Permission;
import cn.itedus.ssyx.service.PermissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author: Guanghao Wei
 * @date: 2023-06-09 14:58
 * @description: 菜单管理控制器
 */
@Api(tags = "菜单管理")
@RestController
@RequestMapping("/admin/acl/permission")
public class PermissionAdminController {
    @Autowired
    private PermissionService permissionService;

    @ApiOperation("获取菜单列表")
    @GetMapping
    public Result index(){
        List<Permission> list = permissionService.queryAllMenu();
        return Result.ok(list);
    }

    @ApiOperation("新增菜单")
    @PostMapping("save")
    public Result savePermission(@RequestBody Permission permission) {
        permissionService.save(permission);
        return Result.ok();
    }

    @ApiOperation("修改菜单")
    @PostMapping("update")
    public Result save(@RequestBody Permission permission) {
        permissionService.updateById(permission);
        return Result.ok();
    }

    @ApiOperation("递归删除菜单")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable("id") Long id) {
        permissionService.removeChildById(id);
        return Result.ok();
    }

    @ApiOperation("鑾峰彇瑙掕壊鏉冮檺")
    @GetMapping("toAssign/{roleId}")
    public Result toAssign(@PathVariable("roleId") Long roleId) {
        List<Permission> list = permissionService.selectAllMenu(roleId);
        return Result.ok(list);
    }

    @ApiOperation("缁欒鑹插垎閰嶆潈闄?")
    @PostMapping("doAssign")
    public Result doAssign(@RequestParam("roleId") Long roleId,
                           @RequestParam(value = "permissionId", required = false) String permissionId) {
        permissionService.saveRolePermission(roleId, permissionId);
        return Result.ok();
    }
}
