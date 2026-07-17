package com.vio.aitukuviobe.domain.picture.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.domain.picture.repository.PictureRepository;
import com.vio.aitukuviobe.domain.picture.service.PictureDomainService;
import com.vio.aitukuviobe.domain.picture.service.PictureSearchService;
import com.vio.aitukuviobe.domain.picture.valueobject.PictureReviewStatusEnum;
import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.space.repository.SpaceRepository;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.repository.UserRepository;
import com.vio.aitukuviobe.domain.user.service.UserDomainService;
import com.vio.aitukuviobe.infrastructure.api.CosManager;
import com.vio.aitukuviobe.infrastructure.ai.model.ModerationResult;
import com.vio.aitukuviobe.infrastructure.ai.model.OutPaintingResult;
import com.vio.aitukuviobe.infrastructure.ai.model.TagsResult;
import com.vio.aitukuviobe.infrastructure.api.aliyunai.AliYunAiApi;
import com.vio.aitukuviobe.infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.vio.aitukuviobe.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.vio.aitukuviobe.domain.picture.service.ModerationAiService;
import com.vio.aitukuviobe.domain.picture.service.PictureAiService;
import com.vio.aitukuviobe.domain.picture.service.SearchAiService;
import com.vio.aitukuviobe.infrastructure.exception.BusinessException;
import com.vio.aitukuviobe.infrastructure.exception.ErrorCode;
import com.vio.aitukuviobe.infrastructure.exception.ThrowUtils;
import com.vio.aitukuviobe.infrastructure.manager.upload.FilePictureUpload;
import com.vio.aitukuviobe.infrastructure.manager.upload.PictureUploadTemplate;
import com.vio.aitukuviobe.infrastructure.manager.upload.UrlPictureUpload;
import com.vio.aitukuviobe.infrastructure.manager.upload.model.dto.file.UploadPictureResult;
import com.vio.aitukuviobe.interfaces.dto.picture.*;
import com.vio.aitukuviobe.interfaces.vo.PictureVO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class PictureDomainServiceImpl implements PictureDomainService {

    @Resource
    private UserRepository userRepository;

    @Resource
    private UserDomainService userDomainService;

    @Resource
    private PictureRepository pictureRepository;

    @Resource
    private SpaceRepository spaceRepository;

    @Resource
    private CosManager cosManager;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private PictureSearchService pictureSearchService;

    // ============================================================
    // LangChain4j AI Services (第 20 章 — AI 框架工程化)
    // 由 LangChain4jConfig 通过 AiServices.builder() 创建代理
    // ============================================================
    @Resource
    private PictureAiService pictureAiService;

    @Resource
    private ModerationAiService moderationAiService;

    @Resource
    private SearchAiService searchAiService;

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public PictureVO uploadPicture(Object inputSource,
                                   PictureUploadRequest pictureUploadRequest,
                                   User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(inputSource == null, ErrorCode.PARAMS_ERROR, "图片为空");

        Long pictureId = null;
        Long spaceId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
            spaceId = pictureUploadRequest.getSpaceId();
        }
        if (pictureId != null) {
            Picture oldPicture = pictureRepository.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userDomainService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }

        if (spaceId != null) {
            Space space = spaceRepository.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
        }

        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        } else if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getName())) {
            picName = pictureUploadRequest.getName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(loginUser.getId());
        picture.setSpaceId(spaceId);
        if (spaceId == null) {
            fillReviewParams(picture, loginUser);
        } else {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        }

        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        final Long finalSpaceId = spaceId;
        Picture finalPicture = picture;
        transactionTemplate.execute(status -> {
            boolean result = pictureRepository.saveOrUpdate(finalPicture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片数据保存失败");
            if (finalSpaceId != null) {
                boolean update = spaceRepository.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + finalPicture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        // 异步同步到 ES 索引（不影响主流程）
        pictureSearchService.syncToEs(picture);
        return PictureVO.objToVo(picture);
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture oldPicture = pictureRepository.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = pictureRepository.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userDomainService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
            if (StrUtil.isBlank(namePrefix)) {
                namePrefix = searchText;
            }
            if (StrUtil.isNotBlank(namePrefix)) {
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        picture.setEditTime(new Date());
        this.validPicture(picture);
        long id = pictureEditRequest.getId();
        Picture oldPicture = pictureRepository.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        checkPictureAuth(loginUser, oldPicture);
        fillReviewParams(picture, loginUser);
        boolean result = pictureRepository.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 异步同步到 ES 索引
        pictureSearchService.syncToEs(picture);
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Picture oldPicture = pictureRepository.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        checkPictureAuth(loginUser, oldPicture);
        transactionTemplate.execute(status -> {
            boolean result = pictureRepository.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                boolean update = spaceRepository.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        // 从 ES 索引中移除
        pictureSearchService.deleteFromEs(pictureId);
        this.clearPictureFile(oldPicture);
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        String pictureUrl = oldPicture.getUrl();
        long count = pictureRepository.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        if (count > 1) return;
        cosManager.deleteObject(pictureUrl);
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();
        if (spaceId == null) {
            if (!picture.getUserId().equals(loginUserId) && !userDomainService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            if (!picture.getUserId().equals(loginUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(
            CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(pictureRepository.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        checkPictureAuth(loginUser, picture);
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        taskRequest.setParameters(createPictureOutPaintingTaskRequest.getParameters());
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }

    // ============================================================
    // LangChain4j AI 方法 (第 20 章)
    // ============================================================

    @Override
    public OutPaintingResult createOutPaintingTask(
            CreatePictureOutPaintingTaskRequest request, User loginUser) {
        Long pictureId = request.getPictureId();
        Picture picture = Optional.ofNullable(pictureRepository.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        checkPictureAuth(loginUser, picture);

        // 一行调用 — 框架自动处理 HTTP + JSON + 重试 + 错误处理
        var params = request.getParameters();
        OutPaintingResult result = pictureAiService.outPainting(
                picture.getUrl(),
                params.getXScale() != null ? params.getXScale() : 1.0,
                params.getYScale() != null ? params.getYScale() : 1.0
        );

        log.info("[LangChain4j] 扩图任务已创建: pictureId={}, taskId={}, status={}",
                pictureId, result.getTaskId(), result.getTaskStatus());
        return result;
    }

    @Override
    public TagsResult extractTags(Picture picture) {
        if (picture == null) {
            return TagsResult.builder()
                    .tags(Collections.emptyList())
                    .confidence(0.0)
                    .build();
        }

        try {
            TagsResult result = pictureAiService.extractTags(
                    picture.getName(),
                    picture.getCategory() != null ? picture.getCategory() : "未分类",
                    picture.getIntroduction() != null ? picture.getIntroduction() : ""
            );

            log.info("[LangChain4j] 标签提取完成: pictureId={}, tags={}, category={}, confidence={}",
                    picture.getId(), result.getTags(), result.getSuggestedCategory(), result.getConfidence());
            return result;
        } catch (Exception e) {
            log.error("[LangChain4j] 标签提取失败: pictureId={}", picture.getId(), e);
            return TagsResult.builder()
                    .tags(Collections.emptyList())
                    .confidence(0.0)
                    .build();
        }
    }

    @Override
    public ModerationResult moderateContent(Picture picture) {
        if (picture == null) {
            return ModerationResult.builder().safe(true).confidence(1.0).build();
        }

        try {
            ModerationResult result = moderationAiService.moderateContent(
                    picture.getName(),
                    picture.getIntroduction() != null ? picture.getIntroduction() : "",
                    picture.getTags() != null ? picture.getTags() : "[]",
                    picture.getCategory() != null ? picture.getCategory() : "未分类"
            );

            log.info("[LangChain4j] 内容审核完成: pictureId={}, safe={}, confidence={}, action={}",
                    picture.getId(), result.getSafe(), result.getConfidence(), result.getSuggestedAction());
            return result;
        } catch (Exception e) {
            log.error("[LangChain4j] 内容审核失败: pictureId={}", picture.getId(), e);
            // 审核失败默认安全（人工兜底）
            return ModerationResult.builder()
                    .safe(true)
                    .confidence(0.0)
                    .suggestedAction("MANUAL_REVIEW")
                    .reason("AI 审核服务异常，转人工审核")
                    .build();
        }
    }

    @Override
    public void autoTagAndModerate(Picture picture) {
        // 1. 自动标签提取
        TagsResult tagsResult = extractTags(picture);
        if (tagsResult.getTags() != null && !tagsResult.getTags().isEmpty()) {
            picture.setTags(JSONUtil.toJsonStr(tagsResult.getTags()));
            if (tagsResult.getSuggestedCategory() != null && picture.getCategory() == null) {
                picture.setCategory(tagsResult.getSuggestedCategory());
            }
            pictureRepository.updateById(picture);
            log.info("[LangChain4j] 自动标签已写入: pictureId={}, tags={}", picture.getId(), tagsResult.getTags());
        }

        // 2. 内容审核
        ModerationResult moderation = moderateContent(picture);
        if (moderation.getSafe() != null && !moderation.getSafe()) {
            if (moderation.getConfidence() != null && moderation.getConfidence() >= 0.9) {
                // 高置信度违规 → 自动拒绝
                picture.setReviewStatus(PictureReviewStatusEnum.REJECT.getValue());
                picture.setReviewMessage("AI 审核拒绝: " + moderation.getReason());
                log.warn("[LangChain4j] 图片自动拒绝: pictureId={}, reason={}", picture.getId(), moderation.getReason());
            } else {
                // 低置信度 → 待人工审核
                picture.setReviewStatus(PictureReviewStatusEnum.PENDING.getValue());
                picture.setReviewMessage("AI 审查标记: " + moderation.getReason());
                log.info("[LangChain4j] 图片标记待审: pictureId={}, reason={}", picture.getId(), moderation.getReason());
            }
            pictureRepository.updateById(picture);
        } else {
            // 内容安全 → 自动通过
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("AI 审核通过");
            pictureRepository.updateById(picture);
            log.info("[LangChain4j] 图片自动通过: pictureId={}", picture.getId());
        }
    }

    @Override
    public Page<Picture> searchPictures(String searchText, String category, List<String> tags,
                                         Long spaceId, long current, long size) {
        return pictureSearchService.advancedSearch(searchText, category, tags, spaceId, 1, current, size);
    }

    @Override
    public void syncPictureToEs(Picture picture) {
        pictureSearchService.syncToEs(picture);
    }

    @Override
    public void deletePictureFromEs(Long pictureId) {
        pictureSearchService.deleteFromEs(pictureId);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) return queryWrapper;
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText).or().like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }
}
