package com.vio.aitukuviobe.interfaces.controller;

import com.vio.aitukuviobe.interfaces.dto.user.UserRegisterRequest;
import com.vio.aitukuviobe.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User API 契约测试")
class UserApiContractTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("POST /api/user/register 返回 JSON 契约正确")
    void registerShouldMatchContract() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setUserAccount("contract_test");
        req.setUserPassword("Test123456");
        req.setCheckPassword("Test123456");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/user/register", req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"code\"");
        assertThat(response.getBody()).contains("\"data\"");
    }

    @Test
    @DisplayName("GET /api/health 返回 UP 状态")
    void healthShouldReturnUp() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
}
