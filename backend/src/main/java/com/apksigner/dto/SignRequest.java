package com.apksigner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class SignRequest {

    @NotNull(message = "文件ID不能为空")
    private Long historyId;

    @NotBlank(message = "签名类型不能为空")
    private String signType;

    private String keyAlias = "apk-key";

    @Min(value = 1, message = "有效期最少1年")
    @Max(value = 50, message = "有效期最多50年")
    private Integer validityYears = 25;

    private String storePassword = "android";
    private String keyPassword = "android";
}
