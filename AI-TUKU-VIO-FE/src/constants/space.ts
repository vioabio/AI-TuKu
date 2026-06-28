// 空间级别枚举
export const SPACE_LEVEL_ENUM = {
  COMMON: 0,
  PROFESSIONAL: 1,
  FLAGSHIP: 2,
} as const;

// 空间级别文本映射
export const SPACE_LEVEL_MAP: Record<number, string> = {
  0: '普通版',
  1: '专业版',
  2: '旗舰版',
};

// 空间级别选项映射
export const SPACE_LEVEL_OPTIONS = Object.keys(SPACE_LEVEL_MAP).map((key) => {
  const value = Number(key);
  return {
    label: SPACE_LEVEL_MAP[value],
    value,
  };
});

// 空间类型枚举
export const SPACE_TYPE_ENUM = {
  PRIVATE: 0,
  TEAM: 1,
} as const;

export const SPACE_TYPE_MAP: Record<number, string> = {
  0: '私有空间',
  1: '团队空间',
};

export const SPACE_TYPE_OPTIONS = [
  { label: '私有空间', value: 0 },
  { label: '团队空间', value: 1 },
];

// 空间角色枚举
export const SPACE_ROLE_ENUM = {
  VIEWER: 'viewer',
  EDITOR: 'editor',
  ADMIN: 'admin',
} as const;

export const SPACE_ROLE_MAP: Record<string, string> = {
  viewer: '浏览者',
  editor: '编辑者',
  admin: '管理员',
};

export const SPACE_ROLE_OPTIONS = [
  { label: '浏览者', value: 'viewer' },
  { label: '编辑者', value: 'editor' },
  { label: '管理员', value: 'admin' },
];

// 空间权限常量
export const SPACE_PERMISSION_ENUM = {
  SPACE_USER_MANAGE: 'spaceUser:manage',
  PICTURE_VIEW: 'picture:view',
  PICTURE_UPLOAD: 'picture:upload',
  PICTURE_EDIT: 'picture:edit',
  PICTURE_DELETE: 'picture:delete',
} as const;