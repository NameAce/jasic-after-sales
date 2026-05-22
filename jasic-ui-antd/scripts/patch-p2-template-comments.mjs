import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src');

/** P2 Vue 页面/组件：template 根注释文案 */
const TEMPLATE_ROOT = {
  'views/manage/user/index.vue': '管理端演示 — 用户列表：搜索区 + 表格 + 新增/编辑抽屉',
  'views/manage/user/modules/user-operate-drawer.vue': '用户新增/编辑抽屉表单',
  'views/manage/user/modules/user-search.vue': '用户列表搜索表单',
  'views/manage/user-detail/[id].vue': '用户详情页（路由参数 id）',
  'views/manage/role/index.vue': '管理端演示 — 角色列表与权限弹窗入口',
  'views/manage/role/modules/role-search.vue': '角色列表搜索表单',
  'views/manage/role/modules/role-operate-drawer.vue': '角色新增/编辑抽屉',
  'views/manage/role/modules/menu-auth-modal.vue': '角色菜单权限分配弹窗',
  'views/manage/role/modules/button-auth-modal.vue': '角色按钮权限分配弹窗',
  'views/manage/menu/index.vue': '管理端演示 — 菜单树表与操作弹窗',
  'views/manage/menu/modules/menu-operate-modal.vue': '菜单新增/编辑弹窗',
  'layouts/base-layout/index.vue': '主布局：顶栏 + 侧栏/混合菜单 + 内容区与页签',
  'layouts/blank-layout/index.vue': '空白布局：仅渲染路由子页面（登录等）',
  'layouts/modules/global-header/index.vue': '顶栏：Logo、面包屑、主题与用户菜单',
  'layouts/modules/global-sider/index.vue': '侧栏容器：菜单与折叠',
  'layouts/modules/global-content/index.vue': '主内容区：路由视图与 keep-alive',
  'layouts/modules/global-tab/index.vue': '多页签栏：切换、关闭与右键菜单',
  'layouts/modules/global-breadcrumb/index.vue': '面包屑：随路由层级展示',
  'layouts/modules/global-footer/index.vue': '页脚版权信息',
  'layouts/modules/global-logo/index.vue': '侧栏/顶栏 Logo 区域',
  'layouts/modules/theme-drawer/index.vue': '主题配置抽屉入口',
  'layouts/modules/global-menu/index.vue': '全局菜单容器（按布局模式切换子菜单）'
};

function walk(rel) {
  const dir = path.join(ROOT, rel);
  const out = [];
  if (!fs.existsSync(dir)) return out;
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) out.push(...walk(path.join(rel, ent.name)));
    else if (ent.name.endsWith('.vue')) out.push(path.join(rel, ent.name).replace(/\\/g, '/'));
  }
  return out;
}

function hasRootComment(lines, tplIdx) {
  return lines.slice(tplIdx + 1, tplIdx + 6).some(l => l.trim().startsWith('<!--'));
}

for (const rel of [...walk('views/manage'), ...walk('layouts')]) {
  const filePath = path.join(ROOT, rel);
  const lines = fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n').split('\n');
  const tplIdx = lines.findIndex(l => l.trim() === '<template>');
  if (tplIdx < 0) continue;
  if (hasRootComment(lines, tplIdx)) {
    console.log(`${rel}: skip template`);
    continue;
  }
  const text =
    TEMPLATE_ROOT[rel] ||
    (rel.startsWith('layouts/')
      ? `布局子模块：${path.basename(rel, '.vue')}`
      : `管理端子模块：${path.basename(rel, '.vue')}`);
  lines.splice(tplIdx + 1, 0, `  <!-- ${text} -->`);
  fs.writeFileSync(filePath, lines.join('\n'), 'utf8');
  console.log(`${rel}: +template`);
}
