package cn.itedus.ssyx.model.search;

import lombok.Data;

@Data
public class LeaderEs {
    private Long id;
    private String takeName;
    private Double latitude;
    private Double longitude;
    private String storePath;
    private String detailAddress;
    private Double distance;
}

