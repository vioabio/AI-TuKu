package com.vio.aitukuviobe.testutil;

import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.domain.picture.repository.PictureRepository;
import com.vio.aitukuviobe.domain.picture.valueobject.PictureReviewStatusEnum;
import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.space.entity.SpaceUser;
import com.vio.aitukuviobe.domain.space.repository.SpaceRepository;
import com.vio.aitukuviobe.domain.space.repository.SpaceUserRepository;
import com.vio.aitukuviobe.domain.space.valueobject.SpaceLevelEnum;
import com.vio.aitukuviobe.domain.space.valueobject.SpaceRoleEnum;
import com.vio.aitukuviobe.domain.space.valueobject.SpaceTypeEnum;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.repository.UserRepository;
import com.vio.aitukuviobe.domain.user.valueobject.UserRoleEnum;

import java.util.Date;
import java.util.UUID;

/**
 * 测试数据工厂 — Builder 模式，消除测试样板代码
 *
 * 使用方式：
 * <pre>
 *   User user = TestDataFactory.aUser().build();
 *   User saved = TestDataFactory.createAndSaveUser(userRepository);
 *   Picture picture = TestDataFactory.aPicture(spaceId, userId).build();
 * </pre>
 */
public class TestDataFactory {

    // ==================== User ====================

    public static User aUser() {
        String account = "test_" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUserAccount(account);
        user.setUserPassword("4602b74663bbec93abbf9259fc18b817"); // MD5("vio" + "12345678")
        user.setUserName("测试用户");
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        return user;
    }

    public static User anAdminUser() {
        User user = aUser();
        user.setUserRole(UserRoleEnum.ADMIN.getValue());
        user.setUserName("管理员");
        return user;
    }

    public static User createAndSaveUser(UserRepository repo) {
        User user = aUser();
        repo.save(user);
        return user;
    }

    // ==================== Picture ====================

    public static Picture aPicture(Long spaceId, Long userId) {
        Picture picture = new Picture();
        picture.setUrl("https://cos.example.com/bucket/" + UUID.randomUUID() + ".jpg");
        picture.setThumbnailUrl("https://cos.example.com/bucket/" + UUID.randomUUID() + "_thumb.jpg");
        picture.setName("测试图片");
        picture.setIntroduction("这是一张测试用图片");
        picture.setCategory("自然");
        picture.setTags("[\"测试\",\"示例\"]");
        picture.setPicSize(102400L);
        picture.setPicWidth(1920);
        picture.setPicHeight(1080);
        picture.setPicScale(1.777);
        picture.setPicFormat("jpg");
        picture.setPicColor("#FF5733");
        picture.setUserId(userId);
        picture.setSpaceId(spaceId);
        picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        picture.setCreateTime(new Date());
        picture.setUpdateTime(new Date());
        return picture;
    }

    public static Picture aReviewedPicture(Long spaceId, Long userId) {
        Picture picture = aPicture(spaceId, userId);
        picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        picture.setReviewMessage("审核通过");
        picture.setReviewerId(userId);
        picture.setReviewTime(new Date());
        return picture;
    }

    public static Picture createAndSavePicture(PictureRepository repo, Long spaceId, Long userId) {
        Picture picture = aPicture(spaceId, userId);
        repo.save(picture);
        return picture;
    }

    // ==================== Space ====================

    public static Space aSpace(Long userId) {
        Space space = new Space();
        space.setSpaceName("测试空间_" + UUID.randomUUID().toString().substring(0, 6));
        space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        space.setMaxSize(SpaceLevelEnum.COMMON.getMaxSize());
        space.setMaxCount(SpaceLevelEnum.COMMON.getMaxCount());
        space.setTotalSize(0L);
        space.setTotalCount(0L);
        space.setUserId(userId);
        space.setCreateTime(new Date());
        space.setUpdateTime(new Date());
        return space;
    }

    public static Space aTeamSpace(Long userId) {
        Space space = aSpace(userId);
        space.setSpaceType(SpaceTypeEnum.TEAM.getValue());
        space.setSpaceName("团队空间_" + UUID.randomUUID().toString().substring(0, 6));
        return space;
    }

    public static Space createAndSaveSpace(SpaceRepository repo, Long userId) {
        Space space = aSpace(userId);
        repo.save(space);
        return space;
    }

    // ==================== SpaceUser ====================

    public static SpaceUser aSpaceUser(Long spaceId, Long userId) {
        SpaceUser spaceUser = new SpaceUser();
        spaceUser.setSpaceId(spaceId);
        spaceUser.setUserId(userId);
        spaceUser.setSpaceRole(SpaceRoleEnum.VIEWER.getValue());
        spaceUser.setCreateTime(new Date());
        spaceUser.setUpdateTime(new Date());
        return spaceUser;
    }

    public static SpaceUser anAdminSpaceUser(Long spaceId, Long userId) {
        SpaceUser spaceUser = aSpaceUser(spaceId, userId);
        spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
        return spaceUser;
    }

    public static SpaceUser createAndSaveSpaceUser(SpaceUserRepository repo, Long spaceId, Long userId) {
        SpaceUser spaceUser = aSpaceUser(spaceId, userId);
        repo.save(spaceUser);
        return spaceUser;
    }
}
