package com.vio.aitukuviobe.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vio.aitukuviobe.exception.BusinessException;
import com.vio.aitukuviobe.exception.ErrorCode;
import com.vio.aitukuviobe.exception.ThrowUtils;
import com.vio.aitukuviobe.mapper.SpaceUserMapper;
import com.vio.aitukuviobe.model.dto.spaceuser.SpaceUserAddRequest;
import com.vio.aitukuviobe.model.dto.spaceuser.SpaceUserQueryRequest;
import com.vio.aitukuviobe.model.entity.Space;
import com.vio.aitukuviobe.model.entity.SpaceUser;
import com.vio.aitukuviobe.model.entity.User;
import com.vio.aitukuviobe.model.enums.SpaceRoleEnum;
import com.vio.aitukuviobe.model.vo.SpaceUserVO;
import com.vio.aitukuviobe.model.vo.SpaceVO;
import com.vio.aitukuviobe.model.vo.UserVO;
import com.vio.aitukuviobe.service.SpaceService;
import com.vio.aitukuviobe.service.SpaceUserService;
import com.vio.aitukuviobe.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser, true);
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) return queryWrapper;
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        if (CollUtil.isEmpty(spaceUserList)) return Collections.emptyList();
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream()
                .map(SpaceUserVO::objToVo).collect(Collectors.toList());
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            User user = userIdUserListMap.containsKey(userId) ? userIdUserListMap.get(userId).get(0) : null;
            spaceUserVO.setUser(userService.getUserVO(user));
            Space space = spaceIdSpaceListMap.containsKey(spaceId) ? spaceIdSpaceListMap.get(spaceId).get(0) : null;
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }
}
