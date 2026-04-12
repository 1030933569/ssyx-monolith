package cn.itedus.ssyx.search.service.impl;

import cn.itedus.ssyx.activity.client.ActivityFeignClient;
import cn.itedus.ssyx.common.auth.AuthContextHolder;
import cn.itedus.ssyx.model.product.Category;
import cn.itedus.ssyx.model.product.SkuInfo;
import cn.itedus.ssyx.model.search.SkuEs;
import cn.itedus.ssyx.search.mapper.CategorySearchMapper;
import cn.itedus.ssyx.search.mapper.SkuInfoSearchMapper;
import cn.itedus.ssyx.search.service.SkuService;
import cn.itedus.ssyx.vo.search.SkuEsQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SkuServiceImpl implements SkuService {
    private static final String HOT_SCORE_KEY = "hotScore";
    private static final String HOT_SCORE_MEMBER_PREFIX = "skuId:";

    @Autowired
    private SkuInfoSearchMapper skuInfoSearchMapper;

    @Autowired
    private CategorySearchMapper categorySearchMapper;

    @Autowired
    private ActivityFeignClient activityFeignClient;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public void upperSku(Long skuId) {
        // ES removed in monolith; DB is the source of truth.
    }

    @Override
    public void lowerSku(Long skuId) {
        // ES removed in monolith; DB is the source of truth.
    }

    @Override
    public List<SkuEs> findHotSkuList() {
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(HOT_SCORE_KEY, 0, 9);

        if (CollectionUtils.isEmpty(tuples)) {
            LambdaQueryWrapper<SkuInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SkuInfo::getPublishStatus, 1).orderByDesc(SkuInfo::getSale);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<SkuInfo> mpPage =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
            IPage<SkuInfo> pageResult = skuInfoSearchMapper.selectPage(mpPage, wrapper);
            List<SkuEs> skuEsList = buildSkuEsList(pageResult.getRecords(), Collections.emptyMap());
            attachRuleList(skuEsList);
            return skuEsList;
        }

        List<Long> skuIdOrder = new ArrayList<>();
        Map<Long, Long> hotScoreMap = new HashMap<>();
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Long skuId = parseSkuId(tuple == null ? null : tuple.getValue());
            if (skuId == null) {
                continue;
            }
            skuIdOrder.add(skuId);
            if (tuple.getScore() != null) {
                hotScoreMap.put(skuId, Math.round(tuple.getScore()));
            }
        }
        if (skuIdOrder.isEmpty()) {
            return Collections.emptyList();
        }

        List<SkuInfo> skuInfos = skuInfoSearchMapper.selectBatchIds(skuIdOrder);
        Map<Long, SkuInfo> skuInfoById = skuInfos.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(SkuInfo::getId, Function.identity(), (a, b) -> a));

        Map<Long, String> categoryNameMap = loadCategoryNames(skuInfos);

        List<SkuEs> result = new ArrayList<>();
        for (Long skuId : skuIdOrder) {
            SkuInfo skuInfo = skuInfoById.get(skuId);
            if (skuInfo == null) {
                continue;
            }
            SkuEs skuEs = toSkuEs(skuInfo, categoryNameMap.get(skuInfo.getCategoryId()));
            skuEs.setHotScore(hotScoreMap.getOrDefault(skuId, 0L));
            result.add(skuEs);
        }

        attachRuleList(result);
        return result;
    }

    @Override
    public Page<SkuEs> search(Pageable pageable, SkuEsQueryVo skuEsQueryVo) {
        Long wareId = AuthContextHolder.getWareId();
        if (wareId == null && skuEsQueryVo != null) {
            wareId = skuEsQueryVo.getWareId();
        }
        if (skuEsQueryVo != null) {
            skuEsQueryVo.setWareId(wareId);
        }

        LambdaQueryWrapper<SkuInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkuInfo::getPublishStatus, 1);
        if (wareId != null) {
            wrapper.eq(SkuInfo::getWareId, wareId);
        }
        if (skuEsQueryVo != null && skuEsQueryVo.getCategoryId() != null) {
            wrapper.eq(SkuInfo::getCategoryId, skuEsQueryVo.getCategoryId());
        }
        if (skuEsQueryVo != null && !StringUtils.isEmpty(skuEsQueryVo.getKeyword())) {
            wrapper.like(SkuInfo::getSkuName, skuEsQueryVo.getKeyword());
        }
        wrapper.orderByDesc(SkuInfo::getSort).orderByDesc(SkuInfo::getId);

        long pageNo = pageable == null ? 1 : pageable.getPageNumber() + 1;
        long pageSize = pageable == null ? 10 : pageable.getPageSize();

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SkuInfo> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNo, pageSize);
        IPage<SkuInfo> pageResult = skuInfoSearchMapper.selectPage(mpPage, wrapper);

        List<SkuInfo> skuInfos = pageResult.getRecords();
        Map<Long, Long> hotScoreMap = loadHotScores(skuInfos);
        List<SkuEs> skuEsList = buildSkuEsList(skuInfos, hotScoreMap);
        attachRuleList(skuEsList);

        Pageable safePageable = pageable == null
                ? org.springframework.data.domain.PageRequest.of(0, (int) pageSize)
                : pageable;
        return new PageImpl<>(skuEsList, safePageable, pageResult.getTotal());
    }

    @Override
    public void incrHtScore(Long skuId) {
        redisTemplate.opsForZSet().incrementScore(HOT_SCORE_KEY, HOT_SCORE_MEMBER_PREFIX + skuId, 1);
    }

    private void attachRuleList(List<SkuEs> skuEsList) {
        if (CollectionUtils.isEmpty(skuEsList)) {
            return;
        }
        List<Long> skuIdList = skuEsList.stream().map(SkuEs::getId).collect(Collectors.toList());
        Map<Long, List<String>> skuIdToRuleListMap = activityFeignClient.findActivity(skuIdList);
        if (skuIdToRuleListMap == null) {
            return;
        }
        skuEsList.forEach(skuEs -> skuEs.setRuleList(skuIdToRuleListMap.get(skuEs.getId())));
    }

    private Map<Long, String> loadCategoryNames(List<SkuInfo> skuInfos) {
        if (CollectionUtils.isEmpty(skuInfos)) {
            return Collections.emptyMap();
        }
        Set<Long> categoryIds = skuInfos.stream()
                .map(SkuInfo::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Category> categories = categorySearchMapper.selectBatchIds(categoryIds);
        if (CollectionUtils.isEmpty(categories)) {
            return Collections.emptyMap();
        }
        return categories.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private Map<Long, Long> loadHotScores(List<SkuInfo> skuInfos) {
        if (CollectionUtils.isEmpty(skuInfos)) {
            return Collections.emptyMap();
        }
        Map<Long, Long> hotScoreMap = new HashMap<>();
        for (SkuInfo skuInfo : skuInfos) {
            if (skuInfo == null || skuInfo.getId() == null) {
                continue;
            }
            Double score = redisTemplate.opsForZSet().score(HOT_SCORE_KEY, HOT_SCORE_MEMBER_PREFIX + skuInfo.getId());
            if (score != null) {
                hotScoreMap.put(skuInfo.getId(), Math.round(score));
            }
        }
        return hotScoreMap;
    }

    private List<SkuEs> buildSkuEsList(List<SkuInfo> skuInfos, Map<Long, Long> hotScoreMap) {
        if (CollectionUtils.isEmpty(skuInfos)) {
            return Collections.emptyList();
        }
        Map<Long, String> categoryNameMap = loadCategoryNames(skuInfos);
        return skuInfos.stream()
                .filter(Objects::nonNull)
                .map(skuInfo -> {
                    SkuEs skuEs = toSkuEs(skuInfo, categoryNameMap.get(skuInfo.getCategoryId()));
                    Long hotScore = hotScoreMap.get(skuInfo.getId());
                    if (hotScore != null) {
                        skuEs.setHotScore(hotScore);
                    }
                    return skuEs;
                })
                .collect(Collectors.toList());
    }

    private SkuEs toSkuEs(SkuInfo skuInfo, String categoryName) {
        SkuEs skuEs = new SkuEs();
        skuEs.setId(skuInfo.getId());
        skuEs.setCategoryId(skuInfo.getCategoryId());
        skuEs.setCategoryName(categoryName);
        skuEs.setKeyword(skuInfo.getSkuName() + (categoryName == null ? "" : "," + categoryName));
        skuEs.setWareId(skuInfo.getWareId());
        skuEs.setIsNewPerson(skuInfo.getIsNewPerson());
        skuEs.setImgUrl(skuInfo.getImgUrl());
        skuEs.setTitle(skuInfo.getSkuName());
        skuEs.setStock(skuInfo.getStock());
        skuEs.setSkuType(skuInfo.getSkuType());
        if (skuInfo.getPrice() != null) {
            skuEs.setPrice(skuInfo.getPrice().doubleValue());
        }
        skuEs.setSale(skuInfo.getSale());
        skuEs.setPerLimit(skuInfo.getPerLimit());
        return skuEs;
    }

    private Long parseSkuId(Object member) {
        if (member == null) {
            return null;
        }
        String text = member.toString();
        if (text.startsWith(HOT_SCORE_MEMBER_PREFIX)) {
            text = text.substring(HOT_SCORE_MEMBER_PREFIX.length());
        }
        try {
            return Long.valueOf(text);
        } catch (Exception ignored) {
            return null;
        }
    }
}

