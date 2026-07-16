package com.vio.aitukuviobe.domain.picture.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.infrastructure.search.PictureEsDocument;

import java.util.List;

/**
 * 图片搜索服务接口
 * <p>
 * 封装 Elasticsearch 全文搜索逻辑，提供与 MySQL LIKE 搜索的优雅降级。
 * 当 ES 不可用时自动回退到 MySQL LIKE 查询。
 *
 * @author vivin
 */
public interface PictureSearchService {

    /**
     * 全文搜索图片（ES 优先，不可用时降级为 MySQL LIKE）
     *
     * @param searchText 搜索关键词（空格分隔多关键词）
     * @param current    当前页码
     * @param size       每页大小
     * @return 分页结果
     */
    Page<Picture> searchPicture(String searchText, long current, long size);

    /**
     * 高级搜索：关键词 + 分类 + 标签 + 空间 ID
     *
     * @param searchText   搜索关键词（可选）
     * @param category     分类（可选）
     * @param tags         标签列表（可选）
     * @param spaceId      空间 ID（可选）
     * @param reviewStatus 审核状态（可选）
     * @param current      当前页码
     * @param size         每页大小
     * @return 分页结果
     */
    Page<Picture> advancedSearch(String searchText, String category, List<String> tags,
                                  Long spaceId, Integer reviewStatus, long current, long size);

    /**
     * 图片保存/更新时同步到 ES 索引
     *
     * @param picture 图片实体
     */
    void syncToEs(Picture picture);

    /**
     * 图片删除时从 ES 索引中移除
     *
     * @param pictureId 图片 ID
     */
    void deleteFromEs(Long pictureId);

    /**
     * 批量同步到 ES（用于全量重建索引）
     *
     * @param pictures 图片实体列表
     */
    void batchSyncToEs(List<Picture> pictures);

    /**
     * 将 Picture 实体转换为 ES 文档
     */
    PictureEsDocument toEsDocument(Picture picture);

    /**
     * 判断 ES 是否可用
     */
    boolean isEsAvailable();
}