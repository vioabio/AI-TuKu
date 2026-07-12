import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'

vi.mock('@/api/userController', () => ({
  userRegisterUsingPost: vi.fn(),
}))

describe('UserRegisterPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('应成功挂载（smoke test）', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/user/register', name: '用户注册', component: UserRegisterPage },
      ],
    })

    const wrapper = mount(UserRegisterPage, {
      global: {
        plugins: [router, createPinia()],
        stubs: { GlobalHeader: true, RouterLink: true },
      },
    })

    expect(wrapper.exists()).toBe(true)
  })
})
