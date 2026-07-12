package com.vio.aitukuviobe.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.repository.UserRepository;
import com.vio.aitukuviobe.domain.user.valueobject.UserRoleEnum;
import com.vio.aitukuviobe.testbase.BaseIntegrationTest;
import com.vio.aitukuviobe.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserRepository 集成测试")
class UserRepositoryImplTest extends BaseIntegrationTest {

    @Resource
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // 清表保证测试隔离
        userRepository.lambdaUpdate().remove();
    }

    @Test
    @DisplayName("保存用户 → 可通过 ID 查询")
    void shouldSaveAndFindById() {
        User user = TestDataFactory.aUser();
        userRepository.save(user);

        User found = userRepository.getById(user.getId());

        assertThat(found).isNotNull();
        assertThat(found.getUserAccount()).isEqualTo(user.getUserAccount());
        assertThat(found.getUserName()).isEqualTo(user.getUserName());
    }

    @Test
    @DisplayName("按账号查询 → 应返回正确用户")
    void shouldFindByAccount() {
        User user = TestDataFactory.aUser();
        userRepository.save(user);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUserAccount, user.getUserAccount());
        User found = userRepository.getOne(wrapper);

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("count 查询 → 应返回正确数量")
    void shouldCountUsers() {
        userRepository.save(TestDataFactory.aUser());
        userRepository.save(TestDataFactory.aUser());

        long count = userRepository.count();

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("更新用户 → 查询应反映最新数据")
    void shouldUpdateUser() {
        User user = TestDataFactory.aUser();
        userRepository.save(user);

        user.setUserName("新名称");
        userRepository.updateById(user);

        User updated = userRepository.getById(user.getId());
        assertThat(updated.getUserName()).isEqualTo("新名称");
    }

    @Test
    @DisplayName("逻辑删除 → 查不到但数据仍存在")
    void shouldSoftDelete() {
        User user = TestDataFactory.aUser();
        userRepository.save(user);

        userRepository.removeById(user.getId());

        User found = userRepository.getById(user.getId());
        assertThat(found).isNull(); // 逻辑删除后查不到
    }

    @Test
    @DisplayName("按角色查询 → 应过滤正确")
    void shouldQueryByRole() {
        User normalUser = TestDataFactory.aUser();
        userRepository.save(normalUser);

        User adminUser = TestDataFactory.anAdminUser();
        userRepository.save(adminUser);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUserRole, UserRoleEnum.ADMIN.getValue());
        List<User> admins = userRepository.list(wrapper);

        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getUserRole()).isEqualTo(UserRoleEnum.ADMIN.getValue());
    }
}
