package com.vio.aitukuviobe.domain.picture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.domain.picture.repository.PictureRepository;
import com.vio.aitukuviobe.domain.picture.service.PictureSearchService;
import com.vio.aitukuviobe.infrastructure.search.PictureEsDocument;
import com.vio.aitukuviobe.infrastructure.search.PictureEsRepository;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图片搜索服务实现
 * <p>
 * ES 优先搜索 + MySQL LIKE 降级策略。
 * 搜索字段：name（权重 3）、introduction（权重 2）、tags（权重 1）
 *
 * @author vivin
 */
@Slf4j
@Service
public class PictureSearchServiceImpl implements PictureSearchService {

    @Lazy
    @Resource
    private PictureEsRepository pictureEsRepository;

    @Resource
    private PictureRepository pictureRepository;

    @Lazy
    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    /**
     * ES 可用性标记（懒检测 + 缓存）
     */
    private volatile Boolean esAvailable = null;

    @Override
    public Page<Picture> searchPicture(String searchText, long current, long size) {
        if (StrUtil.isBlank(searchText)) {
            return new Page<>(current, size);
        }

        // ES 优先
        if (isEsAvailable()) {
            try {
                return searchByEs(searchText, null, null, null, null, current, size);
            } catch (Exception e) {
                log.warn("ES 搜索失败，降级为 MySQL LIKE 搜索: {}", e.getMessage());
                esAvailable = false;
            }
        }

        // MySQL LIKE 降级
        return searchByMySqlLike(searchText, current, size);
    }

    @Override
    public Page<Picture> advancedSearch(String searchText, String category, List<String> tags,
                                         Long spaceId, Integer reviewStatus, long current, long size) {
        // ES 优先
        if (isEsAvailable()) {
            try {
                return searchByEs(searchText, category, tags, spaceId, reviewStatus, current, size);
            } catch (Exception e) {
                log.warn("ES 高级搜索失败，降级为 MySQL: {}", e.getMessage());
                esAvailable = false;
            }
        }

        // MySQL 降级
        return searchByMySqlAdvanced(searchText, category, tags, spaceId, reviewStatus, current, size);
    }

    @Override
    public void syncToEs(Picture picture) {
        if (!isEsAvailable()) return;
        try {
            PictureEsDocument doc = toEsDocument(picture);
            pictureEsRepository.save(doc);
            log.debug("图片 {} 已同步到 ES", picture.getId());
        } catch (Exception e) {
            log.error("同步图片 {} 到 ES 失败: {}", picture.getId(), e.getMessage());
            esAvailable = false;
        }
    }

    @Override
    public void deleteFromEs(Long pictureId) {
        if (!isEsAvailable()) return;
        try {
            pictureEsRepository.deleteById(pictureId);
            log.debug("图片 {} 已从 ES 删除", pictureId);
        } catch (Exception e) {
            log.error("从 ES 删除图片 {} 失败: {}", pictureId, e.getMessage());
            esAvailable = false;
        }
    }

    @Override
    public void batchSyncToEs(List<Picture> pictures) {
        if (CollUtil.isEmpty(pictures) || !isEsAvailable()) return;
        try {
            List<PictureEsDocument> docs = pictures.stream()
                    .map(this::toEsDocument)
                    .collect(Collectors.toList());
            pictureEsRepository.saveAll(docs);
            log.info("批量同步 {} 条图片到 ES 完成", docs.size());
        } catch (Exception e) {
            log.error("批量同步到 ES 失败: {}", e.getMessage());
            esAvailable = false;
        }
    }

    @Override
    public PictureEsDocument toEsDocument(Picture picture) {
        return PictureEsDocument.builder()
                .id(picture.getId())
                .name(picture.getName())
                .introduction(picture.getIntroduction())
                .tags(parseTags(picture.getTags()))
                .category(picture.getCategory())
                .picFormat(picture.getPicFormat())
                .picWidth(picture.getPicWidth())
                .picHeight(picture.getPicHeight())
                .picSize(picture.getPicSize())
                .picColor(picture.getPicColor())
                .userId(picture.getUserId())
                .spaceId(picture.getSpaceId())
                .reviewStatus(picture.getReviewStatus())
                .thumbnailUrl(picture.getThumbnailUrl())
                .url(picture.getUrl())
                .createTime(picture.getCreateTime())
                .editTime(picture.getEditTime())
                .build();
    }

    @Override
    public boolean isEsAvailable() {
        if (esAvailable != null) return esAvailable;
        try {
            // 懒检测：尝试 ping ES 集群
            elasticsearchRestTemplate.indexOps(PictureEsDocument.class).exists();
            esAvailable = true;
            log.info("Elasticsearch 连接成功");
        } catch (Exception e) {
            log.warn("Elasticsearch 不可用，将使用 MySQL LIKE 搜索: {}", e.getMessage());
            esAvailable = false;
        }
        return esAvailable;
    }

    // ==================== ES 搜索实现 ====================

    /**
     * ES 全文搜索
     * <p>
     * 搜索策略：
     * <ul>
     *   <li>name 字段权重 3（最相关）</li>
     *   <li>introduction 字段权重 2</li>
     *   <li>tags 字段权重 1</li>
     *   <li>支持拼音搜索（通过 IK + Pinyin 分词器）</li>
     *   <li>支持模糊搜索（fuzziness: AUTO）</li>
     * </ul>
     */
    private Page<Picture> searchByEs(String searchText, String category, List<String> tags,
                                      Long spaceId, Integer reviewStatus, long current, long size) {
        // 构建布尔查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 全文搜索：多字段匹配，加权
        if (StrUtil.isNotBlank(searchText)) {
            boolQuery.must(QueryBuilders.multiMatchQuery(searchText)
                    .field("name", 3.0f)         // 名称权重最高
                    .field("introduction", 2.0f)  // 简介权重次之
                    .field("tags", 1.0f)          // 标签权重最低
                    .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                    .fuzziness("AUTO"));          // 自动模糊匹配
        }

        // 分类过滤
        if (StrUtil.isNotBlank(category)) {
            boolQuery.filter(QueryBuilders.termQuery("category", category));
        }

        // 标签过滤（任一匹配）
        if (CollUtil.isNotEmpty(tags)) {
            boolQuery.filter(QueryBuilders.termsQuery("tags", tags));
        }

        // 空间过滤
        if (spaceId != null) {
            boolQuery.filter(QueryBuilders.termQuery("spaceId", spaceId));
        }

        // 审核状态过滤
        if (reviewStatus != null) {
            boolQuery.filter(QueryBuilders.termQuery("reviewStatus", reviewStatus));
        }

        // 默认只返回审核通过的公开图片（spaceId 为 null）
        if (spaceId == null && reviewStatus == null) {
            boolQuery.filter(QueryBuilders.termQuery("reviewStatus", 1));
        }

        // 构建搜索请求
        int pageIndex = Math.max(0, (int) (current - 1));
        Pageable pageable = PageRequest.of(pageIndex, (int) size, Sort.by(Sort.Direction.DESC, "createTime"));

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(pageable)
                .withTrackTotalHits(true)
                .build();

        // 执行搜索
        SearchHits<PictureEsDocument> searchHits = elasticsearchRestTemplate.search(
                searchQuery, PictureEsDocument.class);

        // 转换为 Picture 分页结果
        List<Picture> pictureList = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toPictureEntity)
                .collect(Collectors.toList());

        Page<Picture> result = new Page<>(current, size);
        result.setRecords(pictureList);
        result.setTotal(searchHits.getTotalHits());
        return result;
    }

    // ==================== MySQL LIKE 降级 ====================

    /**
     * MySQL LIKE 搜索（降级方案）
     */
    private Page<Picture> searchByMySqlLike(String searchText, long current, long size) {
        LambdaQueryWrapper<Picture> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                        .like(Picture::getName, searchText)
                        .or()
                        .like(Picture::getIntroduction, searchText))
                .eq(Picture::getReviewStatus, 1)
                .isNull(Picture::getSpaceId)
                .orderByDesc(Picture::getCreateTime);

        return pictureRepository.page(new Page<>(current, size), wrapper);
    }

    /**
     * MySQL 高级搜索（降级方案）
     */
    private Page<Picture> searchByMySqlAdvanced(String searchText, String category, List<String> tags,
                                                 Long spaceId, Integer reviewStatus, long current, long size) {
        LambdaQueryWrapper<Picture> wrapper = new LambdaQueryWrapper<>();

        // 搜索词
        if (StrUtil.isNotBlank(searchText)) {
            wrapper.and(w -> w
                    .like(Picture::getName, searchText)
                    .or()
                    .like(Picture::getIntroduction, searchText));
        }

        // 分类
        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(Picture::getCategory, category);
        }

        // 标签
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                wrapper.like(Picture::getTags, "\"" + tag + "\"");
            }
        }

        // 空间
        if (spaceId != null) {
            wrapper.eq(Picture::getSpaceId, spaceId);
        } else {
            wrapper.eq(Picture::getReviewStatus, 1);
            wrapper.isNull(Picture::getSpaceId);
        }

        // 审核状态
        if (reviewStatus != null) {
            wrapper.eq(Picture::getReviewStatus, reviewStatus);
        }

        wrapper.orderByDesc(Picture::getCreateTime);

        return pictureRepository.page(new Page<>(current, size), wrapper);
    }

    // ==================== 工具方法 ====================

    /**
     * 解析 JSON 标签字符串为 List
     */
    private List<String> parseTags(String tagsJson) {
        if (StrUtil.isBlank(tagsJson)) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.toList(tagsJson, String.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * ES 文档 → Picture 实体（轻量转换，不含大字段）
     */
    private Picture toPictureEntity(PictureEsDocument doc) {
        Picture picture = new Picture();
        picture.setId(doc.getId());
        picture.setName(doc.getName());
        picture.setIntroduction(doc.getIntroduction());
        picture.setTags(JSONUtil.toJsonStr(doc.getTags()));
        picture.setCategory(doc.getCategory());
        picture.setPicFormat(doc.getPicFormat());
        picture.setPicWidth(doc.getPicWidth());
        picture.setPicHeight(doc.getPicHeight());
        picture.setPicSize(doc.getPicSize());
        picture.setPicColor(doc.getPicColor());
        picture.setUserId(doc.getUserId());
        picture.setSpaceId(doc.getSpaceId());
        picture.setReviewStatus(doc.getReviewStatus());
        picture.setThumbnailUrl(doc.getThumbnailUrl());
        picture.setUrl(doc.getUrl());
        picture.setCreateTime(doc.getCreateTime());
        picture.setEditTime(doc.getEditTime());
        return picture;
    }
}