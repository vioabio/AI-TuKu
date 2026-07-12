package com.vio.aitukuviobe.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.space.repository.SpaceRepository;
import com.vio.aitukuviobe.domain.space.valueobject.SpaceLevelEnum;
import com.vio.aitukuviobe.domain.space.valueobject.SpaceTypeEnum;
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

@DisplayName("SpaceRepository 集成测试")
class SpaceRepositoryImplTest extends BaseIntegrationTest {

    @Resource
    private SpaceRepository spaceRepository;

    @Resource
    private UserRepository userRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        spaceRepository.lambdaUpdate().remove();
        userRepository.lambdaUpdate().remove();
        User user = TestDataFactory.createAndSaveUser(userRepository);
        userId = user.getId();
    }

    @Test
    @DisplayName("创建空间 → 可查询")
    void shouldCreateAndFindSpace() {
        Space space = TestDataFactory.aSpace(userId);
        spaceRepository.save(space);

        Space found = spaceRepository.getById(space.getId());

        assertThat(found).isNotNull();
        assertThat(found.getSpaceName()).isEqualTo(space.getSpaceName());
        assertThat(found.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("按 userId 查询 → 返回用户的空间列表")
    void shouldQueryByUserId() {
        spaceRepository.save(TestDataFactory.aSpace(userId));
        spaceRepository.save(TestDataFactory.aSpace(userId));

        LambdaQueryWrapper<Space> wrapper = new LambdaQueryWrapper<Space>()
                .eq(Space::getUserId, userId);
        List<Space> spaces = spaceRepository.list(wrapper);

        assertThat(spaces).hasSize(2);
    }

    @Test
    @DisplayName("按 spaceType 过滤 → 正确区分私有空间和团队空间")
    void shouldFilterBySpaceType() {
        Space privateSpace = TestDataFactory.aSpace(userId);
        privateSpace.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        spaceRepository.save(privateSpace);

        Space teamSpace = TestDataFactory.aTeamSpace(userId);
        spaceRepository.save(teamSpace);

        LambdaQueryWrapper<Space> wrapper = new LambdaQueryWrapper<Space>()
                .eq(Space::getSpaceType, SpaceTypeEnum.TEAM.getValue());
        List<Space> teamSpaces = spaceRepository.list(wrapper);

        assertThat(teamSpaces).hasSize(1);
        assertThat(teamSpaces.get(0).getSpaceType()).isEqualTo(SpaceTypeEnum.TEAM.getValue());
    }

    @Test
    @DisplayName("更新空间 → 字段应反映最新值")
    void shouldUpdateSpaceFields() {
        Space space = TestDataFactory.aSpace(userId);
        spaceRepository.save(space);

        space.setSpaceName("新空间名");
        space.setMaxSize(500000L);
        spaceRepository.updateById(space);

        Space updated = spaceRepository.getById(space.getId());
        assertThat(updated.getSpaceName()).isEqualTo("新空间名");
        assertThat(updated.getMaxSize()).isEqualTo(500000L);
    }

    @Test
    @DisplayName("检查私有空间唯一性 → 同用户同类型可找到")
    void shouldFindDuplicatePrivateSpace() {
        Space space = TestDataFactory.aSpace(userId);
        space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        spaceRepository.save(space);

        boolean exists = spaceRepository.lambdaQuery()
                .eq(Space::getUserId, userId)
                .eq(Space::getSpaceType, SpaceTypeEnum.PRIVATE.getValue())
                .exists();

        assertThat(exists).isTrue();
    }
}
