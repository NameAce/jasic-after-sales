/**
 * 根据已勾选节点 key，计算树应展开的节点 key（到勾选项路径上的祖先；若勾选节点仍有子节点则一并展开自身）。
 * 用于分配菜单等场景，打开抽屉时默认展开到已勾选项。
 *
 * @param nodes - 树数据（节点含 `id` 或 `key` 与 `children`）
 * @param checkedKeys - 已勾选 key 列表
 * @param nodeKey - 节点主键字段名，后端菜单树一般为 `id`，Ant Tree DataNode 为 `key`
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function computeExpandedKeysForCheckedMenuTree(
  nodes: unknown[] | undefined,
  checkedKeys: Array<string | number>,
  nodeKey: 'id' | 'key' = 'id'
): Array<string | number> {
  const checked = new Set(checkedKeys.map(k => Number(k)).filter(id => !Number.isNaN(id)));
  const expand = new Set<number>();

  function walk(list: unknown[], ancestors: number[]): void {
    for (const raw of list || []) {
      const node = raw as Record<string, unknown>;
      const rawKey = node[nodeKey];
      const id = typeof rawKey === 'number' ? rawKey : Number(String(rawKey ?? ''));
      if (Number.isNaN(id)) {
        /* skip */
      } else {
        const children = Array.isArray(node.children) ? node.children : [];
        if (children.length) {
          walk(children, [...ancestors, id]);
        }
        if (checked.has(id)) {
          ancestors.forEach(a => expand.add(a));
          if (children.length) {
            expand.add(id);
          }
        }
      }
    }
  }

  walk(nodes || [], []);
  return Array.from(expand);
}
