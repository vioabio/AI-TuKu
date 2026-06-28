package com.vio.aitukuviobe.model.dto.picture;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建扩图任务响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOutPaintingTaskResponse {

    private Output output;

    @Data
    public static class Output {
        /**
         * 任务 ID
         */
        private String taskId;

        /**
         * 任务状态
         * PENDING/ RUNNING/ SUSPENDED/ SUCCEEDED/ FAILED/ UNKNOWN
         */
        private String taskStatus;
    }

    private String code;
    private String message;
    private String requestId;
}
