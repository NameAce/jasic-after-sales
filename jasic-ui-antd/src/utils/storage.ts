/**
 * 浏览器存储封装：带环境前缀的 localStorage / sessionStorage 与 IndexedDB（localforage）。
 */
import { createLocalforage, createStorage } from '@sa/utils';

// 本地存储 key 前缀，避免多环境共用时冲突
const storagePrefix = import.meta.env.VITE_STORAGE_PREFIX || '';

/** 封装 localStorage 的键值读写（带项目前缀） */
export const localStg = createStorage<StorageType.Local>('local', storagePrefix);

/** 封装 sessionStorage 的键值读写（带项目前缀） */
export const sessionStg = createStorage<StorageType.Session>('session', storagePrefix);

/** 基于 IndexedDB 的大对象离线存储 */
export const localforage = createLocalforage<StorageType.Local>('local');
