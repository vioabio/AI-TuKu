import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

// Mock the API module
vi.mock('@/api/userController', () => ({
  getLoginUserUsingGet: vi.fn(),
}))

import { getLoginUserUsingGet } from '@/api/userController'

describe('useLoginUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('初始状态 loginUser 应默认为未登录', () => {
    const store = useLoginUserStore()
    expect(store.loginUser).toBeDefined()
    expect(store.loginUser.userName).toBe('未登录')
  })

  it('fetchLoginUser 成功后应更新用户信息', async () => {
    const mockUser = {
      id: 1,
      userAccount: 'testuser',
      userName: '测试用户',
      userRole: 'user',
      userAvatar: '',
    }

    vi.mocked(getLoginUserUsingGet).mockResolvedValue({
      data: {
        code: 0,
        data: mockUser,
      },
    } as any)

    const store = useLoginUserStore()
    await store.fetchLoginUser()

    expect(store.loginUser.id).toBe(1)
    expect(store.loginUser.userAccount).toBe('testuser')
    expect(store.loginUser.userName).toBe('测试用户')
  })

  it('fetchLoginUser 失败时不应更新状态', async () => {
    vi.mocked(getLoginUserUsingGet).mockResolvedValue({
      data: {
        code: 50000,
        data: null,
        message: 'Internal Error',
      },
    } as any)

    const store = useLoginUserStore()
    await store.fetchLoginUser()

    // 状态应保持默认值
    expect(store.loginUser.userName).toBe('未登录')
  })

  it('fetchLoginUser code 不为 0 时不应更新', async () => {
    vi.mocked(getLoginUserUsingGet).mockResolvedValue({
      data: {
        code: 40100,
        data: null,
      },
    } as any)

    const store = useLoginUserStore()
    await store.fetchLoginUser()

    expect(store.loginUser.userName).toBe('未登录')
  })

  it('setLoginUser 应直接更新状态', () => {
    const store = useLoginUserStore()
    const newUser = {
      id: 2,
      userAccount: 'admin',
      userName: '管理员',
      userRole: 'admin',
    }

    store.setLoginUser(newUser)

    expect(store.loginUser.id).toBe(2)
    expect(store.loginUser.userRole).toBe('admin')
  })
})
