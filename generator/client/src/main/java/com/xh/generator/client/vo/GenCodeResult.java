package com.xh.generator.client.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 代码生成结果
 * sunxh 2024/11/16
 */
@Data
@Schema(title = "代码生成结果表")
public class GenCodeResult {

    @Schema(title = "源码类型")
    private String type;

    @Schema(title = "源码")
    private String code;

    @Schema(title = "模板路径")
    private String templatePath;

    @Schema(title = "文件名")
    private String fileName;

    @Schema(title = "zip路径")
    private String zipPath;

    @Schema(title = "绝对文件夹路径")
    private String absoluteDirPath;

    @Schema(title = "绝对路径")
    private String absolutePath;

    @Schema(title = "错误信息")
    private String errorMsg;

    @Schema(title = "是否执行")
    private Boolean execute = true;
}
