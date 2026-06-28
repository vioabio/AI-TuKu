package com.vio.aitukuviobe.infrastructure.api;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.vio.aitukuviobe.infrastructure.config.CosClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 设置公开读权限，使图片可被浏览器直接访问
        putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种图片的处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        // 图片处理规则列表
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 1. 图片压缩（转成 webp 格式，保留目录路径）
        String keyWithoutExt = key.substring(0, key.lastIndexOf('.'));
        String webpKey = keyWithoutExt + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);
        // 2. 缩略图处理，仅对 > 20 KB 的图片生成缩略图
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            // 拼接缩略图的路径（保留目录路径）
            String thumbnailKey = keyWithoutExt + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            // 缩放规则 /thumbnail/<Width>x<Height>>（如果大于原图宽高，则不处理）
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            rules.add(thumbnailRule);
        }
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        try {
            log.info("开始上传图片到COS(万象CI): bucket={}, key={}, size={} bytes",
                    cosClientConfig.getBucket(), key, file.length());
            PutObjectResult result = cosClient.putObject(putObjectRequest);
            log.info("COS上传成功(万象CI): key={}", key);
            return result;
        } catch (CosServiceException e) {
            log.error("COS上传失败: statusCode={}, errorCode={}, message={}",
                    e.getStatusCode(), e.getErrorCode(), e.getMessage());
            // 如果万象CI CAM角色未配置，自动降级为基础上传
            if (e.getMessage() != null && e.getMessage().contains("role not exist")) {
                log.warn("万象CI角色未配置，降级为基础上传（无图片处理）。请前往 https://console.cloud.tencent.com/cam/role 创建 CI_QCSrole 角色");
                PutObjectRequest simpleRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
                simpleRequest.setCannedAcl(CannedAccessControlList.PublicRead);
                PutObjectResult result = cosClient.putObject(simpleRequest);
                log.info("COS基础上传成功: key={}", key);
                return result;
            }
            throw e;
        }
    }

    /**
     * 删除对象
     *
     * @param key 唯一键
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
