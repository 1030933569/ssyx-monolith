package cn.itedus.ssyx.search.mapper;

import cn.itedus.ssyx.model.product.Category;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategorySearchMapper extends BaseMapper<Category> {
}

