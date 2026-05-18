/**
 * 作用：为指定目录下 TypeScript 文件中的 JSDoc 块注释追加统一维护元信息（@修改人、@修改时间）。
 * @remarks 仅处理尚未包含「修改人」标记的注释块；不修改已带元信息的文件内容。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import fs from 'node:fs';
import path from 'node:path';

const META = '\n * @修改人 黄碧莲\n * @修改时间 2026-05-14';

const ROOT = path.resolve(import.meta.dirname, '..');
/** 需注入 JSDoc 维护元信息的源码根目录（递归扫描其下全部 .ts，排除 .d.ts） */
const REL_DIRS = ['src'];

/**
 * 作用：递归收集目录下所有 `.ts` 文件路径（不含 `.d.ts`）。
 * @param dir 绝对目录路径
 * @returns {string[]} 文件路径列表
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function collectTsFiles(dir) {
  const out = [];
  if (!fs.existsSync(dir)) return out;
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) {
      // 跳过后端生成目录，避免与 gen-route 冲突后丢失维护信息
      if (!p.includes(`${path.sep}router${path.sep}elegant`)) {
        out.push(...collectTsFiles(p));
      }
    } else if (ent.isFile() && ent.name.endsWith('.ts') && !ent.name.endsWith('.d.ts')) out.push(p);
  }
  return out;
}

/**
 * 作用：为文件中每个不含 @修改人 的 JSDoc 块末尾注入维护元信息。
 * @param content 源文件全文
 * @returns {string} 处理后的全文
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function injectMeta(content) {
  return content.replace(/\/\*\*([\s\S]*?)\*\//g, (full, inner) => {
    if (String(inner).includes('@修改人')) return full;
    const trimmed = String(inner).replace(/\s+$/, '');
    return `/**${trimmed}${META}\n */`;
  });
}

/**
 * 作用：脚本入口：遍历配置目录并写回变更文件。
 * @returns {void}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function main() {
  let changed = 0;
  for (const rel of REL_DIRS) {
    const dir = path.join(ROOT, rel);
    for (const file of collectTsFiles(dir)) {
      const before = fs.readFileSync(file, 'utf8');
      const after = injectMeta(before);
      if (after !== before) {
        fs.writeFileSync(file, after, 'utf8');
        changed += 1;
        // eslint-disable-next-line no-console
        console.log('updated:', path.relative(ROOT, file));
      }
    }
  }
  // eslint-disable-next-line no-console
  console.log(`done, files changed: ${changed}`);
}

main();
