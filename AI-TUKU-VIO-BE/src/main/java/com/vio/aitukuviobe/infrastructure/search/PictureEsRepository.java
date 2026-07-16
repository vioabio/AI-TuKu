package com.vio.aitukuviobe.infrastructure.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Picture Elasticsearch Repository
 * <p>
 * 继承 ElasticsearchRepository 获得基础 CRUD 能力，
 * Spring Data 自动根据方法名生成查询。
 *
 * @author vivin
 */
@Repository
public interface PictureEsRepository extends ElasticsearchRepository<PictureEsDocument, Long> {

    /**
     * 全文搜索：同时搜索 name 和 introduction 字段
     */
    Page<PictureEsDocument> findByNameOrIntroduction(String name, String introduction, Pageable pageable);

    /**
     * 按分类搜索
     */
    Page<PictureEsDocument> findByCategory(String category, Pageable pageable);

    /**
     * 按标签搜索（精确匹配标签列表中的任一标签）
     */
    Page<PictureEsDocument> findByTagsIn(List<String> tags, Pageable pageable);

    /**
     * 按空间 ID + 审核状态搜索
     */
    Page<PictureEsDocument> findBySpaceIdAndReviewStatus(Long spaceId, Integer reviewStatus, Pageable pageable);

    /**
     * 按用户 ID 搜索
     */
    Page<PictureEsDocument> findByUserId(Long userId, Pageable pageable);

    /**
     * 按空间 ID 搜索（空间内所有图片）
     */
    Page<PictureEsDocument> findBySpaceId(Long spaceId, Pageable pageable);

    /**
     * 按名称模糊搜索
     */
    Page<PictureEsDocument> findByNameLike(String name, Pageable pageable);

    /**
     * 删除指定 ID 列表的文档
     */
    void deleteByIdIn(List<Long> ids);
}