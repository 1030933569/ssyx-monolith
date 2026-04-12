package cn.itedus.ssyx.search.controller;

import cn.itedus.ssyx.common.result.Result;
import cn.itedus.ssyx.model.search.LeaderEs;
import cn.itedus.ssyx.vo.search.LeaderEsQueryVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: 团长搜索服务接口
 * 简化实现：直接从MySQL查询，不使用Elasticsearch
 */
@Api(tags = "团长搜索接口")
@RestController
@RequestMapping("/api/search/leader")
public class LeaderSearchController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ApiOperation("根据位置搜索附近团长")
    @GetMapping("/{page}/{limit}")
    public Result searchLeader(@PathVariable("page") Integer page,
                               @PathVariable("limit") Integer limit,
                               LeaderEsQueryVo leaderEsQueryVo) {
        
        // 计算偏移量
        int offset = (page - 1) * limit;
        
        // 查询团长列表（简化版：不计算距离，直接返回所有审核通过的团长）
        String sql = "SELECT id, take_name as takeName, latitude, longitude, " +
                     "store_path as storePath, detail_address as detailAddress, " +
                     "0 as distance " +
                     "FROM leader " +
                     "WHERE check_status = 1 AND is_deleted = 0 " +
                     "ORDER BY id " +
                     "LIMIT ? OFFSET ?";
        
        List<LeaderEs> leaders = jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(LeaderEs.class), limit, offset);
        
        // 查询总数
        String countSql = "SELECT COUNT(*) FROM leader WHERE check_status = 1 AND is_deleted = 0";
        Integer total = jdbcTemplate.queryForObject(countSql, Integer.class);
        
        // 构建分页结果
        Map<String, Object> result = new HashMap<>();
        result.put("content", leaders);
        result.put("totalElements", total != null ? total : 0);
        result.put("totalPages", total != null ? (int) Math.ceil((double) total / limit) : 0);
        result.put("number", page);
        result.put("size", limit);
        
        return Result.ok(result);
    }
}
