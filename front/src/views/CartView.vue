<template>
  <section class="card stack">
    <div class="section-head">
      <div>
        <p class="eyebrow">Cart</p>
        <h3>购物车</h3>
      </div>
      <button class="ghost" type="button" @click="loadCart">刷新</button>
    </div>

    <div v-if="error" class="alert">{{ error }}</div>
    <div v-if="notice" class="notice">{{ notice }}</div>
    <div v-if="loading" class="muted">正在同步购物车数据...</div>

    <template v-else>
      <div v-if="items.length" class="list-stack">
        <article v-for="item in items" :key="item.id" class="list-card">
          <div>
            <strong>{{ item.bookTitle || `图书 #${item.bookId}` }}</strong>
            <p class="muted">
              <template v-if="item.bookPrice">单价 ￥{{ item.bookPrice }} · </template>
              数量 {{ item.quantity }}
            </p>
          </div>
          <div class="row">
            <button class="ghost" type="button" @click="changeQty(item, Math.max(1, item.quantity - 1))">-</button>
            <span>{{ item.quantity }}</span>
            <button class="ghost" type="button" @click="changeQty(item, item.quantity + 1)">+</button>
            <button class="ghost" type="button" @click="removeItem(item)">移除</button>
          </div>
        </article>
      </div>
      <p v-else class="muted">购物车还没有商品，去图书中心挑几本加入吧。</p>

      <div v-if="items.length" class="sub-card checkout-bar">
        <label>
          <span>收货地址</span>
          <select v-model="addressId">
            <option :value="null" disabled>请选择收货地址</option>
            <option v-for="addr in addresses" :key="addr.id" :value="addr.id">
              {{ addr.receiverName }} · {{ formatAddress(addr) }}
            </option>
          </select>
        </label>
        <button class="primary" type="button" :disabled="submitting" @click="checkout">
          {{ submitting ? '提交订单中...' : '结算下单' }}
        </button>
      </div>
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { cartApi, addressApi, orderApi, bookApi } from '../api/bookmall'
import { getCurrentUser } from '../utils/session'

const items = ref([])
const addresses = ref([])
const addressId = ref(null)
const error = ref('')
const notice = ref('')
const loading = ref(false)
const submitting = ref(false)

function formatAddress(addr) {
  return [addr.province, addr.city, addr.district, addr.detailAddress].filter(Boolean).join('')
}

async function loadCart() {
  const user = getCurrentUser()
  if (!user?.userId) {
    error.value = '请先登录后再查看购物车'
    return
  }

  loading.value = true
  error.value = ''
  try {
    const raw = await cartApi.list(user.userId)
    // 购物车接口只返回 bookId，这里并查图书标题/价格用于展示。
    items.value = await Promise.all(raw.map(async (item) => {
      try {
        const book = await bookApi.detail(item.bookId)
        return { ...item, bookTitle: book?.title, bookPrice: book?.price }
      } catch {
        return item
      }
    }))
  } catch (e) {
    error.value = e.message || '购物车加载失败'
  } finally {
    loading.value = false
  }
}

async function loadAddresses() {
  const user = getCurrentUser()
  if (!user?.userId) return
  try {
    addresses.value = await addressApi.list(user.userId)
    const def = addresses.value.find((a) => a.isDefault === 1) || addresses.value[0]
    addressId.value = def?.id ?? null
  } catch (e) {
    error.value = e.message || '收货地址加载失败'
  }
}

async function changeQty(item, quantity) {
  try {
    await cartApi.update(item.id, quantity)
    await loadCart()
  } catch (e) {
    error.value = e.message || '修改购物车数量失败'
  }
}

async function removeItem(item) {
  try {
    await cartApi.remove(item.id)
    await loadCart()
  } catch (e) {
    error.value = e.message || '移除购物车商品失败'
  }
}

async function checkout() {
  const user = getCurrentUser()
  if (!user?.userId) {
    error.value = '请先登录后再结算'
    return
  }
  if (!addressId.value) {
    error.value = '请先选择收货地址'
    return
  }
  if (!items.value.length) {
    error.value = '购物车为空，不能下单'
    return
  }

  submitting.value = true
  error.value = ''
  notice.value = ''
  try {
    await orderApi.create({
      userId: user.userId,
      addressId: addressId.value,
      cartItemIds: items.value.map((i) => i.id)
    })
    notice.value = '订单已提交，可以前往订单中心查看'
    await loadCart()
  } catch (e) {
    error.value = e.message || '订单提交失败'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCart()
  loadAddresses()
})
</script>
