package com.vio.aitukuviobe.infrastructure.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 扩图结果 — 替代原 CreateOutPaintingTaskResponse / GetOutPaintingTaskResponse
 *
 * <p>对应优化文档 3.4 节 / 20 章
 *
 * @author vivin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutPaintingResult {

    /** 任务 ID (用于后续轮询) */
    @JsonProperty("taskId")
    private String taskId;

    /** 任务状态: PENDING / RUNNING / SUCCEEDED / FAILED */
    @JsonProperty("taskStatus")
    private String taskStatus;

    /** 输出图片 URL (任务成功时) */
    @JsonProperty("outputImageUrl")
    private String outputImageUrl;

    /** 错误码 (任务失败时) */
    @JsonProperty("errorCode")
    private String errorCode;

    /** 错误信息 (任务失败时) */
    @JsonProperty("errorMessage")
    private String errorMessage;

    /** 提交时间 */
    @JsonProperty("submitTime")
    private String submitTime;

    /** 完成时间 */
    @JsonProperty("endTime")
    private String endTime;

    /** 是否已完成 (终态) */
    public boolean isTerminal() {
        return "SUCCEEDED".equals(taskStatus) || "FAILED".equals(taskStatus);
    }

    /** 是否成功 */
    public boolean isSucceeded() {
        return "SUCCEEDED".equals(taskStatus);
    }
}