<template>
  <a-modal
    class="image-cropper-modal"
    v-model:visible="visible"
    title="编辑图片"
    :footer="false"
    @cancel="closeModal"
    width="800px"
  >
    <div class="image-cropper">
      <!-- 比例选择 -->
      <div class="aspect-ratio-selector">
        <span style="margin-right: 8px; font-weight: 500">裁剪比例：</span>
        <a-radio-group v-model:value="aspectRatio" button-style="solid" size="small">
          <a-radio-button value="free">自由比例</a-radio-button>
          <a-radio-button value="1:1">1:1</a-radio-button>
          <a-radio-button value="4:3">4:3</a-radio-button>
          <a-radio-button value="16:9">16:9</a-radio-button>
          <a-radio-button value="3:4">3:4</a-radio-button>
          <a-radio-button value="9:16">9:16</a-radio-button>
        </a-radio-group>
      </div>
      <div style="margin-bottom: 16px" />
      <vue-cropper
        ref="cropperRef"
        :img="cropperImageUrl"
        :autoCrop="true"
        :fixedBox="false"
        :centerBox="true"
        :canMoveBox="true"
        :info="true"
        outputType="png"
        :fixed="aspectRatio !== 'free'"
        :fixedNumber="currentAspectRatio"
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
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { uploadPictureUsingPost } from '@/api/pictureController'
import { fetchImageAsBlob } from '@/components/utils/index'

interface Props {
  imageUrl?: string
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

// 编辑器组件的引用
const cropperRef = ref()

// 弹窗可见性
const visible = ref(false)

// 加载状态
const loading = ref(false)

// 裁剪比例
const aspectRatio = ref('free')

// 用于 vue-cropper 的图片 URL（经过跨域处理）
const cropperImageUrl = ref<string>()

// 计算当前宽高比
const currentAspectRatio = computed(() => {
  if (aspectRatio.value === 'free') return [0, 0]
  const [width, height] = aspectRatio.value.split(':').map(Number)
  return [width, height]
})

// 打开弹窗
const openModal = () => {
  visible.value = true
  // 通过 fetchImageAsBlob 解决跨域问题
  fetchImageAsBlob(props.imageUrl, (blobUrl) => {
    cropperImageUrl.value = blobUrl
  })
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false
  cropperImageUrl.value = undefined
}

// 向左旋转
const rotateLeft = () => {
  cropperRef.value?.rotateLeft()
}

// 向右旋转
const rotateRight = () => {
  cropperRef.value?.rotateRight()
}

// 缩放
const changeScale = (num: number) => {
  cropperRef.value?.changeScale(num)
}

// 确认裁剪并上传
const handleConfirm = () => {
  cropperRef.value?.getCropBlob(async (blob: Blob) => {
    loading.value = true
    try {
      const params: any = props.picture ? { id: props.picture.id } : {}
      params.spaceId = props.spaceId
      // 将 Blob 转为 File 对象
      const file = new File([blob], 'cropped_image.png', { type: 'image/png' })
      const res = await uploadPictureUsingPost(params, {}, file)
      if (res.data.code === 0 && res.data.data) {
        message.success('图片编辑成功')
        // 将上传成功的图片信息传递给父组件
        props.onSuccess?.(res.data.data)
        closeModal()
      } else {
        message.error('图片上传失败，' + res.data.message)
      }
    } catch (error: any) {
      message.error('图片上传失败，' + (error?.message || ''))
    } finally {
      loading.value = false
    }
  })
}

// 暴露函数给父组件
defineExpose({
  openModal,
})
</script>

<style scoped>
.image-cropper {
  text-align: center;
}

.image-cropper :deep(.vue-cropper) {
  height: 400px;
}

.aspect-ratio-selector {
  margin-bottom: 12px;
}
</style>
