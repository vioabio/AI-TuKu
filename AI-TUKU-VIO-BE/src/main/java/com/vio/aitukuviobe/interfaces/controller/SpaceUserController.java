package com.vio.aitukuviobe.interfaces.controller;

import com.vio.aitukuviobe.infrastructure.common.BaseResponse;
import com.vio.aitukuviobe.infrastructure.common.DeleteRequest;
import com.vio.aitukuviobe.infrastructure.common.ResultUtils;
import com.vio.aitukuviobe.infrastructure.exception.BusinessException;
import com.vio.aitukuviobe.infrastructure.exception.ErrorCode;
import com.vio.aitukuviobe.infrastructure.exception.ThrowUtils;
import com.vio.aitukuviobe.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.vio.aitukuviobe.interfaces.dto.spaceuser.SpaceUserEditRequest;
import com.vio.aitukuviobe.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.vio.aitukuviobe.domain.space.entity.SpaceUser;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.interfaces.vo.SpaceUserVO;
import com.vio.aitukuviobe.application.service.SpaceUserApplicationService;
import com.vio.aitukuviobe.application.service.UserApplicationService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    @PostMapping("/add")
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest,
                                           HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        long id = spaceUserApplicationService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest,
                                                  HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        boolean result = spaceUserApplicationService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/get")
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        SpaceUser spaceUser = spaceUserApplicationService.getOne(
                spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest));
        return ResultUtils.success(spaceUser);
    }

    @PostMapping("/list")
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
                                                          HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        List<SpaceUser> spaceUserList = spaceUserApplicationService.list(
                spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest));
        return ResultUtils.success(spaceUserApplicationService.getSpaceUserVOList(spaceUserList));
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest,
                                                HttpServletRequest request) {
        if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        boolean result = spaceUserApplicationService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserApplicationService.list(
                spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest));
        return ResultUtils.success(spaceUserApplicationService.getSpaceUserVOList(spaceUserList));
    }
}
