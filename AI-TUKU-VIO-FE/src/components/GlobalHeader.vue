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
          {{ loginUserStore.loginUser.userName }}
        </div>
        <div v-else>
          <a-button type="primary" herf="/user/login">登录</a-button>
        </div>
      </div>
    </a-col>
  </a-row>
  </div>
</template>
<script lang="ts" setup>
import { h, ref } from 'vue';
import { HomeOutlined} from '@ant-design/icons-vue';
import type { MenuProps } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import { useLoginUserStore } from '@/stores/useLoginUserStore';

const loginUserStore = useLoginUserStore()

const items = ref<MenuProps['items']>([
  {
    key: '/',
    icon: ()=>h(HomeOutlined),
    label: '首页',
    title: '首页'
  },
  {
    key:"/about",
    label: '关于',
    title: '关于'
  },
  {
    key:"others",
    label: h('a', { href: 'https://github.com/vioabio', target: '_blank' }, '作者'),
    title: '作者'
  }
]);

const router=useRouter();

// 绑定元素“key”隐式具有“any”类型。ts-plugin(7031)
const doMenuClick = ({ key }: { key: string }) => {
  // 外链菜单项不触发路由跳转（如"作者"跳转 GitHub）
  if (key.startsWith('/')) {
    router.push({ path: key })
  }
}

// 动态高亮当前界面
const current = ref<string[]>([router.currentRoute.value.path]);
router.afterEach((to) => {
  current.value = [to.path]
})


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