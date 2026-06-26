import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: () => import('../pages/user/UserLoginPage.vue'),
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: () => import('../pages/user/UserRegisterPage.vue'),
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: () => import('../pages/admin/UserManagePage.vue'),
    },
    {
      path: '/add_picture',
      name: '创建图片',
      component: () => import('../pages/AddPicturePage.vue')
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

export default router
