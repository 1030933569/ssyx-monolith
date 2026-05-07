package cn.itedus.ssyx.user.controller;

import cn.itedus.ssyx.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "Driver Admin API")
@RestController
@RequestMapping("/admin/user/driver")
public class DriverAdminController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ApiOperation("List drivers by warehouse")
    @GetMapping("findDriver/{wareId}")
    public Result findDriver(@PathVariable Long wareId) {
        return Result.ok(jdbcTemplate.queryForList(
                "select id, name, phone, ware_id wareId from driver where is_deleted = 0 and ware_id = ? order by id",
                wareId));
    }
}
