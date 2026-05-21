/** The storage namespace */
declare namespace StorageType {
  interface Session {
    /** The theme color */
    themeColor: string;
    // /**
    //  * the theme settings
    //  */
    // themeSettings: App.Theme.ThemeSetting;
  }

  interface Local {
    /** The i18n language */
    lang: App.I18n.LangType;
    /** The token */
    token: string;
    /** Fixed sider with mix-menu */
    mixSiderFixed: CommonType.YesOrNo;
    /** The refresh token */
    refreshToken: string;
    /** The theme color */
    themeColor: string;
    /** The theme settings（运行时按用户+角色分区写入 `themeSettings__scope__*`，全局键仅作旧版兼容） */
    themeSettings: App.Theme.ThemeSetting;
    /** 最近一次 hydrate 的主题分区（用户+角色），供首屏 loading 在 Pinia 就绪前读取主色 */
    themeActiveScopeId: string;
    /**
     * The override theme flags
     *
     * The value is the build time of the project
     */
    overrideThemeFlag: string;
    /** The global tabs */
    globalTabs: App.Global.Tab[];
    /** The backup theme setting before is mobile */
    backupThemeSettingBeforeIsMobile: {
      layout: UnionKey.ThemeLayoutMode;
      siderCollapse: boolean;
    };
  }
}
