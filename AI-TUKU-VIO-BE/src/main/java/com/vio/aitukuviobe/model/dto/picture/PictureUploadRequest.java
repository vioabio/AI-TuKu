package com.vio.aitukuviobe.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片上传请求
 */
@Data
public class PictureUploadRequest implements Serializable {

    /**
     * id（用于修改）
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片名称（用于批量上传时的自定义命名）
     */
    private String picName;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 文件地址（URL 上传时使用）
     */
    private String fileUrl;

    /**
     * 空间 id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
