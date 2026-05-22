import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { hasTemplateRootComment, insertTemplateRootComment } from './template-comment-placement.mjs';

const META_LINES = [' * @修改人 黄碧莲', ' * @修改时间 2026-05-22'];
const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src');

const JSDOC_DIRS = [
  'views/work-order/components/WorkOrderDetailDrawer.vue',
  'components',
  'views/function',
  'views/_builtin',
  'views/about',
  'views/multi-menu',
  'App.vue'
];

const TEMPLATE_DIRS = [
  'views/home',
  'components',
  'views/function',
  'views/_builtin',
  'views/about',
  'views/multi-menu',
  'App.vue'
];

const fnPatterns = [
  /^async function (\w+)\(/,
  /^function (\w+)\(/,
  /^const (\w+) = async \(/,
  /^const (\w+) = \([^)]*\) =>/,
  /^export async function (\w+)\(/,
  /^export function (\w+)\(/
];

function walk(rel, exts) {
  const p = path.join(ROOT, rel);
  if (!fs.existsSync(p)) return [];
  if (!fs.statSync(p).isDirectory()) return [rel.replace(/\\/g, '/')];
  const out = [];
  for (const ent of fs.readdirSync(p, { withFileTypes: true })) {
    const r = `${rel}/${ent.name}`.replace(/\\/g, '/');
    if (ent.isDirectory()) out.push(...walk(r, exts));
    else if (exts.some(e => ent.name.endsWith(e))) out.push(r);
  }
  return out;
}

function inferPurpose(name) {
  if (/^use[A-Z]/.test(name)) return `组合式函数：${name}`;
  if (/^load|^fetch|^getData/.test(name)) return `加载数据：${name}`;
  if (/^open|^submit|^handle|^reset|^toggle|^close/.test(name)) return `业务/交互：${name}`;
  if (/^format|^get|^is|^has|^can/.test(name)) return `${name}`;
  return `方法：${name}`;
}

function inferReturns(line) {
  if (/async/.test(line)) return 'Promise';
  if (/^function is|^function has|^function can/.test(line)) return 'boolean';
  return 'void';
}

function injectMeta(content) {
  return content
    .replace(/\/\*\*([^*@][^*]*)\*\//g, (full, inner) => {
      if (full.includes('@修改人')) return full;
      return `/**\n * ${inner.trim()}\n${META_LINES.join('\n')}\n */`;
    })
    .replace(/\/\*\*([\s\S]*?)\*\//g, (full, inner) => {
      if (String(inner).includes('@修改人')) return full;
      const trimmed = String(inner).replace(/\s+$/, '');
      return `/**${trimmed}\n${META_LINES.join('\n')}\n */`;
    });
}

function processJsdoc(rel) {
  const fp = path.join(ROOT, rel);
  let lines = fs.readFileSync(fp, 'utf8').replace(/\r\n/g, '\n').split('\n');
  const inserts = [];

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    let name = '';
    for (const re of fnPatterns) {
      const m = line.match(re);
      if (m) {
        name = m[1];
        break;
      }
    }
    if (!name) continue;
    let start = 0;
    for (let k = i - 1; k >= 0; k--) {
      if (fnPatterns.some(re => re.test(lines[k]))) {
        start = k + 1;
        break;
      }
    }
    const prev = lines.slice(start, i).join('\n');
    if (prev.includes('@修改人')) continue;
    if (prev.trim().endsWith('*/')) continue;
    inserts.push({
      line: i,
      block: ['/**', ` * 作用：${inferPurpose(name)}。`, ` * @returns ${inferReturns(line)}`, ...META_LINES, ' */']
    });
  }

  for (let k = inserts.length - 1; k >= 0; k--) {
    lines.splice(inserts[k].line, 0, ...inserts[k].block);
  }

  const content = lines.join('\n');
  const after = injectMeta(content);
  if (after !== content || inserts.length) {
    fs.writeFileSync(fp, after, 'utf8');
    console.log(`JSDoc ${rel}: +${inserts.length} blocks, meta pass`);
  }

  // 文件头
  lines = after.split('\n');
  if (rel.endsWith('.vue') && lines[0]?.includes('<script')) {
    const idx = lines.findIndex((l, i) => i < 25 && l.trim() === '/**');
    if (idx >= 0) {
      const end = lines.findIndex((l, i) => i > idx && i < 40 && l.trim() === '*/');
      if (
        end >= 0 &&
        !lines
          .slice(idx, end + 1)
          .join('\n')
          .includes('@修改人')
      ) {
        lines.splice(end, 0, ...META_LINES);
        fs.writeFileSync(fp, lines.join('\n'), 'utf8');
        console.log(`JSDoc ${rel}: file header meta`);
      }
    } else {
      const si = lines.findIndex((l, i) => i < 5 && l.includes('<script'));
      if (si >= 0) {
        const title = rel.includes('work-order')
          ? '工单详情抽屉：只读信息、流程操作与维修/关单等表单。'
          : rel.includes('components/')
            ? `通用组件：${path.basename(rel, '.vue')}。`
            : rel.includes('_builtin')
              ? `内置页：${path.basename(rel, '.vue')}。`
              : `页面：${path.basename(rel, '.vue')}。`;
        lines.splice(si + 1, 0, '/**', ` * ${title}`, ...META_LINES, ' */');
        fs.writeFileSync(fp, lines.join('\n'), 'utf8');
        console.log(`JSDoc ${rel}: new file header`);
      }
    }
  }
}

const HOME_TPL = {
  'home-section-pie-chart': '首页区块饼图',
  'home-section-line-chart': '首页区块折线图',
  'home-section-bar-chart': '首页区块柱状图',
  'home-trend-chart': '首页趋势图',
  'home-metric-cards': '首页指标卡片',
  'home-workbench-header': '工作台页头',
  'service-metric-cards': '服务商指标卡片',
  'service-trend-chart': '服务商趋势图',
  'service-pool-pie-chart': '服务池饼图',
  'service-header-banner': '服务商横幅',
  'service-history-entry-card': '历史工单入口卡片',
  'hq-site-bar-chart': '总部网点柱状图',
  'hq-status-bar-chart': '总部状态柱状图',
  'hq-kpi-cards': '总部 KPI 卡片',
  'hq-dashboard-section': '总部看板区块',
  'platform-kpi-cards': '平台 KPI 卡片',
  'platform-org-pie-chart': '平台组织饼图',
  'platform-oper-log-chart': '平台操作日志图',
  'platform-header-banner': '平台横幅',
  'platform-dashboard-section': '平台看板区块',
  'header-banner': '通用头部横幅',
  'creativity-banner': '创意横幅',
  'card-data': '卡片数据展示',
  'project-news': '项目动态',
  'pie-chart': '饼图封装',
  'line-chart': '折线图封装'
};

function tplText(rel) {
  const base = path.basename(rel, '.vue');
  if (rel.startsWith('views/home/')) {
    if (rel.endsWith('standard-home-index.vue')) return '标准首页入口：按角色组合各业务模块';
    if (rel.endsWith('hq-home-index.vue')) return '总部首页入口';
    if (rel.endsWith('platform-home-index.vue')) return '平台首页入口';
    if (rel.endsWith('index.vue')) return '首页路由入口（转发到对应角色首页）';
    return HOME_TPL[base] || `首页模块：${base}`;
  }
  if (rel.startsWith('components/')) return `通用组件：${base}`;
  if (rel.startsWith('views/_builtin/')) return `内置页：${rel.replace('views/_builtin/', '')}`;
  if (rel.startsWith('views/function/')) return `功能演示页：${rel.replace('views/function/', '')}`;
  if (rel === 'App.vue') return '应用根组件：ConfigProvider 与路由出口';
  if (rel.startsWith('views/about/')) return '关于页';
  if (rel.startsWith('views/multi-menu/')) return `多级菜单演示：${base}`;
  return base;
}

function processTemplate(rel) {
  const fp = path.join(ROOT, rel);
  const lines = fs.readFileSync(fp, 'utf8').replace(/\r\n/g, '\n').split('\n');
  const tplIdx = lines.findIndex(l => l.trim() === '<template>');
  if (tplIdx < 0) return;
  if (hasTemplateRootComment(lines, tplIdx)) return;
  insertTemplateRootComment(lines, tplIdx, tplText(rel));
  fs.writeFileSync(fp, lines.join('\n'), 'utf8');
  console.log(`Template ${rel}`);
}

console.log('=== Step 1: work-order DetailDrawer meta ===\n');
processJsdoc('views/work-order/components/WorkOrderDetailDrawer.vue');

console.log('\n=== Step 2: home template comments ===\n');
for (const f of walk('views/home', ['.vue'])) processTemplate(f);

console.log('\n=== Step 3: components JSDoc + template ===\n');
for (const f of walk('components', ['.vue'])) {
  processJsdoc(f);
  processTemplate(f);
}

console.log('\n=== Step 4: demo/builtin pages ===\n');
for (const d of ['views/function', 'views/_builtin', 'views/about', 'views/multi-menu']) {
  for (const f of walk(d, ['.vue'])) {
    processJsdoc(f);
    processTemplate(f);
  }
}
processJsdoc('App.vue');
processTemplate('App.vue');

console.log('\n=== Done ===');
