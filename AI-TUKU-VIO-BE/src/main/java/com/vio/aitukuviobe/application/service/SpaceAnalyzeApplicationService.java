package com.vio.aitukuviobe.application.service;

import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.interfaces.dto.space.analyze.*;
import com.vio.aitukuviobe.interfaces.vo.*;

import java.util.List;

/**
 * 空间分析应用服务接口
 */
public interface SpaceAnalyzeApplicationService {

    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest request, User loginUser);

    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest request, User loginUser);

    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest request, User loginUser);

    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest request, User loginUser);

    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest request, User loginUser);

    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest request, User loginUser);
}
