package com.vio.aitukuviobe.interfaces.graphql;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vio.aitukuviobe.application.service.PictureApplicationService;
import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.domain.picture.valueobject.PictureReviewStatusEnum;
import com.vio.aitukuviobe.domain.space.entity.SpaceUser;
import com.vio.aitukuviobe.domain.space.repository.SpaceUserRepository;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.infrastructure.exception.BusinessException;
import com.vio.aitukuviobe.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

/**
 * GraphQL 图片查询/变更控制器 — 对应优化文档 9.1 节
 *
 * <p>替代 REST 多个端点，前端按需取字段：
 * <ul>
 *   <li>GET /api/picture/list      → pictures() 查询</li>
 *   <li>GET /api/picture/{id}      → picture() 查询</li>
 *   <li>POST /api/picture/upload   → uploadPicture() 变更</li>
 *   <li>POST /api/picture/edit     → editPicture() 变更</li>
 *   <li>POST /api/picture/delete   → deletePicture() 变更</li>
 *   <li>POST /api/picture/review   → reviewPicture() 变更</li>
 * </ul>
 *
 * @author vivin
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aituku.graphql.enabled", havingValue = "true")
public class PictureGraphQLController {

    private final PictureApplicationService pictureApplicationService;
    private final SpaceUserRepository spaceUserRepository;

    // ============================================================
    // Query
    // ============================================================

    /**
     * 图片列表查询（替代 GET /api/picture/list）
     *
     * <p>GraphQL 请求示例：
     * <pre>
     * query {
     *   pictures(spaceId: 1, category: "自然", searchText: "风景", page: 1, size: 20) {
     *     total
     *     records {
     *       id
     *       name
     *       url
     *       thumbnailUrl
     *       tags
     *       user { id userName }
     *     }
     *   }
     * }
     * </pre>
     */
    @QueryMapping
    public Map<String, Object> pictures(
        @Argument Long spaceId,
        @Argument String category,
        @Argument List<String> tags,
        @Argument Integer reviewStatus,
        @Argument String searchText,
        @Argument Integer page,
        @Argument Integer size,
        @Argument String sortBy
    ) {
        int current = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? size : 20;

        // 委托给 Application Service（复用现有逻辑）
        Page<Picture> result = pictureApplicationService.searchPictures(
            spaceId, category, tags, reviewStatus, searchText, current, pageSize, sortBy
        );

        return Map.of(
            "total", result.getTotal(),
            "pages", result.getPages(),
            "current", result.getCurrent(),
            "records", result.getRecords()
        );
    }

    /**
     * 单张图片详情
     */
    @QueryMapping
    public Picture picture(@Argument Long id) {
        Picture picture = pictureApplicationService.getPictureById(id);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        return picture;
    }

    // ============================================================
    // Mutation
    // ============================================================

    /**
     * 上传图片（简化版，实际需结合文件上传流程）
     */
    @MutationMapping
    public Picture uploadPicture(@Argument Map<String, Object> input) {
        User loginUser = getLoginUser();
        // 委托给 Application Service
        return pictureApplicationService.uploadPictureFromGraphQL(input, loginUser);
    }

    /**
     * 编辑图片
     */
    @MutationMapping
    public Picture editPicture(@Argument Map<String, Object> input) {
        User loginUser = getLoginUser();
        Long id = Long.valueOf(input.get("id").toString());
        return pictureApplicationService.editPictureFromGraphQL(id, input, loginUser);
    }

    /**
     * 删除图片
     */
    @MutationMapping
    public Boolean deletePicture(@Argument Long id) {
        User loginUser = getLoginUser();
        pictureApplicationService.deletePicture(id, loginUser);
        return true;
    }

    /**
     * 审核图片
     */
    @MutationMapping
    public Picture reviewPicture(
        @Argument Long id,
        @Argument String status,
        @Argument String message
    ) {
        User loginUser = getLoginUser();
        PictureReviewStatusEnum reviewStatus = PictureReviewStatusEnum.valueOf(status);
        return pictureApplicationService.reviewPicture(id, reviewStatus, message, loginUser);
    }

    // ============================================================
    // Schema Mapping — 解决 N+1 (DataLoader)
    // ============================================================

    /**
     * 为 Picture.user 字段批量加载 User 信息
     */
    @SchemaMapping(typeName = "Picture", field = "user")
    public User user(Picture picture) {
        return pictureApplicationService.getUserByPictureId(picture.getUserId());
    }

    /**
     * 为 Picture 计算格式化的文件大小
     */
    @SchemaMapping(typeName = "Picture", field = "picSizeFormatted")
    public String picSizeFormatted(Picture picture) {
        if (picture.getPicSize() == null) return null;
        long size = picture.getPicSize();
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024));
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private User getLoginUser() {
        try {
            return (User) StpUtil.getSession().get("user");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
    }
}
