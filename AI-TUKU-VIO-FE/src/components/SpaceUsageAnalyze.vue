<template>
  <div class="space-usage-analyze">
    <a-card title="空间资源使用">
      <a-spin :spinning="loading">
        <a-flex gap="middle">
          <div style="width: 50%; text-align: center">
            <h3>{{ formatSize(data.usedSize ?? 0) }} / {{ data.maxSize ? formatSize(data.maxSize) : '无限制' }}</h3>
            <a-progress type="dashboard" :percent="data.sizeUsageRatio ?? 0" />
            <div style="margin-top: 8px; color: #888">存储空间</div>
          </div>
          <div style="width: 50%; text-align: center">
            <h3>{{ data.usedCount ?? 0 }} / {{ data.maxCount ?? '无限制' }}</h3>
            <a-progress type="dashboard" :percent="data.countUsageRatio ?? 0" />
            <div style="margin-top: 8px; color: #888">图片数量</div>
          </div>
        </a-flex>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { message } from 'ant-design-vue'
import { getSpaceUsageAnalyzeUsingPost } from '@/api/spaceAnalyzeController'
import { formatSize } from '@/components/utils'

interface Props {
  spaceId?: number
  queryAll?: boolean
  queryPublic?: boolean
}

const props = defineProps<Props>()
const data = ref<API.SpaceUsageAnalyzeResponse>({})
const loading = ref(true)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSpaceUsageAnalyzeUsingPost({
      spaceId: props.spaceId,
      queryAll: props.queryAll,
      queryPublic: props.queryPublic,
    })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data
    } else {
      message.error('获取空间使用分析失败，' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取空间使用分析失败')
  }
  loading.value = false
}

watchEffect(() => { fetchData() })
</script>
