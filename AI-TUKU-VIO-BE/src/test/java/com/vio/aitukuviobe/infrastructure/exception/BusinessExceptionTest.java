package com.vio.aitukuviobe.infrastructure.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessException 单元测试")
class BusinessExceptionTest {

    @Test
    @DisplayName("构造 (int code, String message)")
    void shouldCreateWithCodeAndMessage() {
        BusinessException ex = new BusinessException(40000, "参数错误");
        assertThat(ex.getCode()).isEqualTo(40000);
        assertThat(ex.getMessage()).isEqualTo("参数错误");
    }

    @Test
    @DisplayName("构造 (ErrorCode errorCode)")
    void shouldCreateWithErrorCode() {
        BusinessException ex = new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        assertThat(ex.getCode()).isEqualTo(ErrorCode.NOT_FOUND_ERROR.getCode());
        assertThat(ex.getMessage()).isEqualTo(ErrorCode.NOT_FOUND_ERROR.getMessage());
    }

    @Test
    @DisplayName("构造 (ErrorCode errorCode, String customMessage)")
    void shouldCreateWithErrorCodeAndCustomMessage() {
        BusinessException ex = new BusinessException(ErrorCode.PARAMS_ERROR, "自定义参数错误");
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        assertThat(ex.getMessage()).isEqualTo("自定义参数错误");
    }
}
