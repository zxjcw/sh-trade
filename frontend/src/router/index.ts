import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/types'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/layouts/StudentLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/item/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'search',
        name: 'search',
        component: () => import('@/views/item/Search.vue'),
        meta: { title: '搜索物品' }
      },
      {
        path: 'items/:id',
        name: 'item-detail',
        component: () => import('@/views/item/Detail.vue'),
        meta: { title: '物品详情' }
      },
      {
        path: 'publish',
        name: 'publish',
        component: () => import('@/views/item/Publish.vue'),
        meta: { title: '发布物品', requiresAuth: true, requiresVerified: true }
      },
      {
        path: 'my-items',
        name: 'my-items',
        component: () => import('@/views/item/MyItems.vue'),
        meta: { title: '我的物品', requiresAuth: true }
      },
      {
        path: 'checkout/:itemId',
        name: 'checkout',
        component: () => import('@/views/trade/Checkout.vue'),
        meta: { title: '确认订单', requiresAuth: true, requiresVerified: true }
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/trade/Orders.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'orders/:id',
        name: 'order-detail',
        component: () => import('@/views/trade/OrderDetail.vue'),
        meta: { title: '订单详情', requiresAuth: true }
      },
      {
        path: 'aftersale/apply/:orderId',
        name: 'aftersale-apply',
        component: () => import('@/views/aftersale/Apply.vue'),
        meta: { title: '申请售后', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'wallet',
        name: 'wallet',
        component: () => import('@/views/profile/Wallet.vue'),
        meta: { title: '我的钱包', requiresAuth: true }
      },
      {
        path: 'notifications',
        name: 'notifications',
        component: () => import('@/views/notification/List.vue'),
        meta: { title: '消息通知', requiresAuth: true }
      },
      {
        path: 'verify',
        name: 'verify',
        component: () => import('@/views/auth/Verify.vue'),
        meta: { title: '学籍认证', requiresAuth: true }
      },
      {
        path: 'bounty',
        name: 'bounty-hall',
        component: () => import('@/views/bounty/Hall.vue'),
        meta: { title: '悬赏大厅' }
      },
      {
        path: 'bounty/publish',
        name: 'bounty-publish',
        component: () => import('@/views/bounty/Publish.vue'),
        meta: { title: '发布求购', requiresAuth: true, requiresVerified: true }
      },
      {
        path: 'bounty/mine',
        name: 'bounty-mine',
        component: () => import('@/views/bounty/MyBounties.vue'),
        meta: { title: '我的悬赏', requiresAuth: true }
      },
      {
        path: 'exchange',
        name: 'exchange',
        component: () => import('@/views/exchange/Intents.vue'),
        meta: { title: '物品置换', requiresAuth: true }
      }
    ]
  },
  {
    path: '/ops',
    component: () => import('@/layouts/ConsoleLayout.vue'),
    redirect: '/ops/reviews',
    meta: { roles: ['operator', 'admin'] as Role[], requiresAuth: true },
    children: [
      {
        path: 'reviews',
        name: 'ops-reviews',
        component: () => import('@/views/ops/ReviewList.vue'),
        meta: { title: '物品审核', console: 'ops' }
      },
      {
        path: 'arbitrations',
        name: 'ops-arbitrations',
        component: () => import('@/views/ops/ArbitrationList.vue'),
        meta: { title: '仲裁处理', console: 'ops' }
      },
      {
        path: 'report',
        name: 'ops-report',
        component: () => import('@/views/ops/Report.vue'),
        meta: { title: '运营报表', console: 'ops' }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/ConsoleLayout.vue'),
    redirect: '/admin/categories',
    meta: { roles: ['admin'] as Role[], requiresAuth: true },
    children: [
      {
        path: 'roster',
        name: 'admin-roster',
        component: () => import('@/views/admin/RosterImport.vue'),
        meta: { title: '学籍管理', console: 'admin' }
      },
      {
        path: 'categories',
        name: 'admin-categories',
        component: () => import('@/views/admin/Categories.vue'),
        meta: { title: '分类管理', console: 'admin' }
      },
      {
        path: 'pickup-points',
        name: 'admin-pickup-points',
        component: () => import('@/views/admin/PickupPoints.vue'),
        meta: { title: '自提点管理', console: 'admin' }
      },
      {
        path: 'operators',
        name: 'admin-operators',
        component: () => import('@/views/admin/Operators.vue'),
        meta: { title: '运营账号', console: 'admin' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  document.title = `${String(to.meta.title ?? '拾光集')} · 拾光集`

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const roles = to.meta.roles as Role[] | undefined
  if (roles && !roles.includes(auth.role)) {
    return { name: 'home' }
  }

  if (to.meta.requiresVerified && !auth.isVerified) {
    return { name: 'verify', query: { redirect: to.fullPath } }
  }

  if ((to.name === 'login' || to.name === 'register') && auth.isLoggedIn) {
    return { name: 'home' }
  }

  return true
})

export default router
