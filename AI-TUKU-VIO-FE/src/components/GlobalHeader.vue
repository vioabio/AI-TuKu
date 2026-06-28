<template>
  <div id="globalHeader">
    <a-row :wrap="false">
    <a-col flex="200px">
      <!-- 网站图标部分 -->
      <router-link to="/">
        <div class="title-bar">
          <img class="logo" src="../assets/bilibili_logo.jpg" alt="Logo" >
          <div class="title">AI智能图库</div>
        </div>
      </router-link>
    </a-col>
    <a-col flex="auto">
      <!-- 中间菜单栏 -->
      <a-menu 
      v-model:selectedKeys="current" 
      mode="horizontal" 
      :items="items" 
      @click="doMenuClick"/>
    </a-col>
    <a-col flex="120px">
      <!-- 登录组件 -->
      <div class="user-login-status">
        <div v-if="loginUserStore.loginUser.id">
          <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
        </div>
        <div v-else>
          <a-button type="primary" href="/user/login">登录</a-button>
        </div>
      </div>
    </a-col>
  </a-row>
  </div>
</template>
<script lang="ts" setup>
import { computed, h, ref, watchEffect } from 'vue';
import { HomeOutlined,LogoutOutlined} from '@ant-design/icons-vue';
import { message, type MenuProps } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import { useLoginUserStore } from '@/stores/useLoginUserStore';
import { userLogoutUsingPost } from '@/api/userController.ts';
import { listMyTeamSpaceUsingPost } from '@/api/spaceUserController.ts';

const loginUserStore = useLoginUserStore()

const originItems = [
  {
    key: '/',
    icon: ()=>h(HomeOutlined),
    label: '首页',
    title: '首页'
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/add_picture',
    label: '创建图片',
    title: '创建图片',
  },
  {
    key: '/admin/pictureManage',
    label: '图片管理',
    title: '图片管理',
  },
  {
    key: '/add_picture/batch',
    label: '批量创建图片',
    title: '批量创建图片',
  },
  {
    key: '/admin/spaceManage',
    label: '空间管理',
    title: '空间管理',
  },
  {
    key: '/my_space',
    label: '我的空间',
    title: '我的空间',
  },
  {
    key: '/space_analyze?queryAll=true',
    label: '全空间分析',
    title: '全空间分析',
  },
  {
    key: '/space_analyze?queryPublic=true',
    label: '公共图库分析',
    title: '公共图库分析',
  },
  {
    key:"others",
    label: h('a', { href: 'https://github.com/vioabio', target: '_blank' }, '作者'),
    title: '作者'
  }
];

// 根据权限过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const key = menu?.key as string
    // 管理员才能看到 /admin 开头的菜单 和 全空间/公共图库分析
    if (key?.startsWith('/admin') || key?.startsWith('/space_analyze')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 我的团队空间列表
const teamSpaceList = ref<API.SpaceUserVO[]>([])

const fetchTeamSpaceList = async () => {
  const res = await listMyTeamSpaceUsingPost()
  if (res.data.code === 0 && res.data.data) {
    teamSpaceList.value = res.data.data
  }
}

watchEffect(() => {
  if (loginUserStore.loginUser?.id) fetchTeamSpaceList()
})

// 展示在菜单的路由数组（动态追加团队空间）
const items = computed(() => {
  const filtered = filterMenus(originItems) ?? []
  if (teamSpaceList.value.length > 0) {
    const teamSubMenus = teamSpaceList.value.map((su) => ({
      key: '/space/' + su.spaceId,
      label: su.space?.spaceName ?? '未知空间',
    }))
    const teamGroup = {
      type: 'group' as const,
      label: '我的团队',
      key: 'teamSpace',
      children: teamSubMenus,
    }
    return [...filtered, teamGroup]
  }
  return filtered
})

const router=useRouter();

// 绑定元素”key”隐式具有”any”类型。ts-plugin(7031)
const doMenuClick = ({ key }: { key: string }) => {
  // 外链菜单项不触发路由跳转（如”作者”跳转 GitHub）
  if (key.startsWith('/')) {
    // 支持 key 中携带 query 参数（如 /space_analyze?queryAll=true）
    const [path, queryStr] = key.split('?')
    const query: Record<string, string> = {}
    if (queryStr) {
      queryStr.split('&').forEach(pair => {
        const [k, v] = pair.split('=')
        query[k] = v
      })
    }
    router.push({ path, query: Object.keys(query).length > 0 ? query : undefined })
  }
}

// 动态高亮当前界面
const current = ref<string[]>([router.currentRoute.value.path]);
router.afterEach((to) => {
  current.value = [to.path]
})

// 用户注销
const doLogout = async () => {
  const res = await userLogoutUsingPost()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

</script>

<style scoped>
#globalHeader .title-bar{
  display: flex;
  align-items: center;
}

.title {
  color: black;
  font-size: 18px;
  margin-left: 16px;
}

.logo {
  height: 48px;
}


</style>