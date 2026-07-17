package com.vio.aitukuviobe.domain.picture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.infrastructure.ai.model.ModerationResult;
import com.vio.aitukuviobe.infrastructure.ai.model.OutPaintingResult;
import com.vio.aitukuviobe.infrastructure.ai.model.TagsResult;
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

    /**
     * @deprecated 已由 {@link #createOutPaintingTask(CreatePictureOutPaintingTaskRequest, User)} 替代
     *             使用 LangChain4j AI 框架，返回类型更简洁
     */
    @Deprecated
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(
            CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);

    /**
     * AI 扩图任务 (LangChain4j) — 替代上述 createPictureOutPaintingTask
     *
     * @param request 扩图请求
     * @param loginUser 当前用户
     * @return 扩图结果 (框架自动处理 HTTP/JSON/重试)
     */
    OutPaintingResult createOutPaintingTask(
            CreatePictureOutPaintingTaskRequest request, User loginUser);

    /**
     * AI 自动标签提取 (LangChain4j)
     *
     * @param picture 图片实体
     * @return 标签提取结果 (tags + suggestedCategory + confidence)
     */
    TagsResult extractTags(Picture picture);

    /**
     * AI 内容审核 (LangChain4j)
     *
     * @param picture 图片实体
     * @return 审核结果 (safe/reason/confidence/suggestedAction)
     */
    ModerationResult moderateContent(Picture picture);

    /**
     * AI 批量图片标签提取 + 审核
     *
     * @param picture 图片实体
     */
    void autoTagAndModerate(Picture picture);

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
