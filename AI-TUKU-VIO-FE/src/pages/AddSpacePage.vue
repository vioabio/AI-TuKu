<template>
  <div id="addSpacePage">
    <h2 style="margin-bottom: 16px">{{ isEdit ? '编辑空间' : '创建空间' }}</h2>
    <!-- 空间级别介绍 -->
    <a-card title="空间级别介绍" style="margin-bottom: 16px">
      <a-typography-paragraph>
        * 目前仅支持开通普通版，如需升级空间，请联系管理员。
      </a-typography-paragraph>
      <a-typography-paragraph v-for="spaceLevel in spaceLevelList" :key="spaceLevel.value">
        {{ spaceLevel.text }}： 大小 {{ formatSize(spaceLevel.maxSize) }}， 数量 {{ spaceLevel.maxCount }}
      </a-typography-paragraph>
    </a-card>
    <!-- 表单 -->
    <a-form name="formData" layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item name="spaceName" label="空间名称">
        <a-input v-model:value="formData.spaceName" placeholder="请输入空间名称" allow-clear />
      </a-form-item>
      <a-form-item v-if="!isEdit" name="spaceLevel" label="空间级别">
        <a-select
          v-model:value="formData.spaceLevel"
          :options="SPACE_LEVEL_OPTIONS"
          placeholder="请输入空间级别"
          style="min-width: 180px"
        />
      </a-form-item>
      <a-form-item v-if="!isEdit" name="spaceType" label="空间类型">
        <a-radio-group v-model:value="formData.spaceType" :options="SPACE_TYPE_OPTIONS" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">提交</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { addSpaceUsingPost, getSpaceVoByIdUsingGet, updateSpaceUsingPost } from '@/api/spaceController.ts'
import { listSpaceLevelUsingGet } from '@/api/spaceController.ts'
import { SPACE_LEVEL_ENUM, SPACE_LEVEL_OPTIONS, SPACE_TYPE_ENUM, SPACE_TYPE_OPTIONS } from '@/constants/space'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()

const isEdit = !!route.query?.id
const loading = ref(false)
const spaceLevelList = ref<API.SpaceLevel[]>([])

const formData = reactive<API.SpaceAddRequest>({
  spaceName: '',
  spaceLevel: SPACE_LEVEL_ENUM.COMMON,
  spaceType: SPACE_TYPE_ENUM.PRIVATE,
})

const handleSubmit = async () => {
  loading.value = true
  if (isEdit) {
    const res = await updateSpaceUsingPost({
      id: Number(route.query.id),
      spaceName: formData.spaceName,
    } as API.SpaceUpdateRequest)
    if (res.data.code === 0 && res.data.data) {
      message.success('更新成功')
      router.push('/admin/spaceManage')
    } else {
      message.error('更新失败，' + res.data.message)
    }
  } else {
    const res = await addSpaceUsingPost({ ...formData })
    if (res.data.code === 0 && res.data.data) {
      message.success('创建成功')
      router.push({ path: `/space/${res.data.data}` })
    } else {
      message.error('创建失败，' + res.data.message)
    }
  }
  loading.value = false
}

const fetchSpaceLevelList = async () => {
  const res = await listSpaceLevelUsingGet()
  if (res.data.code === 0 && res.data.data) {
    spaceLevelList.value = res.data.data
  }
}

const fetchOldSpace = async () => {
  const id = route.query?.id
  if (id) {
    const res = await getSpaceVoByIdUsingGet({ id: Number(id) })
    if (res.data.code === 0 && res.data.data) {
      formData.spaceName = res.data.data.spaceName ?? ''
    }
  }
}

const formatSize = (size?: number) => {
  if (!size) return '0 B'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
}

onMounted(() => {
  fetchSpaceLevelList()
  fetchOldSpace()
})
</script>

<style scoped>
#addSpacePage {
  max-width: 720px;
  margin: 0 auto;
}
</style>