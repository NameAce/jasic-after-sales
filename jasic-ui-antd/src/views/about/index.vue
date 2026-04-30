<script setup lang="ts">
/**
 * 关于页：展示 package.json 中的项目名称、版本、生产/开发依赖表格及可选项目链接。
 */
import { $t } from '@/locales';
import pkg from '~/package.json';

/** package.json 在类型上未必声明 homepage/website，展示链接时单独断言 */
type PackageJsonWithLinks = typeof pkg & { homepage?: string; website?: string };
const pkgMeta = pkg as PackageJsonWithLinks;

interface PkgJson {
  name: string;
  version: string;
  dependencies: PkgVersionInfo[];
  devDependencies: PkgVersionInfo[];
}

interface PkgVersionInfo {
  name: string;
  version: string;
}

// package.json 解构字段
const { name, version, dependencies, devDependencies } = pkgMeta;

/**
 * 作用：将 Object.entries 的单项转为依赖展示结构。
 * @param tuple - [包名, 版本号]
 * @returns PkgVersionInfo
 */
function transformVersionData(tuple: [string, string]): PkgVersionInfo {
  const [$name, $version] = tuple;
  return {
    name: $name,
    version: $version
  };
}

// 用于模板展示的 package.json 结构化副本
const pkgJson: PkgJson = {
  name,
  version,
  dependencies: Object.entries(dependencies).map(item => transformVersionData(item)),
  devDependencies: Object.entries(devDependencies).map(item => transformVersionData(item))
};

// 构建时注入的构建时间字符串
const latestBuildTime = BUILD_TIME;
</script>

<template>
  <ASpace direction="vertical" :size="16">
    <ACard :title="$t('page.about.title')" :bordered="false" size="small" class="card-wrapper">
      <p>{{ $t('page.about.introduction') }}</p>
    </ACard>
    <ACard :title="$t('page.about.projectInfo.title')" :bordered="false" size="small" class="card-wrapper">
      <ADescriptions label-placement="left" bordered size="small" :column="{ xs: 1, sm: 2 }">
        <ADescriptionsItem :label="$t('page.about.projectInfo.version')">
          <ATag color="blue">{{ pkgJson.version }}</ATag>
        </ADescriptionsItem>
        <ADescriptionsItem :label="$t('page.about.projectInfo.latestBuildTime')">
          <ATag color="blue">{{ latestBuildTime }}</ATag>
        </ADescriptionsItem>
        <ADescriptionsItem :label="$t('page.about.projectInfo.githubLink')">
          <a class="text-primary" :href="pkgMeta.homepage" target="_blank" rel="noopener noreferrer">
            {{ $t('page.about.projectInfo.githubLink') }}
          </a>
        </ADescriptionsItem>
        <ADescriptionsItem :label="$t('page.about.projectInfo.previewLink')">
          <a class="text-primary" :href="pkgMeta.website" target="_blank" rel="noopener noreferrer">
            {{ $t('page.about.projectInfo.previewLink') }}
          </a>
        </ADescriptionsItem>
      </ADescriptions>
    </ACard>
    <ACard :title="$t('page.about.prdDep')" :bordered="false" size="small" class="card-wrapper">
      <ADescriptions label-placement="left" bordered size="small" :column="{ xs: 1, sm: 2 }">
        <ADescriptionsItem v-for="item in pkgJson.dependencies" :key="item.name" :label="item.name">
          {{ item.version }}
        </ADescriptionsItem>
      </ADescriptions>
    </ACard>
    <ACard :title="$t('page.about.devDep')" :bordered="false" size="small" class="card-wrapper">
      <ADescriptions label-placement="left" bordered size="small" :column="{ xs: 1, sm: 2 }">
        <ADescriptionsItem v-for="item in pkgJson.devDependencies" :key="item.name" :label="item.name">
          {{ item.version }}
        </ADescriptionsItem>
      </ADescriptions>
    </ACard>
  </ASpace>
</template>

<style scoped></style>
