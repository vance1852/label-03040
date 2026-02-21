package com.apksigner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;
import java.util.List;

@Data
public class BatchSignRequest {

    @NotEmpty(message = "文件ID列表不能为空")
    private List<Long> historyIds;

    @NotBlank(message = "签名类型不能为空")
    private String signType;

    private String keyAlias = "apk-key";

    @Min(value = 1, message = "有效期最少1年")
    @Max(value = 50, message = "有效期最多50年")
    private Integer validityYears = 25;

    private String storePassword = "android";
    private String keyPassword = "android";
}
