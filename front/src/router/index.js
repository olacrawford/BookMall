import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import BooksView from '../views/BooksView.vue'
import CartView from '../views/CartView.vue'
import OrdersView from '../views/OrdersView.vue'
import AddressView from '../views/AddressView.vue'
import AfterSaleView from '../views/AfterSaleView.vue'
import { isLoggedIn } from '../utils/session'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginView },
  { path: '/home', component: HomeView, meta: { auth: true } },
  { path: '/books', component: BooksView, meta: { auth: true } },
  { path: '/cart', component: CartView, meta: { auth: true } },
  { path: '/orders', component: OrdersView, meta: { auth: true } },
  { path: '/after-sales', component: AfterSaleView, meta: { auth: true } },
  { path: '/addresses', component: AddressView, meta: { auth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  if (to.meta.auth && !isLoggedIn()) {
    return '/login'
  }
  if (to.path === '/login' && isLoggedIn()) {
    return '/home'
  }
  return true
})

export default router
