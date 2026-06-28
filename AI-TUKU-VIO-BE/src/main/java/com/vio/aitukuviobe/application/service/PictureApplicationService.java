package com.vio.aitukuviobe.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.vio.aitukuviobe.interfaces.dto.picture.*;
import com.vio.aitukuviobe.interfaces.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

/**
 * 图片应用服务接口
 */
public interface PictureApplicationService extends IService<Picture> {

    void validPicture(Picture picture);

    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);

    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    void deletePicture(long pictureId, User loginUser);

    void checkPictureAuth(User loginUser, Picture picture);

    CreateOutPaintingTaskResponse createPictureOutPaintingTask(
            CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
}
