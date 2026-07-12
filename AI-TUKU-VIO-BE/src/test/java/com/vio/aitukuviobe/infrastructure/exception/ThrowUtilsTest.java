package com.vio.aitukuviobe.infrastructure.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ThrowUtils 单元测试")
class ThrowUtilsTest {

    @Test
    @DisplayName("条件为 true → 应抛出指定异常")
    void shouldThrowWhenConditionTrue() {
        assertThatThrownBy(() -> ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
    }

    @Test
    @DisplayName("条件为 false → 不应抛出异常")
    void shouldNotThrowWhenConditionFalse() {
        ThrowUtils.throwIf(false, ErrorCode.PARAMS_ERROR);
    }

    @Test
    @DisplayName("throwIf(condition, ErrorCode, message) → condition=true 抛异常带自定义消息")
    void shouldThrowWithCustomMessage() {
        assertThatThrownBy(() -> ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "自定义错误"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自定义错误");
    }

    @Test
    @DisplayName("throwIf(condition, RuntimeException) → condition=true 抛出指定异常")
    void shouldThrowGivenException() {
        RuntimeException ex = new RuntimeException("原始异常");
        assertThatThrownBy(() -> ThrowUtils.throwIf(true, ex))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("原始异常");
    }
}
