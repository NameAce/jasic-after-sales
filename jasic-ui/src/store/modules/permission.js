import { getMenus } from '@/api/auth'
import Layout from '@/layout/index.vue'

// 在嵌套路由中，如果中间层级没有指定组件，我们需要用一个能够渲染 router-view 的组件
const ParentView = {
  render(h) {
    return h('router-view')
  }
}

const state = {
  routes: [],
  addRoutes: []
}

const mutations = {
  SET_ROUTES(state, routes) {
    state.addRoutes = routes
    state.routes = constantRoutes.concat(routes)
  }
}

export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    hidden: true
  },
  {
    path: '/choose-company',
    component: () => import('@/views/login/chooseCompany.vue'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    hidden: true
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'el-icon-s-home', affix: true }
      }
    ]
  }
]

const viewModules = {
  'system/user/index': () => import('@/views/system/user/index.vue'),
  'system/role/index': () => import('@/views/system/role/index.vue'),
  'system/menu/index': () => import('@/views/system/menu/index.vue'),
  'system/roleTemplate/index': () => import('@/views/system/roleTemplate/index.vue'),
  'org/companyType/index': () => import('@/views/org/companyType/index.vue'),
  'org/company/index': () => import('@/views/org/company/index.vue'),
  'org/contract/index': () => import('@/views/org/contract/index.vue'),
  'org/region/index': () => import('@/views/org/region/index.vue'),
  'log/operLog/index': () => import('@/views/log/operLog/index.vue')
}

const loadView = (viewPath) => {
  return viewModules[viewPath]
}

function filterAsyncRoutes(menus, parentPath = '') {
  const routes = []
  menus.forEach(menu => {
    // 构造实际的路由路径，处理拼接
    let routePath = menu.path
    if (parentPath && !routePath.startsWith('/')) {
      // 避免重复的/拼接，如果已经是/结尾则不需要加
      routePath = (parentPath.endsWith('/') ? parentPath : parentPath + '/') + routePath
    } else if (!parentPath && !routePath.startsWith('/')) {
      routePath = '/' + routePath
    }

    // 如果没有配置 component，认为是一个目录
    const isMenuFolder = !menu.component
    
    const route = {
      path: menu.path.startsWith('/') && parentPath ? menu.path.replace(/^\//, '') : menu.path, // vue-router: 如果path以/开头，会被当作根路径。在children中我们想要相对路径或直接用绝对路径
    }
    
    // 我们如果提供全绝对路径的话，也可以直接写。但vue-router更喜欢 children 中的相对路径(不以/开头)
    // 为了防止多级菜单path配置混乱，统一用相对路径。如果是根节点，必须加/
    if (!parentPath) {
      route.path = menu.path.startsWith('/') ? menu.path : '/' + menu.path
    } else {
      route.path = menu.path.startsWith('/') ? menu.path.substring(1) : menu.path
    }

    route.name = menu.path.replace(/\//g, '_') || menu.menuName
    route.meta = { title: menu.menuName, icon: menu.icon }
    
    if (isMenuFolder) {
      if (parentPath === '') {
        route.component = Layout
      } else {
        // 多级目录嵌套的情况，中间层级需要一个能渲染 router-view 的组件
        route.component = { render: c => c('router-view') }
      }
    } else {
      const comp = loadView(menu.component)
      if (comp) {
        route.component = comp
      } else {
        console.error(`Component not found for path: ${menu.component}`)
        route.component = { render: (h) => h('div', [`Component not found for path: ${menu.component}`]) }
      }
    }
    
    if (menu.children && menu.children.length > 0) {
      // 在 Vue Router 中，如果我们要将子菜单平铺在 Layout 下面（比如后端返回的 path 都是绝对路径或者相对路径），
      // 我们需要处理 children 的 path。
      route.children = filterChildrenRoutes(menu.children, routePath)
      
      // 添加 redirect
      if (route.children.length > 0) {
        let firstChildPath = route.children[0].path
        route.redirect = routePath === '/' ? `/${firstChildPath}` : `${routePath}/${firstChildPath}`
      }
    }
    routes.push(route)
  })
  return routes
}

function filterChildrenRoutes(children, parentPath) {
  const routes = []
  children.forEach(child => {
    let childPath = child.path
    
    // 如果返回的 path 是以 / 开头，它会被 vue-router 当作根路径解析
    // 我们想在 children 中配置路由，最好不要以 / 开头
    if (childPath && childPath.startsWith('/')) {
      childPath = childPath.substring(1)
    }

    const route = {
      path: childPath,
      name: (parentPath + '/' + childPath).replace(/\//g, '_'),
      meta: { title: child.menuName, icon: child.icon }
    }
    
    // 我们如果是在 Layout 下面显示，那么子组件如果没有 component，就不能使用 Layout 了，应该使用一个空的 router-view
    const isMenuFolder = !child.component
    if (isMenuFolder) {
      route.component = ParentView
    } else {
      const comp = loadView(child.component)
      if (comp) {
        route.component = comp
      } else {
        route.component = { render: (h) => h('div', [`Component not found for path: ${child.component}`]) }
      }
    }

    if (child.children && child.children.length > 0) {
      route.children = filterChildrenRoutes(child.children, parentPath + '/' + childPath)
      if (route.children.length > 0) {
        let firstGrandChildPath = route.children[0].path
        route.redirect = `${parentPath}/${childPath}/${firstGrandChildPath}`
      }
    }

    routes.push(route)
  })
  return routes
}

const actions = {
  generateRoutes({ commit }) {
    return new Promise((resolve, reject) => {
      getMenus().then(res => {
        if (!res) return reject(new Error('请求失败'))
        const menus = res.data || []
        // 直接处理路由组件加载
        const asyncRoutes = filterAsyncRoutes(menus)
        // 这个 404 放最后
        asyncRoutes.push({ path: '*', redirect: '/404', hidden: true })
        
        console.log('生成的动态路由:', asyncRoutes)
        
        commit('SET_ROUTES', asyncRoutes)
        resolve(asyncRoutes)
      }).catch(error => reject(error))
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
