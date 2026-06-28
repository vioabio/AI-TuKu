package com.vio.aitukuviobe.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建图片扩图任务请求（用户侧）
 */
@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {

    /**
     * 图片 ID
     */
    private Long pictureId;

    /**
     * 图像处理参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;
}