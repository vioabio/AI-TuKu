package com.vio.aitukuviobe.model.dto.picture;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询扩图任务响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetOutPaintingTaskResponse {

    private String requestId;
    private Output output;

    @Data
    public static class Output {
        private String taskId;
        private String taskStatus;
        private String submitTime;
        private String scheduledTime;
        private String endTime;

        /**
         * 输出图像的 URL
         */
        private String outputImageUrl;

        private String code;
        private String message;

        /**
         * 总任务数
         */
        private Integer total;

        /**
         * 成功任务数
         */
        private Integer succeeded;

        /**
         * 失败任务数
         */
        private Integer failed;
    }
}