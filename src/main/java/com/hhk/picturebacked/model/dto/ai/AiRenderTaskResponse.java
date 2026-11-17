package com.hhk.picturebacked.model.dto.ai;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 阿里云图像编辑任务响应
 * <p>
 * 模型：wanx2.1-imageedit
 * 创建任务返回 task_id，查询任务返回 results[].url
 * </p>
 */
@Data
public class AiRenderTaskResponse implements Serializable {

    /**
     * 请求唯一标识
     */
    private String requestId;

    /**
     * 任务输出信息
     */
    private Output output;

    /**
     * 错误码，成功时不返回
     */
    private String code;

    /**
     * 错误信息，成功时不返回
     */
    private String message;

    @Data
    public static class Output implements Serializable {

        /**
         * 任务 id
         */
        private String taskId;

        /**
         * 任务状态：PENDING / RUNNING / SUCCEEDED / FAILED / CANCELED / UNKNOWN
         */
        private String taskStatus;

        /**
         * 提交时间
         */
        private String submitTime;

        /**
         * 结束时间
         */
        private String endTime;

        /**
         * 任务结果列表
         */
        private List<Result> results;

        /**
         * 任务指标
         */
        private TaskMetrics taskMetrics;

        /**
         * 取第一个成功结果的图片 url
         */
        public String firstImageUrl() {
            if (results == null || results.isEmpty()) {
                return null;
            }
            for (Result result : results) {
                if (result != null && result.getUrl() != null && !result.getUrl().isEmpty()) {
                    return result.getUrl();
                }
            }
            return null;
        }

        /**
         * 取第一个失败的错误信息
         */
        public String firstErrorMessage() {
            if (results == null) {
                return null;
            }
            for (Result result : results) {
                if (result != null && result.getMessage() != null && !result.getMessage().isEmpty()) {
                    return result.getMessage();
                }
            }
            return null;
        }
    }

    @Data
    public static class Result implements Serializable {
        /**
         * 生成的图片 url，有效期 24 小时
         */
        private String url;

        /**
         * 单张失败时的错误码
         */
        private String code;

        /**
         * 单张失败时的错误信息
         */
        private String message;
    }

    @Data
    public static class TaskMetrics implements Serializable {
        /**
         * 总任务数
         */
        private Integer total;

        /**
         * 成功数
         */
        private Integer succeeded;

        /**
         * 失败数
         */
        private Integer failed;
    }

    private static final long serialVersionUID = 1L;
}
