package cn.itedus.ssyx.user.controller;

import cn.itedus.ssyx.common.result.Result;
import cn.itedus.ssyx.model.user.Leader;
import cn.itedus.ssyx.user.mapper.LeaderMapper;
import cn.itedus.ssyx.vo.user.LeaderQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "后台团长管理")
@RestController
@RequestMapping("/admin/user/leader")
public class LeaderAdminController {

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ApiOperation("获取待审核团长列表")
    @GetMapping("checkList/{page}/{limit}")
    public Result checkList(@PathVariable Long page,
                            @PathVariable Long limit,
                            LeaderQueryVo leaderQueryVo) {
        IPage<Leader> pageModel = selectPage(page, limit, leaderQueryVo, 0);
        return Result.ok(pageModel);
    }

    @ApiOperation("获取已审核团长列表")
    @GetMapping("list/{page}/{limit}")
    public Result list(@PathVariable Long page,
                       @PathVariable Long limit,
                       LeaderQueryVo leaderQueryVo) {
        IPage<Leader> pageModel = selectPage(page, limit, leaderQueryVo, 1);
        return Result.ok(pageModel);
    }

    @ApiOperation("获取团长信息")
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id) {
        Leader leader = leaderMapper.selectById(id);
        fillRegionParam(leader, new HashMap<Long, String>());
        return Result.ok(leader);
    }

    @ApiOperation("新增团长")
    @PostMapping("save")
    public Result save(@RequestBody Leader leader) {
        Date now = new Date();
        leader.setId(null);
        leader.setCreateTime(now);
        leader.setUpdateTime(now);
        leader.setIsDeleted(0);
        if (leader.getCheckStatus() == null) {
            leader.setCheckStatus(0);
        }
        if (leader.getWorkStatus() == null) {
            leader.setWorkStatus(0);
        }
        if (leader.getCheckStatus() == 1) {
            leader.setCheckTime(now);
            leader.setCheckUser("admin");
        }
        leaderMapper.insert(leader);
        return Result.ok();
    }

    @ApiOperation("修改团长")
    @PutMapping("update")
    public Result update(@RequestBody Leader leader) {
        leader.setUpdateTime(new Date());
        leaderMapper.updateById(leader);
        return Result.ok();
    }

    @ApiOperation("删除团长")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        leaderMapper.deleteById(id);
        return Result.ok();
    }

    @ApiOperation("批量删除团长")
    @DeleteMapping("batchRemove")
    public Result batchRemove(@RequestBody List<Long> idList) {
        if (idList != null && !idList.isEmpty()) {
            leaderMapper.deleteBatchIds(idList);
        }
        return Result.ok();
    }

    @ApiOperation("审核团长")
    @PostMapping("check/{id}/{status}")
    public Result check(@PathVariable Long id,
                        @PathVariable Integer status) {
        Leader leader = new Leader();
        leader.setId(id);
        leader.setCheckStatus(status != null && status == 1 ? 1 : -1);
        leader.setCheckTime(new Date());
        leader.setCheckUser("admin");
        leaderMapper.updateById(leader);
        return Result.ok();
    }

    private IPage<Leader> selectPage(Long page, Long limit, LeaderQueryVo leaderQueryVo, Integer checkStatus) {
        LambdaQueryWrapper<Leader> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Leader::getCheckStatus, checkStatus);
        if (leaderQueryVo != null && StringUtils.hasText(leaderQueryVo.getKeyword())) {
            String keyword = leaderQueryVo.getKeyword();
            wrapper.and(query -> query.like(Leader::getName, keyword)
                    .or().like(Leader::getPhone, keyword)
                    .or().like(Leader::getTakeName, keyword));
        }
        wrapper.orderByDesc(Leader::getCreateTime);

        Page<Leader> pageParam = new Page<>(page, limit);
        IPage<Leader> pageModel = leaderMapper.selectPage(pageParam, wrapper);
        Map<Long, String> regionNameCache = new HashMap<>();
        for (Leader leader : pageModel.getRecords()) {
            fillRegionParam(leader, regionNameCache);
        }
        return pageModel;
    }

    private void fillRegionParam(Leader leader, Map<Long, String> regionNameCache) {
        if (leader == null) {
            return;
        }
        putRegionName(leader.getParam(), "provinceName", leader.getProvince(), regionNameCache);
        putRegionName(leader.getParam(), "cityName", leader.getCity(), regionNameCache);
        putRegionName(leader.getParam(), "districtName", leader.getDistrict(), regionNameCache);

        String regionName = resolveRegionName(leader.getRegionId(), regionNameCache);
        if (!StringUtils.hasText(regionName)) {
            regionName = resolveRegionName(leader.getDistrict(), regionNameCache);
        }
        if (!StringUtils.hasText(regionName)) {
            regionName = resolveRegionName(leader.getCity(), regionNameCache);
        }
        if (!StringUtils.hasText(regionName)) {
            regionName = resolveRegionName(leader.getProvince(), regionNameCache);
        }
        leader.getParam().put("regionName", regionName);
    }

    private void putRegionName(Map<String, Object> param, String key, Long regionId, Map<Long, String> regionNameCache) {
        param.put(key, resolveRegionName(regionId, regionNameCache));
    }

    private String resolveRegionName(Long regionId, Map<Long, String> regionNameCache) {
        if (regionId == null) {
            return "";
        }
        if (regionNameCache.containsKey(regionId)) {
            return regionNameCache.get(regionId);
        }
        String name = "";
        try {
            name = jdbcTemplate.queryForObject("select name from region where id = ?", new Object[]{regionId}, String.class);
        } catch (Exception ignored) {
        }
        regionNameCache.put(regionId, name == null ? "" : name);
        return regionNameCache.get(regionId);
    }
}
