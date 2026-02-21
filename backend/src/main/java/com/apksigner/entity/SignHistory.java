package com.apksigner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sign_history")
public class SignHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String originalFilename;
    private String signedFilename;
    private String filePath;
    private String signedFilePath;
    private String signType;
    private String keyAlias;
    private Integer validityYears;
    private String storePassword;
    private String keyPassword;
    private Long fileSize;
    private String status;
    private String errorMessage;
    private String downloadCode;
    private String batchId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
