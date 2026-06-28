package com.vio.aitukuviobe.infrastructure.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.OriginalInfo;
import com.vio.aitukuviobe.infrastructure.api.CosManager;
import com.vio.aitukuviobe.infrastructure.common.ResultUtils;
import com.vio.aitukuviobe.infrastructure.config.CosClientConfig;
import com.vio.aitukuviobe.infrastructure.exception.BusinessException;
import com.vio.aitukuviobe.infrastructure.exception.ErrorCode;
import com.vio.aitukuviobe.infrastructure.exception.ThrowUtils;
import com.vio.aitukuviobe.infrastructure.manager.upload.model.dto.file.UploadPictureResult;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 文件服务
 * @deprecated 已废弃，改为使用 upload 包的模板方法优化
 */
@Slf4j
@Service
@Deprecated
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片
        validPicture(multipartFile);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        // 自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 上传文件
            String fileSuffix = "." + FileUtil.getSuffix(originalFilename);
            file = File.createTempFile("picture_upload_", fileSuffix);
            log.info("临时文件创建成功: {}", file.getAbsolutePath());
            multipartFile.transferTo(file);
            log.info("文件写入临时目录成功, 大小: {} bytes", file.length());
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            // 尝试从CI获取图片信息（万象CI角色未配置时会降级，CI结果为空）
            if (putObjectResult.getCiUploadResult() != null
                    && putObjectResult.getCiUploadResult().getOriginalInfo() != null
                    && putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo() != null) {
                ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
                int picWidth = imageInfo.getWidth();
                int picHeight = imageInfo.getHeight();
                double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
                uploadPictureResult.setPicWidth(picWidth);
                uploadPictureResult.setPicHeight(picHeight);
                uploadPictureResult.setPicScale(picScale);
                uploadPictureResult.setPicFormat(imageInfo.getFormat());
            } else {
                // CI不可用时，使用文件扩展名作为格式
                uploadPictureResult.setPicFormat(FileUtil.getSuffix(originalFilename));
            }
            // 设置缩略图地址（保留目录路径，仅替换扩展名）
            String thumbnailPath = uploadPath.substring(0, uploadPath.lastIndexOf('.')) + "_thumbnail." + FileUtil.getSuffix(uploadPath);
            uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + thumbnailPath);
            // 返回可访问的地址
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            String errMsg = e.getMessage();
            if (errMsg == null || errMsg.isEmpty()) {
                errMsg = e.getClass().getSimpleName();
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败: " + errMsg);
        } finally {
            // 临时文件清理
            this.deleteTempFile(file);
        }

    }

    /**
     * 校验文件
     *
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 20 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 20MB");
        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀列表（或者集合）
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    /**
     * 清理临时文件
     *
     * @param file
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}













