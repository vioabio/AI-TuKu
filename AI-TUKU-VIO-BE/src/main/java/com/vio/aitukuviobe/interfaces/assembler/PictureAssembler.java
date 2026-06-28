package com.vio.aitukuviobe.interfaces.assembler;

import cn.hutool.json.JSONUtil;
import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.interfaces.dto.picture.PictureEditRequest;
import com.vio.aitukuviobe.interfaces.dto.picture.PictureUpdateRequest;
import org.springframework.beans.BeanUtils;

/**
 * 图片装配器（DTO ↔ Entity 转换）
 */
public class PictureAssembler {

    public static Picture toPictureEntity(PictureEditRequest request) {
        if (request == null) return null;
        Picture picture = new Picture();
        BeanUtils.copyProperties(request, picture);
        if (request.getTags() != null) {
            picture.setTags(JSONUtil.toJsonStr(request.getTags()));
        }
        return picture;
    }

    public static Picture toPictureEntity(PictureUpdateRequest request) {
        if (request == null) return null;
        Picture picture = new Picture();
        BeanUtils.copyProperties(request, picture);
        if (request.getTags() != null) {
            picture.setTags(JSONUtil.toJsonStr(request.getTags()));
        }
        return picture;
    }
}
