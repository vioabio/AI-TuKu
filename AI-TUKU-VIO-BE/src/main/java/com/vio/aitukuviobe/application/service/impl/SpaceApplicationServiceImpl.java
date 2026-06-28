package com.vio.aitukuviobe.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vio.aitukuviobe.application.service.SpaceApplicationService;
import com.vio.aitukuviobe.application.service.UserApplicationService;
import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.space.service.SpaceDomainService;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.service.UserDomainService;
import com.vio.aitukuviobe.infrastructure.mapper.SpaceMapper;
import com.vio.aitukuviobe.interfaces.dto.space.SpaceAddRequest;
import com.vio.aitukuviobe.interfaces.dto.space.SpaceQueryRequest;
import com.vio.aitukuviobe.interfaces.vo.SpaceVO;
import com.vio.aitukuviobe.interfaces.vo.UserVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 空间应用服务实现
 */
@Service
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceApplicationService {

    @Resource
    private SpaceDomainService spaceDomainService;

    @Resource
    private UserDomainService userDomainService;

    @Resource
    private UserApplicationService userApplicationService;

    @Override
    public void validSpace(Space space, boolean add) {
        spaceDomainService.validSpace(space, add);
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        spaceDomainService.fillSpaceBySpaceLevel(space);
    }

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        return spaceDomainService.addSpace(spaceAddRequest, loginUser);
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userApplicationService.getById(userId);
            UserVO userVO = userDomainService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        spaceDomainService.checkSpaceAuth(loginUser, space);
    }
}
