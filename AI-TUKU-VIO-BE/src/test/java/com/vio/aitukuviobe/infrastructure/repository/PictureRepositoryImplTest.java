package com.vio.aitukuviobe.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.domain.picture.repository.PictureRepository;
import com.vio.aitukuviobe.domain.picture.valueobject.PictureReviewStatusEnum;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.repository.UserRepository;
import com.vio.aitukuviobe.testbase.BaseIntegrationTest;
import com.vio.aitukuviobe.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PictureRepository 集成测试")
class PictureRepositoryImplTest extends BaseIntegrationTest {

    @Resource
    private PictureRepository pictureRepository;

    @Resource
    private UserRepository userRepository;

    private Long userId;
    private Long spaceId = 100L;

    @BeforeEach
    void setUp() {
        pictureRepository.lambdaUpdate().remove();
        userRepository.lambdaUpdate().remove();
        User user = TestDataFactory.createAndSaveUser(userRepository);
        userId = user.getId();
    }

    @Test
    @DisplayName("保存图片 → 可通过 ID 查询")
    void shouldSaveAndFindPicture() {
        Picture pic = TestDataFactory.aPicture(spaceId, userId);
        pictureRepository.save(pic);

        Picture found = pictureRepository.getById(pic.getId());

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo(pic.getName());
        assertThat(found.getUrl()).isEqualTo(pic.getUrl());
    }

    @Test
    @DisplayName("按 spaceId 查询 → 应返回空间下所有图片")
    void shouldQueryBySpaceId() {
        pictureRepository.save(TestDataFactory.aPicture(spaceId, userId));
        pictureRepository.save(TestDataFactory.aPicture(spaceId, userId));
        pictureRepository.save(TestDataFactory.aPicture(200L, userId)); // 另一个空间

        LambdaQueryWrapper<Picture> wrapper = new LambdaQueryWrapper<Picture>()
                .eq(Picture::getSpaceId, spaceId);
        List<Picture> pics = pictureRepository.list(wrapper);

        assertThat(pics).hasSize(2);
    }

    @Test
    @DisplayName("按 userId 查询 → 应返回用户自己上传的所有图片")
    void shouldQueryByUserId() {
        pictureRepository.save(TestDataFactory.aPicture(spaceId, userId));

        LambdaQueryWrapper<Picture> wrapper = new LambdaQueryWrapper<Picture>()
                .eq(Picture::getUserId, userId);
        List<Picture> pics = pictureRepository.list(wrapper);

        assertThat(pics).hasSize(1);
    }

    @Test
    @DisplayName("按 reviewStatus 查询 → 应正确过滤")
    void shouldQueryByReviewStatus() {
        Picture pending = TestDataFactory.aPicture(spaceId, userId);
        pending.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        pictureRepository.save(pending);

        Picture passed = TestDataFactory.aPicture(spaceId, userId);
        passed.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        pictureRepository.save(passed);

        LambdaQueryWrapper<Picture> wrapper = new LambdaQueryWrapper<Picture>()
                .eq(Picture::getReviewStatus, PictureReviewStatusEnum.PASS.getValue());
        List<Picture> passedPics = pictureRepository.list(wrapper);

        assertThat(passedPics).hasSize(1);
    }

    @Test
    @DisplayName("分页查询 → 应正确分页")
    void shouldPaginateResults() {
        for (int i = 0; i < 5; i++) {
            pictureRepository.save(TestDataFactory.aPicture(spaceId, userId));
        }

        Page<Picture> page = new Page<>(1, 2);
        Page<Picture> result = pictureRepository.page(page);

        assertThat(result.getTotal()).isEqualTo(5);
        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("更新图片字段 → 应反映最新值")
    void shouldUpdatePicture() {
        Picture pic = TestDataFactory.aPicture(spaceId, userId);
        pictureRepository.save(pic);

        pic.setName("修改后的名称");
        pic.setIntroduction("修改后的简介");
        pictureRepository.updateById(pic);

        Picture updated = pictureRepository.getById(pic.getId());
        assertThat(updated.getName()).isEqualTo("修改后的名称");
        assertThat(updated.getIntroduction()).isEqualTo("修改后的简介");
    }

    @Test
    @DisplayName("逻辑删除 → 查不到图片")
    void shouldSoftDeletePicture() {
        Picture pic = TestDataFactory.aPicture(spaceId, userId);
        pictureRepository.save(pic);

        pictureRepository.removeById(pic.getId());

        Picture found = pictureRepository.getById(pic.getId());
        assertThat(found).isNull();
    }
}
