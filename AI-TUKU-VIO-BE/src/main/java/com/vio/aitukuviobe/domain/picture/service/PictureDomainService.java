package com.vio.aitukuviobe.domain.picture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.vio.aitukuviobe.interfaces.dto.picture.*;
import com.vio.aitukuviobe.interfaces.vo.PictureVO;

/**
 * 图片领域服务接口
 */
public interface PictureDomainService {

    void validPicture(Picture picture);

    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);

    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    void deletePicture(long pictureId, User loginUser);

    void clearPictureFile(Picture oldPicture);

    void checkPictureAuth(User loginUser, Picture picture);

    CreateOutPaintingTaskResponse createPictureOutPaintingTask(
            CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);
}
