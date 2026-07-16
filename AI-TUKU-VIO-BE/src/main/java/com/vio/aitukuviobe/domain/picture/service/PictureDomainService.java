package com.vio.aitukuviobe.domain.picture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.vio.aitukuviobe.interfaces.dto.picture.*;
import com.vio.aitukuviobe.interfaces.vo.PictureVO;

import java.util.List;

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

    /**
     * ES 全文搜索图片（支持关键词、分类、标签组合搜索）
     * ES 不可用时自动降级为 MySQL LIKE
     *
     * @param searchText 搜索关键词
     * @param category   分类（可选）
     * @param tags       标签列表（可选）
     * @param spaceId    空间 ID（可选，null=公共图库）
     * @param current    当前页码
     * @param size       每页大小
     * @return 分页结果
     */
    Page<Picture> searchPictures(String searchText, String category, List<String> tags,
                                  Long spaceId, long current, long size);

    /**
     * 图片保存/更新后同步到 ES
     */
    void syncPictureToEs(Picture picture);

    /**
     * 图片删除后从 ES 移除
     */
    void deletePictureFromEs(Long pictureId);
}
