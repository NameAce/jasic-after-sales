import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const META_LINES = [' * @修改人 黄碧莲', ' * @修改时间 2026-05-22'];
const META_INLINE = META_LINES.join('\n * ');

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src');

const P2_DIRS = ['views/manage', 'layouts', 'utils', 'hooks'];
const P2_EXTRA = ['store/modules/route/shared.ts', 'router/guard/route.ts', 'router/elegant/transform.ts'];

const fnPatterns = [
  /^async function (\w+)\(/,
  /^function (\w+)\(/,
  /^const (\w+) = async \(/,
  /^const (\w+) = \([^)]*\) =>/,
  /^export async function (\w+)\(/,
  /^export function (\w+)\(/
];

function walk(rel, exts) {
  const dir = path.join(ROOT, rel);
  const out = [];
  if (!fs.existsSync(dir)) return out;
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) out.push(...walk(path.join(rel, ent.name), exts));
    else if (exts.some(e => ent.name.endsWith(e))) out.push(path.join(rel, ent.name).replace(/\\/g, '/'));
  }
  return out;
}

function collectTargets() {
  const set = new Set();
  for (const d of P2_DIRS) {
    for (const f of walk(d, ['.vue', '.ts'])) set.add(f);
  }
  for (const f of P2_EXTRA) set.add(f);
  return [...set].sort();
}

function inferPurpose(name) {
  const n = name;
  if (/^use[A-Z]/.test(n)) return `组合式函数：${n}`;
  if (/^loadList$|^loadRows$|^getData/.test(n)) return '加载列表或分页数据';
  if (n.startsWith('load')) return `加载数据：${n}`;
  if (n.startsWith('open')) return `打开弹层/抽屉：${n}`;
  if (/^submit|^handleSubmit/.test(n)) return `校验并提交：${n}`;
  if (/^remove|^delete/.test(n)) return `删除：${n}`;
  if (n.startsWith('reset')) return `重置：${n}`;
  if (n.startsWith('handle')) return `处理事件：${n}`;
  if (n.startsWith('format')) return `格式化：${n}`;
  if (n.startsWith('get')) return `读取/解析：${n}`;
  if (/^is|^has|^can|^should|^looksLike/.test(n)) return `判断：${n}`;
  if (/^apply|^sync|^set|^clear|^toggle|^close/.test(n)) return `状态处理：${n}`;
  if (/^build|^create|^transform|^parse|^normalize/.test(n)) return `构造/转换：${n}`;
  if (/^to[A-Z]/.test(n)) return `类型/值转换：${n}`;
  if (/^on[A-Z]/.test(n)) return `回调：${n}`;
  if (/^read|^write|^save|^init/.test(n)) return `${n}`;
  return `工具/模块方法：${n}`;
}

function inferReturns(name, line) {
  if (/^export async function|^async function|^const \w+ = async/.test(line)) return 'Promise';
  if (/^export function is|^function is|^export function has|^function has|^export function can/.test(line))
    return 'boolean';
  if (/^export function get|^function get|^export function format|^function format/.test(line)) return '对应类型';
  return 'void';
}

function injectMetaIntoExistingJsdoc(lines) {
  let changed = 0;
  const out = [];
  let i = 0;
  while (i < lines.length) {
    if (lines[i].trim() !== '/**') {
      out.push(lines[i]);
      i++;
      continue;
    }
    let j = i + 1;
    while (j < lines.length && lines[j].trim() !== '*/') j++;
    if (j >= lines.length) {
      out.push(lines[i]);
      i++;
      continue;
    }
    const block = lines.slice(i, j + 1);
    if (!block.join('\n').includes('@修改人')) {
      block.splice(block.length - 1, 0, ...META_LINES);
      changed++;
    }
    out.push(...block);
    i = j + 1;
  }
  return { lines: out, changed };
}

function processFile(relPath) {
  const filePath = path.join(ROOT, relPath);
  if (!fs.existsSync(filePath)) {
    console.log(`SKIP ${relPath}`);
    return;
  }
  let lines = fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n').split('\n');
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

    // 已有 JSDoc 但缺元信息：交给 injectMetaIntoExistingJsdoc
    if (prev.trim().endsWith('*/')) continue;

    const purpose = inferPurpose(name);
    const returns = inferReturns(name, line);
    const block = ['/**', ` * 作用：${purpose}。`, ` * @returns ${returns}`, ...META_LINES, ' */'];
    inserts.push({ line: i, block });
  }

  for (let k = inserts.length - 1; k >= 0; k--) {
    lines.splice(inserts[k].line, 0, ...inserts[k].block);
  }

  const metaResult = injectMetaIntoExistingJsdoc(lines);
  lines = metaResult.lines;

  if (relPath.endsWith('.vue') && lines[0]?.includes('<script')) {
    const scriptDocIdx = lines.findIndex((l, idx) => idx < 20 && l.trim() === '/**');
    if (scriptDocIdx >= 0) {
      const endIdx = lines.findIndex((l, idx) => idx > scriptDocIdx && idx < 40 && l.trim() === '*/');
      if (
        endIdx >= 0 &&
        !lines
          .slice(scriptDocIdx, endIdx + 1)
          .join('\n')
          .includes('@修改人')
      ) {
        lines.splice(endIdx, 0, ...META_LINES);
        metaResult.changed++;
      }
    } else if (lines[0]?.includes('<script setup')) {
      const insertAt = lines.findIndex((l, idx) => idx < 5 && l.includes('<script'));
      if (insertAt >= 0) {
        lines.splice(insertAt + 1, 0, '/**', ' * 页面/组件说明（见同级业务模块）。', ...META_LINES, ' */');
        metaResult.changed++;
      }
    }
  } else if (relPath.endsWith('.ts') && !lines.slice(0, 8).join('\n').includes('@修改人')) {
    const firstExport = lines.findIndex(l => l.startsWith('export '));
    const idx = firstExport > 0 ? firstExport : 0;
    if (idx >= 0 && lines[0]?.trim() !== '/**') {
      const header = relPath.includes('utils/') ? ` * 工具模块：${path.basename(relPath)}。` : ` * 模块：${relPath}。`;
      lines.splice(0, 0, '/**', header, ...META_LINES, ' */', '');
      metaResult.changed++;
    }
  }

  const total = inserts.length + metaResult.changed;
  if (total > 0) {
    fs.writeFileSync(filePath, lines.join('\n'), 'utf8');
    console.log(`${relPath}: +${inserts.length} blocks, meta ${metaResult.changed}`);
  } else {
    console.log(`${relPath}: ok`);
  }
}

const targets = collectTargets();
console.log(`P2 targets: ${targets.length} files\n`);
for (const f of targets) processFile(f);
