package com.vio.aitukuviobe.service;

import com.vio.aitukuviobe.model.dto.space.analyze.*;
import com.vio.aitukuviobe.model.entity.Space;
import com.vio.aitukuviobe.model.entity.User;
import com.vio.aitukuviobe.model.vo.*;

import java.util.List;

/**
 * 空间分析服务接口
 */
public interface SpaceAnalyzeService {

    /**
     * 空间资源使用分析
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(
            SpaceUsageAnalyzeRequest request, User loginUser);

    /**
     * 空间图片分类分析
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(
            SpaceCategoryAnalyzeRequest request, User loginUser);

    /**
     * 空间图片标签分析
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(
            SpaceTagAnalyzeRequest request, User loginUser);

    /**
     * 空间图片大小分析
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(
            SpaceSizeAnalyzeRequest request, User loginUser);

    /**
     * 用户上传行为分析
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(
            SpaceUserAnalyzeRequest request, User loginUser);

    /**
     * 空间使用排行分析
     */
    List<Space> getSpaceRankAnalyze(
            SpaceRankAnalyzeRequest request, User loginUser);
}
