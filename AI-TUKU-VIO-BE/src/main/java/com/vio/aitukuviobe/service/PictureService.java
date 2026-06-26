package com.vio.aitukuviobe.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vio.aitukuviobe.model.dto.picture.PictureEditRequest;
import com.vio.aitukuviobe.model.dto.picture.PictureQueryRequest;
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
     * 上传图片
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

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
}