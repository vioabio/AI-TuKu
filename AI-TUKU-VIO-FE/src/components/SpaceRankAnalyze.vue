<template>
  <div class="space-rank-analyze">
    <a-card title="空间使用排行">
      <a-spin :spinning="loading">
        <a-table
          :columns="columns"
          :data-source="dataList"
          :pagination="false"
          size="small"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'totalSize'">
              {{ formatSize(record.totalSize) }}
            </template>
          </template>
        </a-table>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { message } from 'ant-design-vue'
import { getSpaceRankAnalyzeUsingPost } from '@/api/spaceAnalyzeController'
import { formatSize } from '@/components/utils'

interface Props {
  spaceId?: string
  queryAll?: boolean
  queryPublic?: boolean
}

const props = defineProps<Props>()
const dataList = ref<API.Space[]>([])
const loading = ref(true)

const columns = [
  { title: '排名', key: 'index', width: 60, customRender: ({ index }: any) => index + 1 },
  { title: '空间名称', dataIndex: 'spaceName', key: 'spaceName' },
  { title: '用户 ID', dataIndex: 'userId', key: 'userId' },
  { title: '使用大小', key: 'totalSize' },
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSpaceRankAnalyzeUsingPost({ topN: 10 })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data
    } else {
      message.error('获取空间排行失败，' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取空间排行失败')
  }
  loading.value = false
}

watchEffect(() => { fetchData() })
</script>
