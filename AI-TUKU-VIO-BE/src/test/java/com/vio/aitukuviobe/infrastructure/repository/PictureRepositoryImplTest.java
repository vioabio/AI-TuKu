package com.vio.aitukuviobe.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.domain.picture.valueobject.PictureReviewStatusEnum;
import com.vio.aitukuviobe.testutil.AbstractIntegrationTest;
import com.vio.aitukuviobe.testutil.TestDataFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PictureRepositoryImpl 集成测试")
class PictureRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private PictureRepositoryImpl pictureRepository;

    @BeforeEach
    void setUp() {
        pictureRepository.remove(new LambdaQueryWrapper<>());
    }

    @Test
    @DisplayName("按 spaceId + reviewStatus 分页查询")
    @Transactional
    void shouldFindBySpaceIdAndReviewStatus() {
        Picture pic = TestDataFactory.aPicture(1L, 1L)
                .reviewStatus(PictureReviewStatusEnum.PASS.getValue()).build();
        pictureRepository.save(pic);

        Page<Picture> page = new Page<>(1, 10);
        LambdaQueryWrapper<Picture> wrapper = new LambdaQueryWrapper<Picture>()
                .eq(Picture::getSpaceId, 1L)
                .eq(Picture::getReviewStatus, PictureReviewStatusEnum.PASS.getValue());
        Page<Picture> result = pictureRepository.page(page, wrapper);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getName()).isEqualTo("测试图片");
    }

    @Test
    @DisplayName("保存图片后应有 ID")
    @Transactional
    void shouldAssignIdAfterSave() {
        Picture pic = TestDataFactory.aPicture(null, 1L).build();
        boolean saved = pictureRepository.save(pic);

        assertThat(saved).isTrue();
        assertThat(pic.getId()).isNotNull();
    }

    @Test
    @DisplayName("按 userId 查询图片")
    @Transactional
    void shouldFindByUserId() {
        Picture pic = TestDataFactory.aPicture(null, 99L).build();
        pictureRepository.save(pic);

        long count = pictureRepository.lambdaQuery()
                .eq(Picture::getUserId, 99L).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("逻辑删除后查询不到")
    @Transactional
    void shouldNotFindAfterLogicalDelete() {
        Picture pic = TestDataFactory.aPicture(null, 1L).build();
        pictureRepository.save(pic);

        pictureRepository.removeById(pic.getId());
        Picture found = pictureRepository.getById(pic.getId());

        assertThat(found).isNull();
    }
}
