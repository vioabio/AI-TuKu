<template>
  <div class="space-user-analyze">
    <a-card title="用户上传行为分析">
      <template #extra>
        <a-radio-group v-model:value="timeDimension" button-style="solid" size="small">
          <a-radio-button value="day">日</a-radio-button>
          <a-radio-button value="week">周</a-radio-button>
          <a-radio-button value="month">月</a-radio-button>
        </a-radio-group>
      </template>
      <a-spin :spinning="loading">
        <v-chart v-if="dataList.length > 0" :option="options" style="height: 320px; max-width: 100%" />
        <div v-else style="text-align: center; padding: 40px; color: #999">暂无上传数据</div>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, watchEffect } from 'vue'
import { message } from 'ant-design-vue'
import { getSpaceUserAnalyzeUsingPost } from '@/api/spaceAnalyzeController'

interface Props {
  spaceId?: number
  queryAll?: boolean
  queryPublic?: boolean
}

const props = defineProps<Props>()
const dataList = ref<API.SpaceUserAnalyzeResponse[]>([])
const loading = ref(true)
const timeDimension = ref('day')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSpaceUserAnalyzeUsingPost({
      spaceId: props.spaceId,
      queryAll: props.queryAll,
      queryPublic: props.queryPublic,
      timeDimension: timeDimension.value,
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data
    } else {
      message.error('获取用户分析失败，' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取用户分析失败')
  }
  loading.value = false
}

// 监听 props 和时间维度变化
watchEffect(() => { fetchData() })
watch(() => timeDimension.value, () => { fetchData() })

const options = computed(() => {
  const periods = dataList.value.map(item => item.period)
  const countData = dataList.value.map(item => item.count)

  return {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: periods },
    yAxis: { type: 'value', name: '上传数量' },
    series: [
      {
        name: '上传数量',
        type: 'line',
        data: countData,
        smooth: true,
        areaStyle: { opacity: 0.3 },
      },
    ],
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
  }
})
</script>
