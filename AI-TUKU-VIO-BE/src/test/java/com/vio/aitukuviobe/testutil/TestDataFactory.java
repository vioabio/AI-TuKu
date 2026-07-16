package com.vio.aitukuviobe.testutil;

import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.domain.picture.repository.PictureRepository;
import com.vio.aitukuviobe.domain.picture.valueobject.PictureReviewStatusEnum;
import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.space.repository.SpaceRepository;
import com.vio.aitukuviobe.domain.space.valueobject.SpaceLevelEnum;
import com.vio.aitukuviobe.domain.space.valueobject.SpaceTypeEnum;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.repository.UserRepository;
import com.vio.aitukuviobe.domain.user.valueobject.UserRoleEnum;

import java.util.Date;
import java.util.UUID;

public class TestDataFactory {

    public static User.UserBuilder aUser() {
        return User.builder()
                .userAccount("test_" + UUID.randomUUID().toString().substring(0, 8))
                .userPassword("encrypted_password_placeholder")
                .userName("测试用户")
                .userRole(UserRoleEnum.USER.getValue())
                .createTime(new Date())
                .updateTime(new Date())
                .isDelete(0);
    }

    public static Picture.PictureBuilder aPicture(Long spaceId, Long userId) {
        return Picture.builder()
                .spaceId(spaceId).userId(userId)
                .url("https://cos.example.com/bucket/" + UUID.randomUUID() + ".jpg")
                .thumbnailUrl("https://cos.example.com/bucket/thumb_" + UUID.randomUUID() + ".jpg")
                .name("测试图片").introduction("这是一张测试图片")
                .tags("[\"测试\",\"示例\"]").category("自然")
                .picWidth(1920).picHeight(1080).picSize(102400L).picScale(16.0 / 9.0)
                .picFormat("jpg").picColor("0xFFFFFF")
                .reviewStatus(PictureReviewStatusEnum.PASS.getValue())
                .createTime(new Date()).editTime(new Date()).updateTime(new Date()).isDelete(0);
    }

    public static Space.SpaceBuilder aSpace(Long userId) {
        return Space.builder().userId(userId).spaceName("测试空间")
                .spaceLevel(SpaceLevelEnum.COMMON.getValue()).spaceType(SpaceTypeEnum.PRIVATE.getValue())
                .maxSize(100L * 1024 * 1024).maxCount(100L).totalSize(0L).totalCount(0L)
                .createTime(new Date()).updateTime(new Date()).isDelete(0);
    }
}
