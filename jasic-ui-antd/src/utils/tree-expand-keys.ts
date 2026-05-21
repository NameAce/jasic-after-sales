/** Ant Design Vue Tree 父子联动时 v-model:checked-keys 为 { checked, halfChecked } */
export type TreeCheckedKeysValue =
  | Array<string | number>
  | { checked?: Array<string | number>; halfChecked?: Array<string | number> };

function toMenuNumericId(raw: string | number): number {
  return typeof raw === 'number' ? raw : Number(String(raw ?? ''));
}

/**
 * 将 ATree 的 checkedKeys（数组或 { checked, halfChecked }）规范为 key 数组。
 *
 * @param checkedKeys - v-model:checked-keys 当前值
 * @param includeHalfChecked - 是否合并半选父节点（非严格联动场景保存时可传 true）
 * @returns key 列表
 */
export function toTreeCheckedKeyList(
  checkedKeys: unknown,
  includeHalfChecked = false
): Array<string | number> {
  if (Array.isArray(checkedKeys)) {
    return checkedKeys;
  }
  if (checkedKeys && typeof checkedKeys === 'object') {
    const obj = checkedKeys as { checked?: unknown; halfChecked?: unknown };
    const checked = Array.isArray(obj.checked) ? (obj.checked as Array<string | number>) : [];
    if (!includeHalfChecked) {
      return checked;
    }
    const half = Array.isArray(obj.halfChecked) ? (obj.halfChecked as Array<string | number>) : [];
    return [...checked, ...half];
  }
  return [];
}

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
  checkedKeys: TreeCheckedKeysValue | unknown,
  nodeKey: 'id' | 'key' = 'id'
): Array<string | number> {
  const keyList = toTreeCheckedKeyList(checkedKeys, true);
  const checked = new Set(keyList.map(k => Number(k)).filter(id => !Number.isNaN(id)));
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

/**
 * 将接口 menuIds 转为父子联动树回显用的 `{ checked, halfChecked }`。
 * - checked：接口中明确授权的节点；目录节点若子树未全选则进入 halfChecked
 * - halfChecked：未写入接口但位于已授权节点路径上的父级（半选），避免整棵子树被联动勾满
 *
 * @param nodes - 菜单树
 * @param menuIds - 接口返回的已分配菜单 ID
 * @param nodeKey - 节点主键字段名
 * @returns 赋给 ATree v-model:checked-keys（非 checkStrictly）
 * @修改人 黄碧莲
 * @修改时间 2026-05-21
 */
export function buildLinkedTreeCheckedState(
  nodes: unknown[] | undefined,
  menuIds: Array<string | number>,
  nodeKey: 'id' | 'key' = 'id'
): { checked: Array<string | number>; halfChecked: Array<string | number> } {
  const idSet = new Set(menuIds.map(toMenuNumericId).filter(id => !Number.isNaN(id)));
  const checkedSet = new Set<string | number>();
  const halfSet = new Set<string | number>();

  function allDescendantsInSet(node: Record<string, unknown>): boolean {
    const children = Array.isArray(node.children) ? node.children : [];
    for (const child of children) {
      const c = child as Record<string, unknown>;
      const num = toMenuNumericId(c[nodeKey] as string | number);
      if (Number.isNaN(num) || !idSet.has(num)) {
        return false;
      }
      if (!allDescendantsInSet(c)) {
        return false;
      }
    }
    return true;
  }

  function walk(list: unknown[], ancestors: Array<{ num: number; key: string | number }>): void {
    for (const raw of list || []) {
      const node = raw as Record<string, unknown>;
      const rawKey = node[nodeKey];
      const num = toMenuNumericId(rawKey as string | number);
      if (Number.isNaN(num)) {
        continue;
      }
      const key = rawKey as string | number;
      const path = [...ancestors, { num, key }];
      const children = Array.isArray(node.children) ? node.children : [];

      if (idSet.has(num)) {
        if (children.length && !allDescendantsInSet(node)) {
          halfSet.add(key);
        } else {
          checkedSet.add(key);
        }
        for (const a of ancestors) {
          if (!idSet.has(a.num)) {
            halfSet.add(a.key);
            checkedSet.delete(a.key);
          }
        }
      }

      if (children.length) {
        walk(children, path);
      }
    }
  }

  walk(nodes || [], []);

  for (const k of halfSet) {
    checkedSet.delete(k);
  }

  return {
    checked: Array.from(checkedSet),
    halfChecked: Array.from(halfSet)
  };
}

/**
 * 将接口返回的 menuIds 转为与树节点 key 类型一致的 checked 列表，仅保留树中存在的 ID。
 *
 * @param nodes - 菜单树
 * @param menuIds - 接口返回的已分配菜单 ID
 * @param nodeKey - 节点主键字段名
 * @returns 与树节点 key 类型一致的 key 列表
 * @修改人 黄碧莲
 * @修改时间 2026-05-21
 */
export function resolveMenuTreeCheckedKeys(
  nodes: unknown[] | undefined,
  menuIds: Array<string | number>,
  nodeKey: 'id' | 'key' = 'id'
): Array<string | number> {
  const keyByNumericId = new Map<number, string | number>();

  function walk(list: unknown[]): void {
    for (const raw of list || []) {
      const node = raw as Record<string, unknown>;
      const rawKey = node[nodeKey];
      const num = typeof rawKey === 'number' ? rawKey : Number(String(rawKey ?? ''));
      if (!Number.isNaN(num)) {
        keyByNumericId.set(num, rawKey as string | number);
      }
      const children = Array.isArray(node.children) ? node.children : [];
      if (children.length) {
        walk(children);
      }
    }
  }

  walk(nodes || []);

  const resolved: Array<string | number> = [];
  const seen = new Set<number>();
  for (const raw of menuIds) {
    const num = toMenuNumericId(raw);
    if (Number.isNaN(num) || seen.has(num)) {
      continue;
    }
    const treeKey = keyByNumericId.get(num);
    if (treeKey !== undefined) {
      seen.add(num);
      resolved.push(treeKey);
    }
  }
  return resolved;
}

/**
 * 将树勾选结果扩展为「勾选节点 + 全部祖先菜单 ID」，用于分配菜单保存。
 * 后端按 menu_id 逐条写入关联表且建树时要求父节点存在于结果集，仅传叶子会导致侧栏/路由丢失父级目录。
 *
 * @param nodes - 菜单树（节点含 `id` 或 `key` 与 `children`）
 * @param checkedKeys - 树 v-model（含 halfChecked 的父级半选节点）
 * @param nodeKey - 节点主键字段名，后端菜单树一般为 `id`
 * @returns 去重后的菜单 ID 列表（含原勾选与祖先）
 * @修改人 黄碧莲
 * @修改时间 2026-05-21
 */
export function expandCheckedMenuIdsWithAncestors(
  nodes: unknown[] | undefined,
  checkedKeys: TreeCheckedKeysValue | unknown,
  nodeKey: 'id' | 'key' = 'id'
): number[] {
  const keyList = toTreeCheckedKeyList(checkedKeys, true);
  const checked = new Set(keyList.map(k => toMenuNumericId(k)).filter(id => !Number.isNaN(id)));
  const result = new Set<number>(checked);

  function walk(list: unknown[], ancestors: number[]): void {
    for (const raw of list || []) {
      const node = raw as Record<string, unknown>;
      const rawKey = node[nodeKey];
      const id = typeof rawKey === 'number' ? rawKey : Number(String(rawKey ?? ''));
      if (Number.isNaN(id)) {
        continue;
      }
      const children = Array.isArray(node.children) ? node.children : [];
      if (checked.has(id)) {
        ancestors.forEach(a => result.add(a));
      }
      if (children.length) {
        walk(children, [...ancestors, id]);
      }
    }
  }

  walk(nodes || [], []);
  return Array.from(result);
}
