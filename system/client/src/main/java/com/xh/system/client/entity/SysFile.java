package com.xh.system.client.entity;

import com.xh.common.core.entity.BaseDataPermissionEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Table
@Data
@Schema(title = "系统文件")
@EqualsAndHashCode(callSuper = true)
public class SysFile extends BaseDataPermissionEntity<Integer> {

    @Schema(title = "对象存储key")
    private String object;

    @Schema(title = "文件名")
    private String name;

    @Schema(title = "文件类型")
    private String contentType;

    @Schema(title = "文件后缀扩展名")
    private String suffix;

    @Schema(title = "文件大小")
    private Long size;

    @Schema(title = "视频抽帧预览图，上传时会尝试抽第10帧图片作为视频文件的预览图")
    private Integer previewImageFileId;

    @Schema(title = "图片宽度")
    private Integer imgWidth;

    @Schema(title = "图片高度")
    private Integer imgHeight;

    @Schema(title = "图片宽高比")
    private Double imgRatio;

    @Schema(title = "文件状态", allowableValues = {"1", "2", "3", "4"}, description = "1：正常，2:：禁用，3：标记删除，4：已删除")
    private Integer status;

    @Schema(title = "文件摘要sha1")
    private String sha1;
}
