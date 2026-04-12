package cn.itedus.ssyx.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 开团（创建提货点）最简化入参：只填写提货点信息即可
 */
@Data
@ApiModel(description = "开团（创建提货点）入参")
public class LeaderOpenGroupReqVo {

    @ApiModelProperty(value = "提货点名称", required = true)
    private String takeName;

    @ApiModelProperty(value = "详细地址", required = true)
    private String detailAddress;

    @ApiModelProperty(value = "经度")
    private Double longitude;

    @ApiModelProperty(value = "纬度")
    private Double latitude;

    @ApiModelProperty(value = "门店照片")
    private String storePath;
}

