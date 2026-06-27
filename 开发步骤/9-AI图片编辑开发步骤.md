# 9 - AI 图片编辑

## 本节重点

为进一步提升用户使用私有空间的体验，本节将重点扩展图片编辑功能，包括：

- 基础图片编辑（裁剪、旋转、缩放）
- AI 图片编辑（扩图）

通过这些功能扩展，用户可以在平台上轻松完成从基础编辑到高级处理的多样化操作，而不需要使用其他 PS 软件。

---

## 一、基础图片编辑

### 需求分析

在日常的图片管理中，用户经常需要对图片进行简单处理，比如裁剪多余部分、旋转图片、放大缩小尺寸等。引入基础图片编辑功能，帮助用户快速完成以下操作：

- **裁剪**：支持按固定比例或自由裁剪
- **旋转**：提供顺时针、逆时针旋转功能

非常适合上传证件照之类的场景。该功能不需要限制仅在空间内才能使用，公共图库也可以支持。

### 方案设计

图片编辑功能的实现以前端为主，编辑完成后通过调用现有的图片上传接口，将编辑后的图片保存至平台。具体业务流程：

1. 在图片上传页面，如果用户已上传图片，页面会展示"编辑图片"按钮
2. 用户点击"编辑图片"后，将打开图片编辑的弹窗组件，支持裁剪、旋转等操作
3. 用户确认编辑后，会调用图片上传接口，将编辑后的新图片保存至平台，同时更新图片信息

### 前端开发

#### 1、图片编辑组件

选用开源的 **vue-cropper** 组件（Vue3 版本）。

**安装依赖：**

```bash
npm install vue-cropper@next
```

在 `main.ts` 中引入依赖：

```typescript
import VueCropper from 'vue-cropper';
import 'vue-cropper/dist/index.css'

app.use(VueCropper)
```

**新建图片编辑组件 ImageCropper**，参考官方 Demo 实现：

```vue
<template>
  <div class="image-cropper">
    <vue-cropper
      ref="cropperRef"
      :img="imageUrl"
      :autoCrop="true"
      :fixedBox="false"
      :centerBox="true"
      :canMoveBox="true"
      :info="true"
      outputType="png"
    />
    <div style="margin-bottom: 16px" />
    <!-- 图片操作 -->
    <div class="image-cropper-actions">
      <a-space>
        <a-button @click="rotateLeft">向左旋转</a-button>
        <a-button @click="rotateRight">向右旋转</a-button>
        <a-button @click="changeScale(1)">放大</a-button>
        <a-button @click="changeScale(-1)">缩小</a-button>
        <a-button type="primary" :loading="loading" @click="handleConfirm">确认</a-button>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  imageUrl?: string
}

const props = defineProps<Props>()

// 编辑器组件的引用
const cropperRef = ref()

// 向左旋转
const rotateLeft = () => {
  cropperRef.value.rotateLeft()
}

// 向右旋转
const rotateRight = () => {
  cropperRef.value.rotateRight()
}

// 缩放
const changeScale = (num: number) => {
  cropperRef.value.changeScale(num)
}

// 确认裁剪
const handleConfirm = () => {
  cropperRef.value.getCropBlob((blob: Blob) => {
    // blob 为已裁切的文件
  })
}
</script>

<style scoped>
.image-cropper {
  text-align: center;
}
  
.image-cropper .vue-cropper {
  height: 400px;
}
</style>
```

#### 2、图片编辑弹窗

将图片编辑组件放入弹窗中，并增加上传逻辑。当用户点击确认后，将裁剪后的图片上传：

**弹窗模板：**

```vue
<a-modal class="image-cropper" v-model:visible="visible" title="编辑图片" :footer="false" @cancel="closeModal">
  <!-- 图片编辑组件 -->
</a-modal>
```

**上传逻辑：**

```typescript
/**
 * 上传
 * @param file
 */
const handleUpload = async ({ file }: any) => {
  loading.value = true
  try {
    const params: API.PictureUploadRequest = props.picture ? { id: props.picture.id } : {}
    params.spaceId = props.spaceId
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
      closeModal();
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error) {
    message.error('图片上传失败')
  } finally {
    loading.value = false
  }
}
```

#### 3、使用图片编辑弹窗组件

在创建图片页面使用组件，在图片下方补充一个编辑按钮：

```vue
<div v-if="picture" class="edit-bar">
  <a-button :icon="h(EditOutlined)" @click="doEditPicture">编辑图片</a-button>
  <ImageCropper
    ref="imageCropperRef"
    :imageUrl="picture?.url"
    :picture="picture"
    :spaceId="spaceId"
    :onSuccess="onCropSuccess"
  />
</div>
```

编辑图片事件函数：

```typescript
// 图片编辑弹窗引用
const imageCropperRef = ref()

// 编辑图片
const doEditPicture = () => {
  if (imageCropperRef.value) {
    imageCropperRef.value.openModal()
  }
}

// 编辑成功事件
const onCropSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}
```

CSS 样式优化：

```css
#addPicturePage .edit-bar {
  text-align: center;
  margin: 16px 0;
}
```

#### 4、图片跨域问题解决

由于 COS 对象存储的图片域名和前端域名不同，需要解决跨域问题。

**方案：** 写一个工具函数 `fetchImageAsBlob`，将图片 url 的前缀替换为当前页面的地址（通过 Vite 代理），先 fetch 获取图片的 blob 对象，再将 blob 转化为 base64：

```typescript
/**
 * 获取图片 blob 对象和 base64
 * @param url 图片 url
 * @param cb 回调函数,返回 blob url 和 base64
 */
export const fetchImageAsBlob = async (
  url?: string,
  cb?: (blobUrl: string, base64: string) => void,
) => {
  if (!url) return
  const formatUrl = url.replace('https://pic.code-nav.cn', window.location.origin)
  try {
    const response = await fetch(formatUrl)
    if (!response.ok) {
      throw new Error('图片加载失败')
    }
    const imageBlob = await response.blob()
    const objectUrl = URL.createObjectURL(imageBlob)

    // 转换为 base64
    const reader = new FileReader()
    reader.readAsDataURL(imageBlob)
    reader.onloadend = () => {
      const base64 = reader.result as string
      cb?.(objectUrl, base64)
    }
  } catch (error: any) {
    console.log(error)
  }
}
```

Vite 代理配置：

```typescript
server: {
  host: 'localhost',
  // 代理
  proxy: {
    // 改为你的图片存储 url 前缀
    '/yu_picture': {
      // 改为你的对象存储域名
      target: 'https://codefather.cn',
      changeOrigin: true,
    }
  },
},
```

### 扩展

1. **优化业务流程：** 在图片上传前，先触发编辑弹窗，完成图片裁剪后再上传到后端。
2. **支持调整裁剪区域的固定比例**（比如 16:9），利用 vue-cropper 组件的 `fixedNumber` 属性：

```vue
<!-- 比例选择 -->
<div class="aspect-ratio-selector">
  <a-radio-group v-model:value="aspectRatio" button-style="solid">
    <a-radio-button value="free">自由比例</a-radio-button>
    <a-radio-button value="1:1">1:1</a-radio-button>
    <a-radio-button value="4:3">4:3</a-radio-button>
    <a-radio-button value="16:9">16:9</a-radio-button>
    <a-radio-button value="3:4">3:4</a-radio-button>
    <a-radio-button value="9:16">9:16</a-radio-button>
  </a-radio-group>
</div>

<vue-cropper
  ref="cropperRef"
  :img="imageUrl"
  :autoCrop="true"
  :fixedBox="false"
  :centerBox="true"
  :canMoveBox="true"
  :info="true"
  outputType="png"
  :fixed="aspectRatio !== 'free'"
  :fixedNumber="currentAspectRatio"
/>

const aspectRatio = ref('free')

// 计算当前宽高比
const currentAspectRatio = computed(() => {
  if (aspectRatio.value === 'free') return [0, 0]
  const [width, height] = aspectRatio.value.split(':').map(Number)
  return [width, height]
})
```

3. 支持图片的任意角度旋转操作
4. 支持对图片尺寸进行等比例放大的操作

---

## 二、AI 图片编辑

### 需求分析

利用 AI 绘图大模型来编辑图片，实现**扩图**、擦除补全、图配文、去水印等功能。

### 方案设计

#### 1、AI 绘图大模型选择

选用**阿里云百炼**平台的通义万象图像编辑模型，因为：

- 阿里云系公司，国内网络访问稳定
- 有完善的文档和在线调试能力
- 开箱即用，适合企业的独立开发者

#### 2、调用方式

阿里云百炼 API 采用**异步**的方式调用，即：

1. 先创建任务 → 返回 taskId
2. 再通过 taskId 轮询查询任务状态，直到任务完成 → 返回结果图片 URL

由于接口是异步的，前端需要轮询查询任务状态。**推荐前端轮询**方案：

- 创建任务后，前端每隔 2~3 秒轮询查询任务状态
- 任务成功：获取结果图片 URL 并展示
- 任务失败：提示用户，停止轮询
- 退出页面：清理定时器，避免内存泄漏

### 后端开发

#### 1、AI 扩图 API

在 `application.yml` 中添加阿里云 AI 配置：

```yaml
# 阿里云 AI 配置
aliYunAi:
  apiKey: your-api-key
```

**创建扩图任务请求类**（`model.dto.picture` 包）：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOutPaintingTaskRequest {
    /**
     * 模型
     */
    private String model = "image-out-painting";

    /**
     * 输入信息
     */
    private Input input;

    /**
     * 图像处理参数
     */
    private Parameters parameters;

    @Data
    public static class Input {
        /**
         * 图片 URL
         */
        private String imageUrl;
    }

    @Data
    public static class Parameters {
        /**
         * 可选，图像居中，在水平方向上按比例扩展，默认值 1.0，范围 [1.0, 3.0]
         */
        @Alias("x_scale")
        @JsonProperty("xScale")
        private Float xScale;

        /**
         * 可选，图像居中，在垂直方向上按比例扩展，默认值 1.0，范围 [1.0, 3.0]
         */
        @Alias("y_scale")
        @JsonProperty("yScale")
        private Float yScale;

        /**
         * 可选，添加 "Generated by AI" 水印，默认值 true
         */
        @Alias("add_watermark")
        private Boolean addWatermark = false;
    }
}
```

> ⚠️ 注意：SpringMVC 对第二个字母是大写的参数无法映射（如 `xScale`），需要用 `@JsonProperty("xScale")` 注解解决。

**创建扩图任务响应类：**

```java
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
```

**查询任务响应类：**

```java
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
```

**API 调用类**（通过 Hutool HTTP 工具调用阿里云百炼 API）：

```java
@Slf4j
@Component
public class AliYunAiApi {
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建任务
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图参数为空");
        }
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                // 必须开启异步处理，设置为enable
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
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
     * 查询创建的任务
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 id 不能为空");
        }
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }
}
```

#### 2、扩图服务

在 PictureService 中编写创建扩图任务方法：

```java
@Override
public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
    // 获取图片信息
    Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
    Picture picture = Optional.ofNullable(this.getById(pictureId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
    // 权限校验
    checkPictureAuth(loginUser, picture);
    // 构造请求参数
    CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
    CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
    input.setImageUrl(picture.getUrl());
    taskRequest.setInput(input);
    BeanUtil.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);
    // 创建任务
    return aliYunAiApi.createOutPaintingTask(taskRequest);
}
```

#### 3、扩图接口

在 PictureController 添加 AI 扩图接口：

```java
/**
 * 创建 AI 扩图任务
 */
@PostMapping("/out_painting/create_task")
public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
        @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
        HttpServletRequest request) {
    if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    User loginUser = userService.getLoginUser(request);
    CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
    return ResultUtils.success(response);
}

/**
 * 查询 AI 扩图任务
 */
@GetMapping("/out_painting/get_task")
public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
    ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
    GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
    return ResultUtils.success(task);
}
```

### 前端开发

#### 1、AI 扩图弹窗

新建 `ImageOutPainting` 组件，包含原始图片展示、扩图结果展示、操作按钮：

```vue
<template>
  <a-modal
    class="image-out-painting"
    v-model:visible="visible"
    title="AI 扩图"
    :footer="false"
    @cancel="closeModal"
  >
    <a-row gutter="16">
      <a-col span="12">
        <h4>原始图片</h4>
        <img :src="picture?.url" :alt="picture?.name" style="max-width: 100%" />
      </a-col>
      <a-col span="12">
        <h4>扩图结果</h4>
        <img
          v-if="resultImageUrl"
          :src="resultImageUrl"
          :alt="picture?.name"
          style="max-width: 100%"
        />
      </a-col>
    </a-row>
    <div style="margin-bottom: 16px" />
    <a-flex gap="16" justify="center">
      <a-button type="primary" :loading="!!taskId" ghost @click="createTask">生成图片</a-button>
      <a-button type="primary" v-if="resultImageUrl" :loading="uploadLoading" @click="handleUpload">应用结果</a-button>
    </a-flex>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { createPictureOutPaintingTaskUsingPost, getPictureOutPaintingTaskUsingGet, uploadPictureByUrlUsingPost } from '@/api/pictureController'

interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

// 是否可见
const visible = ref(false)

// 扩图结果
const resultImageUrl = ref<string>()

// 任务 id
let taskId = ref<string>()

// 上传 loading
const uploadLoading = ref<boolean>(false)

// 打开弹窗
const openModal = () => {
  visible.value = true
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false
}

/**
 * 创建任务
 */
const createTask = async () => {
  if (!props.picture?.id) {
    return
  }
  const res = await createPictureOutPaintingTaskUsingPost({
    pictureId: props.picture.id,
    parameters: {
      xScale: 2,
      yScale: 2,
    },
  })
  if (res.data.code === 0 && res.data.data) {
    message.success('创建任务成功，请耐心等待，不要退出界面')
    console.log(res.data.data.output.taskId)
    taskId.value = res.data.data.output.taskId
    // 开启轮询
    startPolling()
  } else {
    message.error('创建任务失败，' + res.data.message)
  }
}

// 轮询定时器
let pollingTimer: NodeJS.Timeout = null

// 清理轮询定时器
const clearPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
    taskId.value = null
  }
}

// 开始轮询
const startPolling = () => {
  if (!taskId.value) return

  pollingTimer = setInterval(async () => {
    try {
      const res = await getPictureOutPaintingTaskUsingGet({
        taskId: taskId.value,
      })
      if (res.data.code === 0 && res.data.data) {
        const taskResult = res.data.data.output
        if (taskResult.taskStatus === 'SUCCEEDED') {
          message.success('扩图任务成功')
          resultImageUrl.value = taskResult.outputImageUrl
          clearPolling()
        } else if (taskResult.taskStatus === 'FAILED') {
          message.error('扩图任务失败')
          clearPolling()
        }
      }
    } catch (error) {
      console.error('轮询任务状态失败', error)
      message.error('检测任务状态失败，请稍后重试')
      clearPolling()
    }
  }, 3000) // 每隔 3 秒轮询一次
}

// 应用结果（上传图片）
const handleUpload = async () => {
  uploadLoading.value = true
  try {
    const params: API.PictureUploadRequest = {
      fileUrl: resultImageUrl.value,
      spaceId: props.spaceId,
    }
    if (props.picture) {
      params.id = props.picture.id
    }
    const res = await uploadPictureByUrlUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      props.onSuccess?.(res.data.data)
      closeModal()
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error) {
    message.error('图片上传失败')
  } finally {
    uploadLoading.value = false
  }
}

// 清理定时器，避免内存泄漏
onUnmounted(() => {
  clearPolling()
})

// 暴露函数给父组件
defineExpose({
  openModal,
})
</script>

<style scoped>
.image-out-painting {
  text-align: center;
}
</style>
```

#### 2、创建图片页面引入弹窗

在创建图片页面添加 AI 扩图按钮：

```vue
<a-space size="middle">
  <a-button :icon="h(EditOutlined)" @click="doEditPicture">编辑图片</a-button>
  <a-button type="primary" ghost :icon="h(FullscreenOutlined)" @click="doImagePainting">
    AI 扩图
  </a-button>
</a-space>
<ImageOutPainting
  ref="imageOutPaintingRef"
  :picture="picture"
  :spaceId="spaceId"
  :onSuccess="onImageOutPaintingSuccess"
/>
```

对应的事件函数：

```typescript
// AI 扩图弹窗引用
const imageOutPaintingRef = ref()

// AI 扩图
const doImagePainting = () => {
  if (imageOutPaintingRef.value) {
    imageOutPaintingRef.value.openModal()
  }
}

// 编辑成功事件
const onImageOutPaintingSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}
```

---

## 扩展知识 - 异步任务优化

AI 扩图的接口调用方式为**异步**，常用两种轮询方式：

**1）前端轮询（推荐）：**

前端创建任务后，使用 `setInterval` 定时查询，任务成功/失败后清理定时器。优点：实现简单，前端可控；缺点：产生额外HTTP请求。

**2）后端轮询：**

后端阻塞等待任务完成再返回：

```java
@RestController
public class TaskController {
    @PostMapping("/createTask")
    public String createTask() {
        String taskId = taskService.submitTask();
        return taskId;
    }

    @GetMapping("/waitForTask")
    public ResponseEntity<String> waitForTask(@RequestParam String taskId) {
        while (true) {
            String status = taskService.checkTaskStatus(taskId);
            if ("success".equals(status)) {
                return ResponseEntity.ok("Task completed");
            } else if ("failed".equals(status)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Task failed");
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred");
            }
        }
    }
}
```

后端轮询容易因为任务阻塞导致资源耗尽，通常推荐**前端轮询**方案。

---

## 扩展

1. 利用通义万象实现更多 AI 编辑功能：擦除补全、图配文、去水印等
2. 考虑异步任务的通知方案（WebSocket 推送等），避免前端持续轮询
3. 控制用户 AI 扩图次数，防止 API 费用过高
