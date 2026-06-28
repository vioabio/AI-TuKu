package com.vio.aitukuviobe.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vio.aitukuviobe.exception.BusinessException;
import com.vio.aitukuviobe.exception.ErrorCode;
import com.vio.aitukuviobe.exception.ThrowUtils;
import com.vio.aitukuviobe.mapper.PictureMapper;
import com.vio.aitukuviobe.model.dto.space.analyze.*;
import com.vio.aitukuviobe.model.entity.Picture;
import com.vio.aitukuviobe.model.entity.Space;
import com.vio.aitukuviobe.model.entity.User;
import com.vio.aitukuviobe.model.vo.*;
import com.vio.aitukuviobe.service.PictureService;
import com.vio.aitukuviobe.service.SpaceAnalyzeService;
import com.vio.aitukuviobe.service.SpaceService;
import com.vio.aitukuviobe.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 空间分析服务实现
 */
@Service
public class SpaceAnalyzeServiceImpl implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    /**
     * 根据分析范围补充查询条件
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest request, QueryWrapper<Picture> queryWrapper) {
        if (request.isQueryAll()) {
            return;
        }
        if (request.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = request.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定分析范围");
        }
    }

    /**
     * 校验空间分析权限
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest request, User loginUser) {
        if (request.isQueryAll() || request.isQueryPublic()) {
            // 查询全部/公共图库，仅管理员可以访问
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        } else {
            // 查询指定空间，校验空间权限
            Long spaceId = request.getSpaceId();
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(
            SpaceUsageAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        if (request.isQueryAll() || request.isQueryPublic()) {
            boolean isAdmin = userService.isAdmin(loginUser);
            ThrowUtils.throwIf(!isAdmin, ErrorCode.NO_AUTH_ERROR, "无权访问空间");
            // 统计公共图库的资源使用（只查 picSize 列，提高性能）
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            if (!request.isQueryAll()) {
                queryWrapper.isNull("spaceId");
            }
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = pictureObjList.stream()
                    .mapToLong(result -> result instanceof Long ? (Long) result : 0).sum();
            long usedCount = pictureObjList.size();
            SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
            response.setUsedSize(usedSize);
            response.setUsedCount(usedCount);
            response.setMaxSize(null);
            response.setSizeUsageRatio(null);
            response.setMaxCount(null);
            response.setCountUsageRatio(null);
            return response;
        } else {
            Long spaceId = request.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);

            SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
            response.setUsedSize(space.getTotalSize());
            response.setMaxSize(space.getMaxSize());
            double sizeUsageRatio = NumberUtil.round(
                    space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            response.setSizeUsageRatio(sizeUsageRatio);
            response.setUsedCount(space.getTotalCount());
            response.setMaxCount(space.getMaxCount());
            double countUsageRatio = NumberUtil.round(
                    space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            response.setCountUsageRatio(countUsageRatio);
            return response;
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(
            SpaceCategoryAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyzeAuth(request, loginUser);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(request, queryWrapper);

        queryWrapper.select("category AS category",
                        "COUNT(*) AS count",
                        "SUM(picSize) AS totalSize")
                .groupBy("category");

        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = result.get("category") != null
                            ? result.get("category").toString() : "未分类";
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(
            SpaceTagAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyzeAuth(request, loginUser);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(request, queryWrapper);

        // 只查询 tags 字段
        queryWrapper.select("tags");
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());

        // 合并所有标签并统计使用次数
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        // 转换为响应对象，按使用次数降序排序
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(
            SpaceSizeAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyzeAuth(request, loginUser);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(request, queryWrapper);

        // 只查询 picSize 字段
        queryWrapper.select("picSize");
        List<Long> picSizes = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number) size).longValue())
                .collect(Collectors.toList());

        // 使用 LinkedHashMap 保持分段顺序
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizes.stream()
                .filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizes.stream()
                .filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizes.stream()
                .filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizes.stream()
                .filter(size -> size >= 1 * 1024 * 1024).count());

        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(
            SpaceUserAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyzeAuth(request, loginUser);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = request.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillAnalyzeQueryWrapper(request, queryWrapper);

        // 根据时间维度选择不同的 MySQL 函数
        String timeDimension = request.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period",
                        "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period",
                        "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period",
                        "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }

        queryWrapper.groupBy("period").orderByAsc("period");

        List<Map<String, Object>> queryResult =
                pictureService.getBaseMapper().selectMaps(queryWrapper);
        return queryResult.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Space> getSpaceRankAnalyze(
            SpaceRankAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 仅管理员可查看
        ThrowUtils.throwIf(!userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR, "无权查看空间排行");

        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + request.getTopN());

        return spaceService.list(queryWrapper);
    }
}
