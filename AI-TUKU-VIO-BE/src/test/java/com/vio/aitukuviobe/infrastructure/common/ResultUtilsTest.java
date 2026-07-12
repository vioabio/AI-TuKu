package com.vio.aitukuviobe.infrastructure.common;

import com.vio.aitukuviobe.infrastructure.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResultUtils 单元测试")
class ResultUtilsTest {

    @Test
    @DisplayName("success → code=0, message='ok'")
    void shouldReturnSuccess() {
        BaseResponse<String> response = ResultUtils.success("hello");
        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getMessage()).isEqualTo("ok");
    }

    @Test
    @DisplayName("success(null) → code=0, data=null")
    void shouldReturnSuccessWithNull() {
        BaseResponse<Object> response = ResultUtils.success(null);
        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("error(ErrorCode) → 正确 code 和 message")
    void shouldReturnErrorFromErrorCode() {
        BaseResponse<?> response = ResultUtils.error(ErrorCode.PARAMS_ERROR);
        assertThat(response.getCode()).isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        assertThat(response.getMessage()).isEqualTo(ErrorCode.PARAMS_ERROR.getMessage());
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("error(code, message) → 正确 code 和 message")
    void shouldReturnErrorWithCodeAndMessage() {
        BaseResponse<?> response = ResultUtils.error(40000, "自定义错误");
        assertThat(response.getCode()).isEqualTo(40000);
        assertThat(response.getMessage()).isEqualTo("自定义错误");
    }

    @Test
    @DisplayName("所有 SUCCESS 码应一致为 0")
    void successCodeShouldAlwaysBeZero() {
        assertThat(ErrorCode.SUCCESS.getCode()).isEqualTo(0);
        assertThat(ErrorCode.SUCCESS.getMessage()).isEqualTo("ok");
    }
}
