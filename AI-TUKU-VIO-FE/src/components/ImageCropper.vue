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
      <!-- 协同编辑状态 -->
      <div v-if="isTeamSpace" class="image-edit-actions" style="margin-bottom: 12px">
        <a-space>
          <a-tag v-if="editingUser" color="orange">{{ editingUser.userName }} 正在编辑</a-tag>
          <a-button v-if="canEnterEdit" type="primary" ghost @click="enterEdit">进入编辑</a-button>
          <a-button v-if="canExitEdit" danger ghost @click="exitEdit">退出编辑</a-button>
        </a-space>
      </div>
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
      <div class="image-cropper-actions">
        <a-space>
          <a-button @click="rotateLeft" :disabled="!canEdit">向左旋转</a-button>
          <a-button @click="rotateRight" :disabled="!canEdit">向右旋转</a-button>
          <a-button @click="changeScale(1)" :disabled="!canEdit">放大</a-button>
          <a-button @click="changeScale(-1)" :disabled="!canEdit">缩小</a-button>
          <a-button type="primary" :loading="loading" :disabled="!canEdit" @click="handleConfirm">确认</a-button>
        </a-space>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect, onUnmounted, type Ref } from 'vue'
import { message } from 'ant-design-vue'
import { uploadPictureUsingPost } from '@/api/pictureController'
import { fetchImageAsBlob } from '@/components/utils/index'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { PictureEditWebSocket } from '@/utils/PictureEditWebSocket'
import {
  PICTURE_EDIT_MESSAGE_TYPE_ENUM,
  PICTURE_EDIT_ACTION_ENUM,
} from '@/constants/picture'
import { SPACE_TYPE_ENUM } from '@/constants/space'

interface Props {
  imageUrl?: string
  picture?: API.PictureVO
  spaceId?: number
  space?: API.SpaceVO
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()
const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser

const cropperRef = ref()
const visible = ref(false)
const loading = ref(false)
const aspectRatio = ref('free')
const cropperImageUrl = ref<string>()

// 协同编辑状态
const editingUser = ref<API.UserVO>()
const websocket: Ref<PictureEditWebSocket | null> = ref(null)

// 是否为团队空间
const isTeamSpace = computed(() => {
  return props.space?.spaceType === SPACE_TYPE_ENUM.TEAM
})

// 没有用户正在编辑中，可进入编辑
const canEnterEdit = computed(() => {
  return !editingUser.value
})

// 正在编辑的用户是本人，可退出编辑
const canExitEdit = computed(() => {
  return editingUser.value?.id === loginUser?.id
})

// 可以编辑：非团队空间默认可编辑，团队空间需是当前编辑者
const canEdit = computed(() => {
  if (!isTeamSpace.value) return true
  return editingUser.value?.id === loginUser?.id
})

const currentAspectRatio = computed(() => {
  if (aspectRatio.value === 'free') return [0, 0]
  const [width, height] = aspectRatio.value.split(':').map(Number)
  return [width, height]
})

// WebSocket 消息发送
const sendWsMessage = (msg: Record<string, any>) => {
  websocket.value?.sendMessage(msg)
}

const enterEdit = () => {
  sendWsMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT })
}

const exitEdit = () => {
  sendWsMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT })
}

const editAction = (action: string) => {
  sendWsMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION, editAction: action })
}

// 初始化 WebSocket
const initWebsocket = () => {
  if (!props.picture?.id) return
  if (websocket.value) {
    websocket.value.disconnect()
    editingUser.value = undefined
  }
  websocket.value = new PictureEditWebSocket(props.picture.id)
  websocket.value.connect()

  websocket.value.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.INFO, (msg) => {
    message.info(msg.message)
  })
  websocket.value.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ERROR, (msg) => {
    message.error(msg.message)
  })
  websocket.value.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT, (msg) => {
    message.info(msg.message)
    editingUser.value = msg.user
  })
  websocket.value.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION, (msg) => {
    message.info(msg.message)
    switch (msg.editAction) {
      case PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT:
        cropperRef.value?.rotateLeft(); break
      case PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT:
        cropperRef.value?.rotateRight(); break
      case PICTURE_EDIT_ACTION_ENUM.ZOOM_IN:
        cropperRef.value?.changeScale(1); break
      case PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT:
        cropperRef.value?.changeScale(-1); break
    }
  })
  websocket.value.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT, (msg) => {
    message.info(msg.message)
    editingUser.value = undefined
  })
}

// 团队空间才初始化 WebSocket
watchEffect(() => {
  if (isTeamSpace.value) initWebsocket()
})

// 打开弹窗
const openModal = () => {
  visible.value = true
  fetchImageAsBlob(props.imageUrl, (blobUrl) => {
    cropperImageUrl.value = blobUrl
  })
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false
  cropperImageUrl.value = undefined
  if (websocket.value) {
    websocket.value.disconnect()
    websocket.value = null
  }
  editingUser.value = undefined
}

// 向左旋转
const rotateLeft = () => {
  cropperRef.value?.rotateLeft()
  if (isTeamSpace.value) editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT)
}

// 向右旋转
const rotateRight = () => {
  cropperRef.value?.rotateRight()
  if (isTeamSpace.value) editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT)
}

// 缩放
const changeScale = (num: number) => {
  cropperRef.value?.changeScale(num)
  if (isTeamSpace.value) {
    editAction(num > 0 ? PICTURE_EDIT_ACTION_ENUM.ZOOM_IN : PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT)
  }
}

// 确认裁剪并上传
const handleConfirm = () => {
  cropperRef.value?.getCropBlob(async (blob: Blob) => {
    loading.value = true
    try {
      const params: any = props.picture ? { id: props.picture.id } : {}
      params.spaceId = props.spaceId
      const file = new File([blob], 'cropped_image.png', { type: 'image/png' })
      const res = await uploadPictureUsingPost(params, {}, file)
      if (res.data.code === 0 && res.data.data) {
        message.success('图片编辑成功')
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

// 资源释放
onUnmounted(() => {
  if (websocket.value) websocket.value.disconnect()
  editingUser.value = undefined
})

defineExpose({ openModal })
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
.image-edit-actions {
  padding: 8px;
  background: #fff7e6;
  border-radius: 6px;
}
</style>
