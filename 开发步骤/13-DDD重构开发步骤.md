# DDD 重构 - 开发步骤指南

本文档基于"智能协同云图库项目教程"第13节整理，涵盖 DDD 领域驱动设计理论、三层架构向四层架构转化、以及项目实际 DDD 重构的完整步骤。

---

## 一、DDD 领域驱动设计概念

### 什么是 DDD？

DDD（Domain-Driven Design，领域驱动设计）是一种**软件开发方法论和设计思想**，通过领域模型确定业务和应用的边界，保证业务模型和代码模型的一致性。

- DDD 是设计思想，**确定业务和应用的边界**
- 微服务架构需要**将系统拆分为多个小而独立的服务**
- DDD 指导我们根据领域模型确定业务边界，最终落实成服务的边界和代码的边界

#### DDD 的适用场景

- 业务复杂的系统（金融、电商等）
- 多个部门或团队协作的大型项目
- 长周期、长期维护的项目

**总结：大型的、跨部门协作的、长期维护的复杂项目。**

#### DDD 的两大建设阶段

| 阶段 | 说明 |
|------|------|
| **战略设计** | 从业务出发，事件风暴 → 发散讨论 → 收敛聚类 → 形成聚合、限界上下文 |
| **战术设计** | 从技术实现出发，将领域模型映射为代码（聚合、实体、值对象、领域服务） |

### DDD 核心名词解析

| 概念 | 说明 | 代码映射 |
|------|------|----------|
| **领域** | 系统关注的业务范围，可划分为核心域/通用域/支撑域 | - |
| **限界上下文** | 领域边界，确保术语和规则不与其他上下文冲突 | 微服务边界 |
| **实体** | 有唯一标识的业务对象（如订单、用户） | Entity 类（充血模型） |
| **值对象** | 无唯一标识，创建后不可变，只能整体替换（如地址） | 枚举、ValueObject |
| **聚合** | 多个实体和值对象的组合，保证数据一致性 | 领域内关联的实体组 |
| **聚合根** | 聚合内的"带头人"，对外统一提供接口 | 如 Order 聚合根 |
| **领域服务** | 不能归属于单一实体的跨实体业务逻辑 | DomainService 类 |

**DDD 建模范式总结**：事件风暴 → 找出实体和值对象 → 确定聚合根 → 形成聚合 → 划分限界上下文。

---

## 二、DDD 架构设计

### 充血模型 vs 贫血模型

| 特点 | 贫血模型 | 充血模型 |
|------|----------|----------|
| 封装性 | 数据和逻辑分离 | 数据和逻辑封装在同一对象 |
| 职责分离 | Service 负责业务逻辑，Entity 负责数据 | Entity 同时负责数据和自身业务逻辑 |
| 适用场景 | 简单 CRUD、DTO 传输 | 复杂领域逻辑和业务建模 |
| 优点 | 简单易用，职责清晰 | 高内聚，符合面向对象思想 |
| 缺点 | Service 层臃肿 | 复杂度增加 |

在本项目 DDD 重构中，**将实体相关的校验逻辑下沉到实体类中**。

### DDD 四层架构

| 传统三层 | DDD 四层 | 职责 |
|----------|----------|------|
| Controller | **interfaces（用户接口层）** | 处理 HTTP 请求，调用应用服务，返回视图/数据 |
| - | **application（应用层）** | 编排领域服务，控制事务，为接口层提供调用支持 |
| Service | **domain（领域层）** | 核心业务逻辑，实体类（充血模型）+ 领域服务 |
| Repository/Mapper | **infrastructure（基础设施层）** | 数据库交互、外部服务、工具类等 |

**依赖规则**：严格来说每层只能与直接下层产生依赖。领域层只能被应用层调用，应用层只能被接口层调用。

### DDD 代码架构目录结构

```
src/main/java/com/yupi/yupicture/
├── interfaces/                    # 用户接口层
│   ├── controller/                # REST Controller
│   ├── dto/                       # 请求封装类（DTO）
│   ├── vo/                        # 响应封装类（VO）
│   └── assembler/                 # 转换器（DTO ↔ Entity）
├── application/                   # 应用层
│   └── service/                   # 应用服务（编排领域服务）
├── domain/                        # 领域层
│   ├── user/
│   │   ├── entity/               # 实体类（充血模型）
│   │   ├── service/              # 领域服务
│   │   ├── repository/           # 数据库接口（依赖倒置）
│   │   └── valueobject/          # 值对象（枚举等）
│   ├── picture/                  # 图片领域
│   └── space/                    # 空间领域
├── infrastructure/               # 基础设施层
│   ├── repository/               # Repository 实现
│   ├── mapper/                   # MyBatis Mapper
│   └── manager/                  # 通用管理器
└── shared/                       # 跨领域公共服务
    ├── auth/                     # 鉴权
    ├── websocket/                # WebSocket
    └── sharding/                 # 分表
```

---

## 三、项目 DDD 重构

### 改造方案

#### 领域划分

| 领域 | 核心实体 | 说明 |
|------|----------|------|
| 用户领域 (user) | User | 用户注册、登录、信息管理 |
| 图片领域 (picture) | Picture | 图片上传、编辑、审核 |
| 空间领域 (space) | Space、SpaceUser | 空间管理、成员管理 |

#### 重构核心原则

- **依赖倒置**：领域层定义 Repository 接口 → infrastructure 层实现
- **方法下沉**：业务逻辑从 Controller → ApplicationService → DomainService → Entity
- **事务在应用层**：`@Transactional` 放在 ApplicationService
- **编排在应用层**：ApplicationService 调用多个 DomainService 的方法
- **跨领域调用走应用层**：DomainService 不应调用 ApplicationService
- **实体用充血模型**：与实体自身强相关的逻辑下沉到 Entity 类

---

### 第一步：新建项目结构

创建 DDD 四层架构的包结构。

---

### 第二步：基础设施层

- 将 `mapper` 包移动到 `infrastructure.mapper` 下
- 将 `FileManager` 不依赖应用服务的通用 Manager 移至 `infrastructure.manager`
- 将 `model.dto.file` 随同 FileManager 一起移动

---

### 第三步：用户领域重构

#### 1、重构 model 包

| 原始包 | 重构后的包 | 备注 |
|--------|-----------|------|
| model.entity (User) | domain.user.entity | User 实体类 |
| model.enums (UserRoleEnum 等) | domain.user.valueobject | 枚举类值对象 |
| model.dto.user | interfaces.dto.user | 请求封装类 |
| model.vo (UserVO 等) | interfaces.vo.user | 响应封装类 |

#### 2、重构 constant 包

将用户常亮 `UserConstant` 移动到 `domain.user.valueobject`。

#### 3、重构数据访问层（依赖倒置）

**领域层定义接口**：
```java
package com.yupi.yupicture.domain.user.repository;

public interface UserRepository extends IService<User> {
}
```

**基础设施层实现**：
```java
package com.yupi.yupicture.infrastructure.repository;

@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {
}
```

#### 4、重构 Service

**小技巧：利用 IDE 重构**

- **先移动** Service 接口和实现类到应用服务层（IDE 会自动重构所有调用处）
- **再复制** Service 接口和实现类为领域服务层

| 原始类 | 重构后的类 |
|--------|-----------|
| service.UserService | application.service.UserApplicationService |
| service.impl.UserServiceImpl | application.service.impl.UserApplicationServiceImpl |
| service.UserService（复制） | domain.user.service.UserDomainService |
| service.impl.UserServiceImpl（复制） | domain.user.service.impl.UserDomainServiceImpl |

**方法下沉规则**：
- 不调用其他应用服务的方法 → 下沉到 DomainService
- 调用其他应用服务的方法 → 保留在 ApplicationService
- 不涉及数据库的校验逻辑 → 下沉到 Entity 类

**给 User 实体补充方法（充血模型）**：
```java
/**
 * 校验用户注册
 */
public static void validUserRegister(String userAccount, String userPassword, String checkPassword) {
    if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
    }
    if (userAccount.length() < 4) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
    }
    if (userPassword.length() < 8 || checkPassword.length() < 8) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
    }
    if (!userPassword.equals(checkPassword)) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
    }
}

/**
 * 校验用户登录
 */
public static void validUserLogin(String userAccount, String userPassword) {
    if (StrUtil.hasBlank(userAccount, userPassword)) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
    }
    if (userAccount.length() < 4) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
    }
    if (userPassword.length() < 8) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
    }
}

/**
 * 是否为管理员
 */
public boolean isAdmin() {
    return UserRoleEnum.ADMIN.getValue().equals(this.getUserRole());
}
```

**应用服务层示例**（编排领域服务 + 控制事务）：
```java
@Service
@Slf4j
public class UserApplicationServiceImpl implements UserApplicationService {

    @Resource
    private UserDomainService userDomainService;

    @Override
    @Transactional
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        // 校验（调用实体方法）
        User.validUserRegister(userAccount, userPassword, checkPassword);
        // 注册（调用领域服务）
        return userDomainService.userRegister(userAccount, userPassword, checkPassword);
    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        User.validUserLogin(userAccount, userPassword);
        return userDomainService.userLogin(userAccount, userPassword, request);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        return userDomainService.getLoginUser(request);
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return userDomainService.userLogout(request);
    }

    // ... 其他方法类似，调用领域服务或保留跨领域逻辑
}
```

**领域服务层示例**（调用 Repository）：
```java
@Service
@Slf4j
public class UserDomainServiceImpl implements UserDomainService {

    @Resource
    private UserRepository userRepository;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 检查是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userRepository.getBaseMapper().selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = userRepository.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }
    // ... 其他方法
}
```

#### 5、重构 Controller

将 UserController 移动到 `interfaces.controller`，编写 **Assembler 转换器**：

```java
/**
 * 用户对象转换
 */
public class UserAssembler {
    public static User toUserEntity(UserAddRequest request) {
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return user;
    }

    public static User toUserEntity(UserUpdateRequest request) {
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return user;
    }
}
```

**精简后的 Controller**（调用 ApplicationService 和 Assembler）：
```java
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserApplicationService userApplicationService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        long result = userApplicationService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest,
            HttpServletRequest request) {
        LoginUserVO loginUserVO = userApplicationService.userLogin(userLoginRequest, request);
        return ResultUtils.success(loginUserVO);
    }

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User userEntity = UserAssembler.toUserEntity(userAddRequest);
        return ResultUtils.success(userApplicationService.addUser(userEntity));
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User userEntity = UserAssembler.toUserEntity(userUpdateRequest);
        userApplicationService.updateUser(userEntity);
        return ResultUtils.success(true);
    }

    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        Page<UserVO> userVOPage = userApplicationService.listUserVOByPage(userQueryRequest);
        return ResultUtils.success(userVOPage);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userApplicationService.getLoginUser(request);
        return ResultUtils.success(userApplicationService.getLoginUserVO(user));
    }

    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        User user = userApplicationService.getUserById(id);
        return ResultUtils.success(user);
    }

    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        return ResultUtils.success(userApplicationService.getUserVOById(id));
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        boolean b = userApplicationService.deleteUser(deleteRequest);
        return ResultUtils.success(b);
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = userApplicationService.userLogout(request);
        return ResultUtils.success(result);
    }
}
```

#### 6、其他代码兼容

- `isAdmin` 调用改为对象方法：`loginUser.isAdmin()`
- 给 ApplicationService 补充其他应用服务需要的方法（如 listByIds）
- 其他应用服务尽量调用应用服务层方法，而非领域服务层

---

### 第四步：图片领域重构

#### 1、重构 model 包

| 原始包 | 重构后的包 |
|--------|-----------|
| model.entity (Picture) | domain.picture.entity |
| model.enums (PictureReviewStatusEnum) | domain.picture.valueobject |
| model.dto.picture | interfaces.dto.picture |
| model.vo (PictureVO, PictureTagCategory) | interfaces.vo.picture |

#### 2、重构数据访问层

```java
package com.yupi.yupicture.domain.picture.repository;

public interface PictureRepository extends IService<Picture> {
}
```

```java
package com.yupi.yupicture.infrastructure.repository;

@Service
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {
}
```

#### 3、重构 Service

| 原始类 | 重构后的类 |
|--------|-----------|
| service.PictureService | application.service.PictureApplicationService |
| service.impl.PictureServiceImpl | application.service.impl.PictureApplicationServiceImpl |
| service.PictureService（复制） | domain.picture.service.PictureDomainService |
| service.impl.PictureServiceImpl（复制） | domain.picture.service.impl.PictureDomainServiceImpl |

**方法下沉规则**：
- `getPictureVO` / `getPictureVOPage` 调用了 `userApplicationService` → 保留在 PictureApplicationService
- 其他方法下沉到 PictureDomainService

**PictureApplicationServiceImpl**：
```java
@Service
@Slf4j
public class PictureApplicationServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureApplicationService {

    @Resource
    private PictureDomainService pictureDomainService;
    @Resource
    private UserApplicationService userApplicationService;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        return pictureDomainService.uploadPicture(inputSource, pictureUploadRequest, loginUser);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        return pictureDomainService.getQueryWrapper(pictureQueryRequest);
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userApplicationService.getUserById(userId);
            UserVO userVO = userApplicationService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) return pictureVOPage;
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 批量关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userApplicationService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    // ... 其他方法委托给 pictureDomainService
}
```

**PictureDomainServiceImpl** 核心方法（调用 PictureRepository）：
```java
@Service
@Slf4j
public class PictureDomainServiceImpl implements PictureDomainService {
    @Resource
    private PictureRepository pictureRepository;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 如果是更新，校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = pictureRepository.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // 事务中保存
        transactionTemplate.execute(status -> {
            boolean result = pictureRepository.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            return null;
        });
        // ...
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        Picture oldPicture = pictureRepository.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        transactionTemplate.execute(status -> {
            boolean result = pictureRepository.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            return true;
        });
    }

    @Override
    public void editPicture(Picture picture, User loginUser) {
        Picture oldPicture = pictureRepository.getById(id);
        boolean result = pictureRepository.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Picture oldPicture = pictureRepository.getById(id);
        boolean result = pictureRepository.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    // ... 其他方法
}
```

#### 4、重构 Controller

**PictureAssembler 转换器**：
```java
public class PictureAssembler {
    public static Picture toPictureEntity(PictureEditRequest request) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(request, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(request.getTags()));
        return picture;
    }

    public static Picture toPictureEntity(PictureUpdateRequest request) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(request, picture);
        picture.setTags(JSONUtil.toJsonStr(request.getTags()));
        return picture;
    }
}
```

**精简后的 PictureController**：
```java
@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private PictureApplicationService pictureApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        PictureVO pictureVO = pictureApplicationService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureApplicationService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
            HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        Picture picture = PictureAssembler.toPictureEntity(pictureEditRequest);
        pictureApplicationService.editPicture(picture, loginUser);
        return ResultUtils.success(true);
    }

    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        Picture picture = pictureApplicationService.getById(id);
        // 获取权限列表
        User loginUser = userApplicationService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureApplicationService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        return ResultUtils.success(pictureVO);
    }

    // ... 其他接口类似
}
```

---

### 第五步：空间领域重构

#### 1、重构 model 包

| 原始包 | 重构后的包 |
|--------|-----------|
| model.entity (Space, SpaceUser) | domain.space.entity |
| model.enums (SpaceLevelEnum, SpaceRoleEnum, SpaceTypeEnum) | domain.space.valueobject |
| model.dto.space | interfaces.dto.space |
| model.vo (SpaceVO, SpaceUserVO) | interfaces.vo.space |

#### 2、重构数据访问层

```java
package com.yupi.yupicture.domain.space.repository;

public interface SpaceRepository extends IService<Space> {
}

public interface SpaceUserRepository extends IService<SpaceUser> {
}
```

```java
package com.yupi.yupicture.infrastructure.repository;

@Service
public class SpaceRepositoryImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceRepository {
}

@Service
public class SpaceUserRepositoryImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserRepository {
}
```

#### 3、重构 Service

| 原始类 | 重构后的类 |
|--------|-----------|
| service.SpaceService | application.service.SpaceApplicationService |
| service.SpaceUserService | application.service.SpaceUserApplicationService |
| service.SpaceAnalyzeService | application.service.SpaceAnalyzeApplicationService |

领域服务层（复制）：
| 原始类 | 重构后的类 |
|--------|-----------|
| service.SpaceService（复制） | domain.space.service.SpaceDomainService |
| service.SpaceUserService（复制） | domain.space.service.SpaceUserDomainService |

> 不需要 SpaceAnalyzeDomainService，因为分析功能依赖的是 Space 和 Picture 应用服务，没有独立的分析表。

**方法下沉规则**：
- `getSpaceUserVOList`、`getSpaceUserVO`、`validSpaceUser`、`addSpaceUser`、`getSpaceVOPage`、`getSpaceVO`、`addSpace` 调用了其他应用服务 → 保留在 ApplicationService
- 其他方法 → 下沉到 DomainService

**SpaceApplicationServiceImpl**：
```java
@Service
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceApplicationService {

    @Resource
    private SpaceDomainService spaceDomainService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    @Lazy
    private SpaceUserApplicationService spaceUserApplicationService;

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 保留原本实现（涉及多应用服务调用）
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 保留原本实现（涉及 userApplicationService 调用）
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        // 保留原本实现
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        spaceDomainService.fillSpaceBySpaceLevel(space);
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        spaceDomainService.checkSpaceAuth(loginUser, space);
    }
}
```

**SpaceDomainServiceImpl**：
```java
@Service
public class SpaceDomainServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceDomainService {

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        // 保留原有实现
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) space.setMaxSize(maxSize);
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) space.setMaxCount(maxCount);
        }
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        // 保留原有实现
    }
}
```

> 上述代码中还可以进一步将 `checkSpaceAuth` 中的校验逻辑下沉到 Space 实体类。

#### 4、重构 Controller

**Assembler 转换器**：
```java
public class SpaceAssembler {
    public static Space toSpaceEntity(SpaceAddRequest request) {
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }

    public static Space toSpaceEntity(SpaceUpdateRequest request) {
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }

    public static Space toSpaceEntity(SpaceEditRequest request) {
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }
}

public class SpaceUserAssembler {
    public static SpaceUser toSpaceUserEntity(SpaceUserAddRequest request) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(request, spaceUser);
        return spaceUser;
    }

    public static SpaceUser toSpaceUserEntity(SpaceUserEditRequest request) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(request, spaceUser);
        return spaceUser;
    }
}
```

---

### 第六步：公共服务重构

| 原始包 | 重构后 | 说明 |
|--------|--------|------|
| manager (FileManager) | infrastructure.manager | 不依赖应用服务，放基础设施层 |
| auth、websocket、sharding | shared | 跨领域公共服务 |

---

### 第七步：剩余代码整理

- 将 FileController、MainController 等移至 `interfaces.controller`
- **重构完成后，修改所有包名引用**：Mapper 扫描路径、配置文件中的算法路径、接口文档路径等

---

## 四、开发顺序总结

| 顺序 | 步骤 | 内容 |
|------|------|------|
| 1 | 新建项目结构 | 创建 interfaces / application / domain / infrastructure / shared 包 |
| 2 | 基础设施层 | 移动 mapper → infrastructure.mapper |
| 3 | 用户领域-model | 移动 entity/enums/dto/vo 到对应位置 |
| 4 | 用户领域-repository | UserRepository 接口 + UserRepositoryImpl 实现 |
| 5 | 用户领域-service | 移动 Service → ApplicationService + 复制 → DomainService |
| 6 | 用户领域-方法下沉 | 校验逻辑→Entity，数据库操作→DomainService，编排→ApplicationService |
| 7 | 用户领域-controller | UserAssembler + 精简 Controller |
| 8 | 用户领域-兼容 | 修复 isAdmin 调用等编译错误 |
| 9 | 图片领域 | 同用户领域流程（model/repository/service/controller） |
| 10 | 空间领域 | 同用户领域流程（model/repository/service/controller） |
| 11 | 公共服务 | Manager 按依赖程度分到 infrastructure 或 shared |
| 12 | 剩余代码 | FileController 等移动 + 全局包名替换 |
| 13 | 启动验证 | 编译通过即可（配置未完全改完的话启动可能报错也正常） |

---

## 五、关键技巧总结

| 技巧 | 说明 |
|------|------|
| **IDE 自动重构** | 先移动 Service 到 application 层，IDE 自动更新所有调用处 |
| **复制再删减** | 从原 Service 复制出 DomainService，再按需删除不用的方法 |
| **继承 MyBatis Plus** | ApplicationService 或 DomainService 可继承 IService/ServiceImpl 减少样板代码 |
| **方法下沉判断** | 不调用其他应用服务 → 下沉到 DomainService；调用其他应用服务 → 保留在 ApplicationService |
| **实体充血模式** | 不涉及数据库的校验逻辑 → 下沉到实体类静态方法或实例方法 |
| **事务放应用层** | `@Transactional` 统一放 ApplicationService |
| **依赖倒置** | 领域层定义 Repository 接口 → infrastructure 层实现 |
| **跨领域调应用层** | DomainService 不应调用 ApplicationService，跨领域编排在 ApplicationService 完成 |
