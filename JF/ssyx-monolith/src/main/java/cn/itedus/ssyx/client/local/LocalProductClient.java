package cn.itedus.ssyx.client.local;

import cn.itedus.ssyx.client.product.ProductFeignClient;
import cn.itedus.ssyx.model.product.Category;
import cn.itedus.ssyx.model.product.SkuInfo;
import cn.itedus.ssyx.product.service.CategoryService;
import cn.itedus.ssyx.product.service.SkuInfoService;
import cn.itedus.ssyx.vo.product.SkuInfoVo;
import cn.itedus.ssyx.vo.product.SkuStockLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalProductClient implements ProductFeignClient {
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SkuInfoService skuInfoService;

    @Override
    public Category getCategory(Long categoryId) {
        return categoryService.getById(categoryId);
    }

    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        return skuInfoService.getById(skuId);
    }

    @Override
    public List<SkuInfo> findSkuInfoList(List<Long> idList) {
        return skuInfoService.findSkuInfoList(idList);
    }

    @Override
    public List<SkuInfo> findSkuInfoListByKeyword(String keyword) {
        return skuInfoService.findSkuInfoListByKeyword(keyword);
    }

    @Override
    public List<Category> findCategoryList(List<Long> idList) {
        return categoryService.findCategoryList(idList);
    }

    @Override
    public List<Category> findAllCategoryList() {
        return categoryService.findAllList();
    }

    @Override
    public List<SkuInfo> findNewPersonSkuInfoList() {
        return skuInfoService.findNewPersonSkuInfoList();
    }

    @Override
    public SkuInfoVo getSkuInfoById(Long skuId) {
        return skuInfoService.getSkuInfoVo(skuId);
    }

    @Override
    public Boolean checkAndLock(List<SkuStockLockVo> skuStockLockVoList, String orderNo) {
        return skuInfoService.checkAndLock(skuStockLockVoList, orderNo);
    }
}

