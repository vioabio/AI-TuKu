import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'

vi.mock('@/api/userController', () => ({
  userLoginUsingPost: vi.fn(),
}))

describe('UserLoginPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('应成功挂载（smoke test）', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/user/login', name: '用户登录', component: UserLoginPage },
      ],
    })

    const wrapper = mount(UserLoginPage, {
      global: {
        plugins: [router, createPinia()],
        stubs: { GlobalHeader: true, RouterLink: true },
      },
    })

    expect(wrapper.exists()).toBe(true)
  })
})
