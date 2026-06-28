package com.vio.aitukuviobe.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vio.aitukuviobe.application.service.SpaceApplicationService;
import com.vio.aitukuviobe.application.service.SpaceUserApplicationService;
import com.vio.aitukuviobe.application.service.UserApplicationService;
import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.space.entity.SpaceUser;
import com.vio.aitukuviobe.domain.space.service.SpaceUserDomainService;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.service.UserDomainService;
import com.vio.aitukuviobe.infrastructure.mapper.SpaceUserMapper;
import com.vio.aitukuviobe.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.vio.aitukuviobe.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.vio.aitukuviobe.interfaces.vo.SpaceUserVO;
import com.vio.aitukuviobe.interfaces.vo.SpaceVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 空间用户应用服务实现
 */
@Service
public class SpaceUserApplicationServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserApplicationService {

    @Resource
    private SpaceUserDomainService spaceUserDomainService;

    @Resource
    private UserDomainService userDomainService;

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        return spaceUserDomainService.addSpaceUser(spaceUserAddRequest);
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        spaceUserDomainService.validSpaceUser(spaceUser, add);
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        return spaceUserDomainService.getQueryWrapper(spaceUserQueryRequest);
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        if (CollUtil.isEmpty(spaceUserList)) return Collections.emptyList();
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream()
                .map(SpaceUserVO::objToVo).collect(Collectors.toList());
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceApplicationService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            User user = userIdUserListMap.containsKey(userId) ? userIdUserListMap.get(userId).get(0) : null;
            spaceUserVO.setUser(userDomainService.getUserVO(user));
            Space space = spaceIdSpaceListMap.containsKey(spaceId) ? spaceIdSpaceListMap.get(spaceId).get(0) : null;
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }
}
