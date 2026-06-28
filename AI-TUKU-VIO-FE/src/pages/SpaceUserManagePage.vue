<template>
  <div id="spaceUserManagePage">
    <h2 style="margin-bottom: 16px">空间成员管理</h2>
    <a-card title="添加成员" style="margin-bottom: 16px">
      <a-form layout="inline" :model="formData" @finish="handleSubmit">
        <a-form-item label="用户ID" name="userId">
          <a-input v-model:value="formData.userId" placeholder="请输入用户ID" />
        </a-form-item>
        <a-form-item label="角色" name="spaceRole">
          <a-select v-model:value="formData.spaceRole"
            :options="SPACE_ROLE_OPTIONS" placeholder="选择角色" style="width: 120px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">添加</a-button>
        </a-form-item>
      </a-form>
    </a-card>
    <a-card title="成员列表">
      <a-table :columns="columns" :data-source="dataList" :pagination="false"
        row-key="id" :loading="loading">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userInfo'">
            <a-space>
              <a-avatar :src="record.user?.userAvatar" size="small" />
              {{ record.user?.userName ?? '无名' }}
            </a-space>
          </template>
          <template v-if="column.dataIndex === 'spaceRole'">
            <a-tag :color="roleColorMap[record.spaceRole]">
              {{ SPACE_ROLE_MAP[record.spaceRole] }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-select v-model:value="record.editRole"
                :options="SPACE_ROLE_OPTIONS" style="width: 100px" size="small" />
              <a-button type="link" size="small" @click="doEdit(record)">保存</a-button>
              <a-popconfirm title="确定移除该成员？" @confirm="doDelete(record)">
                <a-button type="link" danger size="small">移除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import { listSpaceUserUsingPost, addSpaceUserUsingPost, deleteSpaceUserUsingPost, editSpaceUserUsingPost } from '@/api/spaceUserController'
import { SPACE_ROLE_OPTIONS, SPACE_ROLE_MAP } from '@/constants/space'

const route = useRoute()
const spaceId = Number(route.params.id) || 0

const dataList = ref<any[]>([])
const loading = ref(false)
const formData = reactive({ userId: '', spaceRole: 'viewer' })

const roleColorMap: Record<string, string> = { viewer: 'green', editor: 'blue', admin: 'red' }

const columns = [
  { title: '用户', dataIndex: 'userInfo', key: 'userInfo' },
  { title: '角色', dataIndex: 'spaceRole', key: 'spaceRole' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'action' },
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listSpaceUserUsingPost({ spaceId })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = (res.data.data).map((item: any) => ({
        ...item,
        editRole: item.spaceRole,
      }))
    } else {
      message.error('获取成员列表失败')
    }
  } catch (error: any) {
    message.error('获取成员列表失败')
  }
  loading.value = false
}

const handleSubmit = async () => {
  if (!formData.userId) { message.warning('请输入用户ID'); return }
  try {
    const res = await addSpaceUserUsingPost({
      spaceId,
      userId: Number(formData.userId),
      spaceRole: formData.spaceRole,
    })
    if (res.data.code === 0) {
      message.success('添加成功')
      formData.userId = ''
      fetchData()
    } else {
      message.error('添加失败，' + res.data.message)
    }
  } catch (error: any) {
    message.error('添加失败')
  }
}

const doEdit = async (record: any) => {
  try {
    const res = await editSpaceUserUsingPost({ id: record.id, spaceRole: record.editRole })
    if (res.data.code === 0) { message.success('修改成功'); fetchData() }
    else message.error('修改失败')
  } catch { message.error('修改失败') }
}

const doDelete = async (record: any) => {
  try {
    const res = await deleteSpaceUserUsingPost({ id: record.id })
    if (res.data.code === 0) { message.success('移除成功'); fetchData() }
    else message.error('移除失败')
  } catch { message.error('移除失败') }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
#spaceUserManagePage { max-width: 900px; margin: 0 auto; }
</style>
