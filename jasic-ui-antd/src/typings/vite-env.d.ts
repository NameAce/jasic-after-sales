/**
 * Namespace Env
 *
 * It is used to declare the type of the import.meta object
 */
declare namespace Env {
  /** The router history mode */
  type RouterHistoryMode = 'hash' | 'history' | 'memory';

  /** Shape of `import.meta.env` (name avoids shadowing global `ImportMeta`) */
  interface ViteEnv extends ImportMetaEnv {
    /** The base url of the application */
    readonly VITE_BASE_URL: string;
    /** The title of the application */
    readonly VITE_APP_TITLE: string;
    /** The description of the application */
    readonly VITE_APP_DESC: string;
    /** The router history mode */
    readonly VITE_ROUTER_HISTORY_MODE?: RouterHistoryMode;
    /** The prefix of the iconify icon */
    readonly VITE_ICON_PREFIX: 'icon';
    /**
     * The prefix of the local icon
     *
     * This prefix is start with the icon prefix
     */
    readonly VITE_ICON_LOCAL_PREFIX: 'local-icon';
    /** backend service base url */
    readonly VITE_SERVICE_BASE_URL: string;
    /**
     * success code of backend service
     *
     * when the code is received, the request is successful
     */
    readonly VITE_SERVICE_SUCCESS_CODE: string;
    /**
     * logout codes of backend service
     *
     * when the code is received, the user will be logged out and redirected to login page
     *
     * use "," to separate multiple codes
     */
    readonly VITE_SERVICE_LOGOUT_CODES: string;
    /**
     * modal logout codes of backend service
     *
     * when the code is received, the user will be logged out by displaying a modal
     *
     * use "," to separate multiple codes
     */
    readonly VITE_SERVICE_MODAL_LOGOUT_CODES: string;
    /**
     * token expired codes of backend service
     *
     * when the code is received, it will refresh the token and resend the request
     *
     * use "," to separate multiple codes
     */
    readonly VITE_SERVICE_EXPIRED_TOKEN_CODES: string;
    /**
     * 是否调用 `POST /auth/refresh-token`（与 jasic-ui / 常见 Sa-Token 网关一致时多为无此接口）。
     * 仅当为 `Y` 且登录响应含 `refreshToken` 时才会请求刷新；否则收到过期码后直接登出，避免误调 404。
     */
    readonly VITE_AUTH_REFRESH_TOKEN?: CommonType.YesOrNo;
    /**
     * Forbidden / no-permission backend codes (comma-separated), e.g. A0200 — aligned with jasic-ui warning behavior
     */
    readonly VITE_SERVICE_FORBIDDEN_CODES?: string;
    /**
     * Server-level backend error codes (comma-separated), will route to 500 page, e.g. A0500
     */
    readonly VITE_SERVICE_SERVER_ERROR_CODES?: string;
    /** Optional Apifox mock token; only sent as apifoxToken header when non-empty */
    readonly VITE_APIFOX_TOKEN?: string;
    /** when the route mode is static, the defined super role */
    readonly VITE_STATIC_SUPER_ROLE: string;
    /**
     * other backend service base url
     *
     * the value is a json
     */
    readonly VITE_OTHER_SERVICE_BASE_URL: string;
    /**
     * Whether to enable the http proxy
     *
     * Only valid in the development environment
     */
    readonly VITE_HTTP_PROXY?: CommonType.YesOrNo;
    /**
     * The auth route mode
     *
     * - Static: the auth routes is generated in front-end
     * - Dynamic: the auth routes is generated in back-end
     */
    readonly VITE_AUTH_ROUTE_MODE: 'static' | 'dynamic';
    /**
     * Whether to translate backend menu title by meta.i18nKey.
     *
     * - "N": use backend meta.title as primary label
     * - "Y": use $t(meta.i18nKey) when i18nKey exists
     */
    readonly VITE_BACKEND_MENU_USE_I18N?: CommonType.YesOrNo;
    /**
     * The home route key
     *
     * It only has effect when the auth route mode is static, if the route mode is dynamic, the home route key is
     * defined in the back-end
     */
    readonly VITE_ROUTE_HOME: import('@elegant-router/types').LastLevelRouteKey;
    /**
     * Default menu icon if menu icon is not set
     *
     * Iconify icon name
     */
    readonly VITE_MENU_ICON: string;
    /**
     * 接口菜单 `icon` 无 Iconify 集合前缀（无 `:`）时自动补全：`{本变量}:{icon}`。
     * Example: `mdi` + API `cog-outline` -> `mdi:cog-outline`.
     */
    readonly VITE_MENU_ICON_API_COLLECTION?: string;
    /** Whether to build with sourcemap */
    readonly VITE_SOURCE_MAP?: CommonType.YesOrNo;
    /** Whether to enable vite-plugin-vue-devtools in local development */
    readonly VITE_ENABLE_VUE_DEVTOOLS?: CommonType.YesOrNo;
    /**
     * Iconify api provider url
     *
     * If the project is deployed in intranet, you can set the api provider url to the local iconify server
     *
     * @link https://docs.iconify.design/api/providers.html
     */
    readonly VITE_ICONIFY_URL?: string;
    /** Used to differentiate storage across different domains */
    readonly VITE_STORAGE_PREFIX?: string;
    /** Whether to automatically detect updates after configuring application packaging */
    readonly VITE_AUTOMATICALLY_DETECT_UPDATE?: CommonType.YesOrNo;
  }
}

interface ImportMeta {
  readonly env: Env.ViteEnv;
}
