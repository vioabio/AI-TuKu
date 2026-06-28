<template>
  <div id="spaceDetailPage">
    <!-- 空间信息 -->
    <a-flex justify="space-between">
      <h2>{{ space.spaceName }}（私有空间）</h2>
      <a-space size="middle">
        <a-button type="primary" :href="`/add_picture?spaceId=${id}`" target="_blank">
          + 创建图片
        </a-button>
        <a-button @click="doSpaceAnalyze">空间分析</a-button>
        <a-tooltip
          :title="`占用空间 ${formatSize(space.totalSize ?? 0)} / ${formatSize(space.maxSize ?? 0)}`"
        >
          <a-progress
            type="circle"
            :percent="space.maxSize ? +(((space.totalSize ?? 0) * 100) / space.maxSize).toFixed(1) : 0"
            :size="42"
          />
        </a-tooltip>
      </a-space>
    </a-flex>
    <div style="margin-bottom: 16px" />
    <!-- 图片列表 -->
    <PictureList :dataList="dataList" :loading="loading" showOp :canEdit="true" :canDelete="true" :onReload="fetchData" />
    <!-- 分页 -->
    <a-pagination
      style="text-align: right"
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      :show-total="() => `图片总数 ${total} / ${space.maxCount ?? 0}`"
      @change="onPageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import { listPictureVoByPageUsingPost } from '@/api/pictureController.ts'
import PictureList from '@/components/PictureList.vue'

const router = useRouter()
const props = defineProps<{ id: string | number }>()
// 模板中使用的 id
const id = props.id

const space = ref<API.SpaceVO>({})
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({ id: props.id })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    } else {
      message.error('获取空间详情失败，' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取空间详情失败，' + (error?.message || '网络异常'))
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { spaceId: props.id, ...searchParams, nullSpaceId: false }
    const res = await listPictureVoByPageUsingPost(params as any)
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取数据失败，' + (error?.message || '网络异常'))
  }
  loading.value = false
}

const doSpaceAnalyze = () => {
  router.push({ path: '/space_analyze', query: { spaceId: id as string } })
}

const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}

const formatSize = (size: number) => {
  if (!size) return '0 B'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
}

onMounted(() => {
  fetchSpaceDetail()
  fetchData()
})
</script>