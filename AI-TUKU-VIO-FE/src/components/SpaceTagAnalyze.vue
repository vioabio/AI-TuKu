<template>
  <div class="space-tag-analyze">
    <a-card title="图片标签分析">
      <a-spin :spinning="loading">
        <v-chart v-if="dataList.length > 0" :option="options" style="height: 320px; max-width: 100%" />
        <div v-else style="text-align: center; padding: 40px; color: #999">暂无标签数据</div>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { message } from 'ant-design-vue'
import { getSpaceTagAnalyzeUsingPost } from '@/api/spaceAnalyzeController'

interface Props {
  spaceId?: string
  queryAll?: boolean
  queryPublic?: boolean
}

const props = defineProps<Props>()
const dataList = ref<API.SpaceTagAnalyzeResponse[]>([])
const loading = ref(true)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSpaceTagAnalyzeUsingPost({
      spaceId: props.spaceId,
      queryAll: props.queryAll,
      queryPublic: props.queryPublic,
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data
    } else {
      message.error('获取标签分析失败，' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取标签分析失败')
  }
  loading.value = false
}

watchEffect(() => { fetchData() })

const options = computed(() => {
  const topTags = dataList.value.slice(0, 30)
  return {
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'wordCloud',
        shape: 'circle',
        left: 'center',
        top: 'center',
        width: '90%',
        height: '90%',
        sizeRange: [14, 48],
        rotationRange: [-45, 45],
        textStyle: {
          fontFamily: 'sans-serif',
          fontWeight: 'bold',
          color: () =>
            'rgb(' +
            Math.round(Math.random() * 200 + 55) +
            ',' +
            Math.round(Math.random() * 200 + 55) +
            ',' +
            Math.round(Math.random() * 200 + 55) +
            ')',
        },
        emphasis: {
          textStyle: { shadowBlur: 10, shadowColor: '#333' },
        },
        data: topTags.map(item => ({
          name: item.tag,
          value: item.count,
        })),
      },
    ],
  }
})
</script>
