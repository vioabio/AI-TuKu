package com.vio.aitukuviobe.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.vio.aitukuviobe.config.CosClientConfig;
import com.vio.aitukuviobe.exception.BusinessException;
import com.vio.aitukuviobe.exception.ErrorCode;
import com.vio.aitukuviobe.manager.CosManager;
import com.vio.aitukuviobe.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 图片上传模板（模板方法模式）
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    protected CosManager cosManager;

    @Resource
    protected CosClientConfig cosClientConfig;

    /**
     * 模板方法，定义上传流程
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片
        validPicture(inputSource);

        // 2. 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        File file = null;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源（本地或 URL）
            processFile(inputSource, file);

            // 4. 上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);

            // 5. 封装返回结果
            return buildResult(originFilename, file, uploadPath, putObjectResult);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6. 清理临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 校验输入源（本地文件或 URL）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 封装返回结果
     * 优先使用万象CI处理后的压缩图和缩略图（ProcessResults）
     * 如果CI不可用则降级使用原始图片信息
     */
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath,
                                            PutObjectResult putObjectResult) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));

        // 尝试从 CI ProcessResults 获取压缩图和缩略图
        if (putObjectResult.getCiUploadResult() != null
                && putObjectResult.getCiUploadResult().getProcessResults() != null) {
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                // 第一个是压缩图（webp格式）
                CIObject compressedCiObject = objectList.get(0);
                int picWidth = compressedCiObject.getWidth();
                int picHeight = compressedCiObject.getHeight();
                double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
                uploadPictureResult.setPicWidth(picWidth);
                uploadPictureResult.setPicHeight(picHeight);
                uploadPictureResult.setPicScale(picScale);
                uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
                uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
                // 设置图片为压缩后的地址（webp格式）
                uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());

                // 第二个是缩略图（仅 >20KB 图片会生成）
                if (objectList.size() > 1) {
                    CIObject thumbnailCiObject = objectList.get(1);
                    uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
                }
                return uploadPictureResult;
            }
        }

        // CI 不可用时的降级处理
        uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
        uploadPictureResult.setPicSize(FileUtil.size(file));

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
            uploadPictureResult.setPicFormat(FileUtil.getSuffix(originFilename));
        }

        // 降级时手动拼接缩略图地址
        String thumbnailPath = uploadPath.substring(0, uploadPath.lastIndexOf('.'))
                + "_thumbnail." + FileUtil.getSuffix(uploadPath);
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + thumbnailPath);

        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
