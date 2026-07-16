package com.vio.aitukuviobe.infrastructure.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Picture Elasticsearch 文档映射
 * <p>
 * 索引名称：picture_index
 * 将 name、introduction、tags、category 等可搜索字段写入 ES，
 * 支持全文搜索、拼音搜索、模糊搜索、标签聚合。
 *
 * @author vivin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "picture_index", createIndex = true)
public class PictureEsDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ES 文档 ID（与 MySQL picture.id 保持一致）
     */
    @Id
    private Long id;

    /**
     * 图片名称（全文搜索 + 拼音搜索）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    /**
     * 图片简介（全文搜索）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String introduction;

    /**
     * 标签列表（关键词搜索 + 聚合）
     */
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    /**
     * 分类（关键词搜索 + 聚合）
     */
    @Field(type = FieldType.Keyword)
    private String category;

    /**
     * 图片格式（过滤条件）
     */
    @Field(type = FieldType.Keyword)
    private String picFormat;

    /**
     * 图片宽度
     */
    @Field(type = FieldType.Integer)
    private Integer picWidth;

    /**
     * 图片高度
     */
    @Field(type = FieldType.Integer)
    private Integer picHeight;

    /**
     * 图片体积（字节）
     */
    @Field(type = FieldType.Long)
    private Long picSize;

    /**
     * 图片主色调
     */
    @Field(type = FieldType.Keyword)
    private String picColor;

    /**
     * 所属用户 ID
     */
    @Field(type = FieldType.Long)
    private Long userId;

    /**
     * 所属空间 ID（null = 公共图库）
     */
    @Field(type = FieldType.Long)
    private Long spaceId;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    @Field(type = FieldType.Integer)
    private Integer reviewStatus;

    /**
     * 缩略图 URL
     */
    @Field(type = FieldType.Keyword, index = false)
    private String thumbnailUrl;

    /**
     * 原始图片 URL
     */
    @Field(type = FieldType.Keyword, index = false)
    private String url;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date)
    private Date createTime;

    /**
     * 编辑时间
     */
    @Field(type = FieldType.Date)
    private Date editTime;
}