package com.vio.aitukuviobe.infrastructure.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.vio.aitukuviobe.infrastructure.exception.BusinessException;
import com.vio.aitukuviobe.infrastructure.exception.ErrorCode;
import com.vio.aitukuviobe.infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.vio.aitukuviobe.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.vio.aitukuviobe.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 阿里云百炼 AI API 调用类
 */
@Slf4j
@Component
public class AliYunAiApi {

    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL =
            "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建扩图任务
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(
            CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图参数为空");
        }
        String requestBody = JSONUtil.toJsonStr(createOutPaintingTaskRequest);
        log.info("创建 AI 扩图任务，请求体：{}", requestBody);
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                // 必须开启异步处理，设置为 enable
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(requestBody);
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：HTTP状态码={}, 响应={}", httpResponse.getStatus(), httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            String responseBody = httpResponse.body();
            log.info("创建任务响应：{}", responseBody);
            CreateOutPaintingTaskResponse response =
                    JSONUtil.toBean(responseBody, CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.error("AI 扩图失败，errorCode:{}, errorMessage:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口响应异常");
            }
            return response;
        }
    }

    /**
     * 查询创建的任务状态
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 id 不能为空");
        }
        try (HttpResponse httpResponse = HttpRequest.get(
                        String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                log.error("查询任务失败，HTTP状态码：{}，响应：{}", httpResponse.getStatus(), httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            String responseBody = httpResponse.body();
            log.info("查询任务响应：taskId={}, body={}", taskId, responseBody);
            GetOutPaintingTaskResponse response =
                    JSONUtil.toBean(responseBody, GetOutPaintingTaskResponse.class);
            // 记录任务失败的原因
            if (response.getOutput() != null) {
                String taskStatus = response.getOutput().getTaskStatus();
                if ("FAILED".equals(taskStatus)) {
                    log.error("AI 扩图任务失败！taskId={}, output.code={}, output.message={}",
                            taskId,
                            response.getOutput().getCode(),
                            response.getOutput().getMessage());
                }
            }
            return response;
        }
    }
}