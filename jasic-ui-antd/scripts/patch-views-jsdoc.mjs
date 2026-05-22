import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const META = '@修改人 黄碧莲\n * @修改时间 2026-05-22';

const TARGET_FILES = [
  'views/org/index.vue',
  'views/advanced-modules/index.vue',
  'views/advanced-modules/config-form-panel.vue',
  'views/system/notify-scene/index.vue',
  'views/system/notify-trace/index.vue',
  'views/system/user/index.vue',
  'views/system/menu/index.vue',
  'views/system/role/index.vue',
  'views/system/branch/index.vue',
  'views/system/branch/branch-site-orders-drawer.vue',
  'views/notify/index.vue',
  'views/user-center/index.vue',
  'views/log/index.vue',
  'views/company-address/index.vue'
];

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src');

function inferPurpose(name) {
  const n = name;
  if (/^loadList$|^loadRows$/.test(n)) return '加载当前 Tab/条件下表格数据';
  if (n.startsWith('load')) return `加载数据：${n}`;
  if (/^open.*Form$|^open.*Drawer$|^open.*Modal$/.test(n)) return `打开表单/抽屉：${n}`;
  if (/^submit.*Form$|^submit/.test(n)) return `校验并提交：${n}`;
  if (/^remove|^delete/.test(n)) return `删除记录：${n}`;
  if (/^reset.*Query$|^resetQuery/.test(n)) return `重置查询条件并刷新列表：${n}`;
  if (/^handle.*Search$/.test(n)) return `执行查询（回到第一页）：${n}`;
  if (n.startsWith('handle')) return `处理交互事件：${n}`;
  if (n.startsWith('pickRows')) return '从分页接口响应解析列表数组';
  if (n.startsWith('pickTotal')) return '从分页接口响应解析总条数';
  if (n.startsWith('format')) return `格式化展示：${n}`;
  if (n.startsWith('get')) return `读取/解析：${n}`;
  if (n.startsWith('is')) return `判断是否满足条件：${n}`;
  if (n.startsWith('ensure')) return `确保前置数据已加载：${n}`;
  if (n.startsWith('apply')) return `应用配置或路由参数：${n}`;
  if (n.startsWith('sync')) return `同步状态：${n}`;
  if (n.startsWith('trigger')) return `触发业务动作：${n}`;
  if (n.startsWith('build')) return `构造数据或配置：${n}`;
  if (/^to[A-Z]/.test(n)) return `类型/值转换：${n}`;
  if (/^on[A-Z]/.test(n)) return `组件回调：${n}`;
  if (n.startsWith('table')) return `表格相关：${n}`;
  if (n.startsWith('orgActionColumn')) return '按 Tab 生成操作列配置';
  if (/^preview|^openPreview/.test(n)) return '打开预览';
  if (/^import|^export/.test(n)) return `导入/导出：${n}`;
  if (n.startsWith('assign')) return `分配关联：${n}`;
  if (n.startsWith('save')) return `保存：${n}`;
  if (n.startsWith('init')) return `初始化：${n}`;
  if (n.startsWith('fetch')) return `请求接口：${n}`;
  if (n.startsWith('render')) return `渲染辅助：${n}`;
  if (n.startsWith('validate')) return `校验：${n}`;
  if (n.startsWith('close')) return `关闭弹层：${n}`;
  if (n.startsWith('toggle')) return `切换状态：${n}`;
  if (n.startsWith('expand')) return `展开/折叠：${n}`;
  return `页面内业务方法：${n}`;
}

function inferReturns(name, line) {
  if (line.startsWith('async function')) return 'Promise';
  if (/^function is|^function can|^function should|^function has/.test(line)) return 'boolean';
  if (/^function get|^function pick|^function format|^function build|^function to/.test(line)) return '对应类型或 void';
  if (/^function handle|^function reset|^function apply|^function sync|^function open|^function close/.test(line))
    return 'void';
  return 'void';
}

function processFile(relPath) {
  const filePath = path.join(ROOT, relPath);
  if (!fs.existsSync(filePath)) {
    console.log(`SKIP missing ${relPath}`);
    return;
  }
  let content = fs.readFileSync(filePath, 'utf8');
  const lines = content.split(/\r?\n/);
  const fnPatterns = [
    /^async function (\w+)\(/,
    /^function (\w+)\(/,
    /^const (\w+) = async \(/,
    /^const (\w+) = \([^)]*\) =>/
  ];
  const inserts = [];

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    let m = null;
    let name = '';
    for (const re of fnPatterns) {
      m = line.match(re);
      if (m) {
        name = m[1];
        break;
      }
    }
    if (!m) continue;

    let start = 0;
    for (let k = i - 1; k >= 0; k--) {
      if (fnPatterns.some(re => re.test(lines[k]))) {
        start = k + 1;
        break;
      }
    }
    const prev = lines.slice(start, i).join('\n');
    if (prev.includes('@修改人')) continue;

    const purpose = inferPurpose(name);
    const returns = inferReturns(name, line);
    const block = ['/**', ` * 作用：${purpose}。`, ` * @returns ${returns}`, ` * ${META}`, ' */'].join('\n');
    inserts.push({ line: i, block });
  }

  for (let k = inserts.length - 1; k >= 0; k--) {
    lines.splice(inserts[k].line, 0, inserts[k].block);
  }

  // 文件头 script 说明补元数据
  if (lines[0]?.includes('<script')) {
    const scriptDocIdx = lines.findIndex((l, idx) => idx < 15 && l.trim() === '/**');
    if (scriptDocIdx >= 0) {
      const endIdx = lines.findIndex((l, idx) => idx > scriptDocIdx && idx < 30 && l.trim() === '*/');
      if (
        endIdx >= 0 &&
        !lines
          .slice(scriptDocIdx, endIdx + 1)
          .join('\n')
          .includes('@修改人')
      ) {
        lines.splice(endIdx, 0, ' * @修改人 黄碧莲', ' * @修改时间 2026-05-22');
      }
    }
  }

  if (inserts.length) {
    content = lines.join('\n');
    fs.writeFileSync(filePath, content, 'utf8');
    console.log(`${relPath}: +${inserts.length}`);
  } else {
    console.log(`${relPath}: skip functions`);
  }
}

for (const f of TARGET_FILES) processFile(f);
