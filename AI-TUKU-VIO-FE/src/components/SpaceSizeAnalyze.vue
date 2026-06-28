<template>
  <div class="space-size-analyze">
    <a-card title="空间图片大小分析">
      <a-spin :spinning="loading">
        <v-chart v-if="dataList.length > 0" :option="options" style="height: 320px; max-width: 100%" />
        <div v-else style="text-align: center; padding: 40px; color: #999">暂无数据</div>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { message } from 'ant-design-vue'
import { getSpaceSizeAnalyzeUsingPost } from '@/api/spaceAnalyzeController'

interface Props {
  spaceId?: string
  queryAll?: boolean
  queryPublic?: boolean
}

const props = defineProps<Props>()
const dataList = ref<API.SpaceSizeAnalyzeResponse[]>([])
const loading = ref(true)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSpaceSizeAnalyzeUsingPost({
      spaceId: props.spaceId,
      queryAll: props.queryAll,
      queryPublic: props.queryPublic,
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data
    } else {
      message.error('获取大小分析失败，' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取大小分析失败')
  }
  loading.value = false
}

watchEffect(() => { fetchData() })

const options = computed(() => {
  const pieData = dataList.value.map(item => ({
    name: item.sizeRange,
    value: item.count,
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)',
    },
    legend: { top: 'bottom' },
    series: [
      {
        name: '图片大小',
        type: 'pie',
        radius: '50%',
        data: pieData,
      },
    ],
  }
})
</script>
