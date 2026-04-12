package cn.itedus.ssyx.model.search;

import lombok.Data;

import java.util.List;

@Data
public class SkuEs {
    private Long id;
    private String keyword;
    private Integer skuType;
    private Integer isNewPerson;
    private Long categoryId;
    private String categoryName;
    private String imgUrl;
    private String title;
    private Double price;
    private Integer stock;
    private Integer perLimit;
    private Integer sale;
    private Long wareId;
    private Long hotScore = 0L;
    private List<String> ruleList;
}

