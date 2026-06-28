<template>
  <div id="mySpacePage" style="text-align: center; padding: 40px">
    <a-spin tip="正在加载空间..." />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController.ts'

const router = useRouter()

const checkUserSpace = async () => {
  const loginUser = useLoginUserStore().loginUser
  if (!loginUser?.id) {
    router.replace('/user/login')
    return
  }
  try {
    const res = await listSpaceVoByPageUsingPost({
      userId: loginUser.id,
      current: 1,
      pageSize: 1,
      spaceType: 0,
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records
      if (records && records.length > 0) {
        router.replace(`/space/${records[0].id}`)
      } else {
        router.replace('/add_space')
      }
    } else {
      message.error('获取空间失败，' + res.data.message)
      router.replace('/add_space')
    }
  } catch (error: any) {
    message.error('获取空间失败，' + (error?.message || '网络异常'))
    router.replace('/add_space')
  }
}

onMounted(() => { checkUserSpace() })
</script>