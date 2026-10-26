/**
 * 权限指令兼容模块
 * 重新导出 permission.ts 中的指令，保持向后兼容
 */
export {
  permission,
  auth,
  role,
  setupPermissionDirective,
  checkPermission,
  checkRole
} from './permission'
