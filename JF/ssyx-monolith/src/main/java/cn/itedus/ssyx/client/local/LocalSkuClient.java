package cn.itedus.ssyx.client.local;

import cn.itedus.ssyx.SkuFeignClient;
import cn.itedus.ssyx.model.search.SkuEs;
import cn.itedus.ssyx.search.service.SkuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalSkuClient implements SkuFeignClient {
    @Autowired
    private SkuService skuService;

    @Override
    public List<SkuEs> findHotSkuList() {
        return skuService.findHotSkuList();
    }

    @Override
    public Boolean incrHotScore(Long skuId) {
        skuService.incrHtScore(skuId);
        return true;
    }
}

