package com.hhk.picturebacked.api.Aliyunimage;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.hhk.picturebacked.exception.BusinessException;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.model.dto.ai.AiRenderTaskResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阿里云图像编辑 API（电商素材渲染）
 * <p>
 * 模型：wanx2.1-imageedit
 * 功能：description_edit（指令编辑），用于抠图换背景 / 白底图 / 场景图
 * 全部异步：创建任务拿 task_id，再轮询查询结果
 * </p>
 */
@Slf4j
@Component
public class AliYunImageEditApi {

    /**
     * 读取配置文件
     */
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    /**
     * 模型名
     */
    public static final String MODEL = "wanx2.1-imageedit";

    /**
     * 模拟模式开关
     * <p>
     * 开启后不真正调用百炼，直接把原图当作渲染结果返回。
     * 用于阿里云账号欠费/限流、或本地环境没有百炼额度时的联调自测：
     * MQ 消费、任务状态机、产物落库、审核卡点这些链路仍然被完整走通。
     * </p>
     */
    @Value("${aliYunAi.mock:false}")
    private boolean mockMode;

    /**
     * 创建任务地址
     */
    public static final String CREATE_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/image-synthesis";

    /**
     * 查询任务状态地址模板
     */
    public static final String GET_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 场景模板 -> 指令 prompt
     * 运营只选模板不写词，降低使用门槛，也避免生成内容跑偏
     */
    private static final Map<String, String> SCENE_TEMPLATES = new HashMap<>();

    static {
        // 场景图
        SCENE_TEMPLATES.put("NORDIC", "将商品置于北欧风格客厅场景中，浅色木质家具，自然柔和光线，简约北欧风背景，保持商品本身外观、颜色、比例完全不变");
        SCENE_TEMPLATES.put("OUTDOOR", "将商品置于户外自然环境中，阳光草地背景，真实自然光影，保持商品本身外观、颜色、比例完全不变");
        SCENE_TEMPLATES.put("CHRISTMAS", "将商品置于温馨圣诞节日场景中，暖色灯光与节日装饰点缀背景，保持商品本身外观、颜色、比例完全不变");
        // 白底
        SCENE_TEMPLATES.put("WHITE", "将商品抠出并放置于纯白色背景(RGB 255,255,255)之上，商品居中，保留自然阴影，无其他任何物体");
        // 透明底
        SCENE_TEMPLATES.put("TRANSPARENT", "将商品完整抠出，去除所有背景，输出透明背景 PNG，保留商品边缘细节与自然光影");
    }

    /**
     * 获取场景模板列表
     */
    public Map<String, String> listSceneTemplates() {
        return SCENE_TEMPLATES;
    }

    /**
     * 根据模板标识获取 prompt
     *
     * @param template 模板标识
     * @return prompt，找不到返回 null
     */
    public String getPrompt(String template) {
        return SCENE_TEMPLATES.get(template);
    }

    /**
     * 创建图像编辑任务
     *
     * @param baseImageUrl 原图 url
     * @param prompt        编辑指令
     * @return 任务响应，output.taskId 为任务 id
     */
    public AiRenderTaskResponse createEditTask(String baseImageUrl, String prompt) {
        if (StrUtil.isBlank(baseImageUrl) || StrUtil.isBlank(prompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "AI 渲染参数为空");
        }
        if (mockMode) {
            return mockCreateTask(baseImageUrl);
        }
        Map<String, Object> input = new HashMap<>();
        input.put("function", "description_edit");
        input.put("prompt", prompt);
        input.put("base_image_url", baseImageUrl);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("n", 1);
        parameters.put("watermark", false);

        Map<String, Object> body = new HashMap<>();
        body.put("model", MODEL);
        body.put("input", input);
        body.put("parameters", parameters);

        HttpRequest httpRequest = HttpRequest.post(CREATE_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                // 异步处理必须开启
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(body));
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("AI 渲染创建任务失败，body = {}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 渲染任务创建失败");
            }
            AiRenderTaskResponse response = JSONUtil.toBean(httpResponse.body(), AiRenderTaskResponse.class);
            if (response.getCode() != null && !response.getCode().isEmpty()) {
                log.error("AI 渲染接口异常，code = {}, message = {}", response.getCode(), response.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 渲染接口响应异常：" + response.getMessage());
            }
            return response;
        }
    }

    /**
     * 查询图像编辑任务状态
     *
     * @param taskId 任务 id
     * @return 任务响应
     */
    public AiRenderTaskResponse getEditTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        }
        if (mockMode) {
            return mockGetTask(taskId);
        }
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取 AI 任务状态失败");
            }
            return JSONUtil.toBean(httpResponse.body(), AiRenderTaskResponse.class);
        }
    }

    /**
     * 模拟模式：造一个直接成功的任务
     */
    private AiRenderTaskResponse mockCreateTask(String baseImageUrl) {
        AiRenderTaskResponse response = new AiRenderTaskResponse();
        AiRenderTaskResponse.Output output = new AiRenderTaskResponse.Output();
        output.setTaskId("mock-" + System.currentTimeMillis());
        output.setTaskStatus("PENDING");
        response.setOutput(output);
        // 把原图地址挂在 taskId 上，查询时原样返回，模拟「渲染后拿到新图」
        MOCK_SOURCE_URLS.put(output.getTaskId(), baseImageUrl);
        log.warn("AI 渲染处于模拟模式(aliYunAi.mock=true)，未真正调用百炼, mockTaskId = {}", output.getTaskId());
        return response;
    }

    /**
     * 模拟模式：直接返回成功结果
     */
    private AiRenderTaskResponse mockGetTask(String taskId) {
        AiRenderTaskResponse response = new AiRenderTaskResponse();
        AiRenderTaskResponse.Output output = new AiRenderTaskResponse.Output();
        output.setTaskId(taskId);
        output.setTaskStatus("SUCCEEDED");
        String url = MOCK_SOURCE_URLS.get(taskId);
        if (StrUtil.isNotBlank(url)) {
            AiRenderTaskResponse.Result result = new AiRenderTaskResponse.Result();
            result.setUrl(url);
            output.setResults(Collections.singletonList(result));
        }
        response.setOutput(output);
        return response;
    }

    /**
     * 模拟模式任务 -> 原图地址
     */
    private static final Map<String, String> MOCK_SOURCE_URLS = new ConcurrentHashMap<>();
}
