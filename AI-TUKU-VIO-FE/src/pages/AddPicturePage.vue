<template>
  <div id="addPicturePage">
    <h2 style="margin-bottom: 16px">
      {{ isEditMode ? '修改图片' : '创建图片' }}
    </h2>

    <!-- 编辑模式：数据加载中 -->
    <a-spin v-if="isEditMode && loadingOldData" style="width: 100%; padding: 80px 0" />

    <template v-if="!isEditMode || !loadingOldData">
      <!-- 选择上传方式 -->
      <a-tabs v-model:activeKey="uploadType">
        <a-tab-pane key="file" tab="文件上传">
          <PictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
        </a-tab-pane>
        <a-tab-pane key="url" tab="URL 上传" force-render>
          <UrlPictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
        </a-tab-pane>
      </a-tabs>

      <!-- 图片编辑工具栏（上传/加载图片后显示） -->
      <div v-if="picture" class="edit-bar">
        <a-space size="middle">
          <a-button :icon="h(EditOutlined)" @click="doEditPicture">编辑图片</a-button>
          <a-button type="primary" ghost :icon="h(FullscreenOutlined)" @click="doImagePainting">
            AI 扩图
          </a-button>
        </a-space>
      </div>

      <!-- 图片编辑弹窗（裁剪/旋转/缩放 + WebSocket 协同编辑） -->
      <ImageCropper
        ref="imageCropperRef"
        :imageUrl="picture?.url"
        :picture="picture"
        :spaceId="spaceId"
        :space="space"
        :onSuccess="onCropSuccess"
      />

      <!-- AI 扩图弹窗 -->
      <ImageOutPainting
        ref="imageOutPaintingRef"
        :picture="picture"
        :spaceId="spaceId"
        :onSuccess="onImageOutPaintingSuccess"
      />

      <!-- 图片信息表单 -->
      <a-form
        v-if="picture"
        name="pictureForm"
        layout="vertical"
        :model="pictureForm"
        @finish="handleSubmit"
      >
        <a-form-item name="name" label="名称">
          <a-input v-model:value="pictureForm.name" placeholder="请输入名称" allow-clear />
        </a-form-item>
        <a-form-item name="introduction" label="简介">
          <a-textarea
            v-model:value="pictureForm.introduction"
            placeholder="请输入简介"
            :auto-size="{ minRows: 2, maxRows: 5 }"
            allow-clear
          />
        </a-form-item>
        <a-form-item name="category" label="分类">
          <a-auto-complete
            v-model:value="pictureForm.category"
            placeholder="请输入分类"
            :options="categoryOptions"
            allow-clear
          />
        </a-form-item>
        <a-form-item name="tags" label="标签">
          <a-select
            v-model:value="pictureForm.tags"
            mode="tags"
            placeholder="请输入标签"
            :options="tagOptions"
            allow-clear
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" style="width: 100%" :loading="submitting">
            {{ isEditMode ? '保存修改' : '创建' }}
          </a-button>
        </a-form-item>
      </a-form>
    </template>
  </div>
</template>

<script setup lang="ts">
import PictureUpload from '@/components/PictureUpload.vue'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import ImageCropper from '@/components/ImageCropper.vue'
import ImageOutPainting from '@/components/ImageOutPainting.vue'
import { computed, h, onMounted, reactive, ref, watchEffect } from 'vue'
import { message } from 'ant-design-vue'
import { EditOutlined, FullscreenOutlined } from '@ant-design/icons-vue'
import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()

const picture = ref<API.PictureVO>()
const pictureForm = reactive<API.PictureEditRequest>({})
const uploadType = ref<'file' | 'url'>('file')
const isEditMode = computed(() => !!route.query?.id)
const loadingOldData = ref(false)
const submitting = ref(false)

// 空间信息（团队空间协同编辑需要）
const space = ref<API.SpaceVO>()
const spaceId = computed<string | undefined>(() => {
  const val = route.query?.spaceId
  return val != null ? String(val) : undefined
})

// 组件引用
const imageCropperRef = ref()
const imageOutPaintingRef = ref()

// 获取空间信息
const fetchSpace = async () => {
  if (spaceId.value) {
    const res = await getSpaceVoByIdUsingGet({ id: spaceId.value })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    }
  }
}
watchEffect(() => { fetchSpace() })

// ---- 图片编辑（裁剪/旋转/缩放） ----
const doEditPicture = () => {
  imageCropperRef.value?.openModal()
}
const onCropSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// ---- AI 扩图 ----
const doImagePainting = () => {
  imageOutPaintingRef.value?.openModal()
}
const onImageOutPaintingSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// ---- 上传成功回调 ----
const onSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
  pictureForm.name = newPicture.name
}

// ---- 提交表单（创建 or 修改） ----
const handleSubmit = async (values: any) => {
  const pictureId = picture.value?.id
  if (!pictureId) {
    message.warning('请先上传图片')
    return
  }
  submitting.value = true
  try {
    const res = await editPictureUsingPost({ id: pictureId, ...values } as any)
    if (res.data.code === 0 && res.data.data) {
      message.success(isEditMode.value ? '修改成功' : '创建成功')
      router.push({ path: `/picture/${pictureId}` })
    } else {
      message.error((isEditMode.value ? '修改' : '创建') + '失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('请求失败：' + (e?.message || '网络错误'))
  } finally {
    submitting.value = false
  }
}

// ---- 标签/分类选项 ----
interface OptionItem { value: string; label: string }
const categoryOptions = ref<OptionItem[]>([])
const tagOptions = ref<OptionItem[]>([])

const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((d: string) => ({ value: d, label: d }))
    categoryOptions.value = (res.data.data.categoryList ?? []).map((d: string) => ({ value: d, label: d }))
  }
}

// ---- 编辑模式：加载旧数据 ----
const getOldPicture = async () => {
  const id = route.query?.id
  if (!id) return
  loadingOldData.value = true
  try {
    const res = await getPictureVoByIdUsingGet({ id: id as string })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      pictureForm.name = data.name
      pictureForm.introduction = data.introduction
      pictureForm.category = data.category
      pictureForm.tags = data.tags
    } else {
      message.error('获取图片信息失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片信息失败：' + (e?.message || '网络错误'))
  } finally {
    loadingOldData.value = false
  }
}

onMounted(() => {
  getTagCategoryOptions()
  getOldPicture()
})
</script>

<style scoped>
#addPicturePage {
  max-width: 720px;
  margin: 0 auto;
}

#addPicturePage .edit-bar {
  text-align: center;
  margin: 16px 0;
}
</style>
