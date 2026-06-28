package com.vio.aitukuviobe.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vio.aitukuviobe.model.dto.picture.CreateOutPaintingTaskResponse;
import com.vio.aitukuviobe.model.dto.picture.CreatePictureOutPaintingTaskRequest;
import com.vio.aitukuviobe.model.dto.picture.PictureEditRequest;
import com.vio.aitukuviobe.model.dto.picture.PictureQueryRequest;
import com.vio.aitukuviobe.model.dto.picture.PictureReviewRequest;
import com.vio.aitukuviobe.model.dto.picture.PictureUploadByBatchRequest;
import com.vio.aitukuviobe.model.dto.picture.PictureUploadRequest;
import com.vio.aitukuviobe.model.entity.Picture;
import com.vio.aitukuviobe.model.entity.User;
import com.vio.aitukuviobe.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * @description 针对表【picture(图片)】的数据库操作Service
 */
public interface PictureService extends IService<Picture> {

    /**
     * 校验图片
     */
    void validPicture(Picture picture);

    /**
     * 上传图片（支持本地文件和 URL）
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 审核请求
     * @param loginUser            当前登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 补充审核参数
     *
     * @param picture   图片
     * @param loginUser 当前登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 批量请求
     * @param loginUser                   当前登录用户
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    /**
     * 获取图片包装类（单条）
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片包装类（分页）
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 获取查询对象
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 编辑图片
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 删除图片
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 清理图片文件
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 校验图片权限
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 创建 AI 扩图任务
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(
            CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            User loginUser);
}
