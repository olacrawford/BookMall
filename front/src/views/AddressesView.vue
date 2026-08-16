<template>
  <section class="card stack">
    <div class="section-head">
      <div>
        <p class="eyebrow">Addresses</p>
        <h3>收货地址</h3>
      </div>
      <button class="ghost" type="button" @click="loadAddresses">刷新</button>
    </div>

    <form class="form-grid address-form" @submit.prevent="createAddress">
      <label>
        <span>收货人</span>
        <input v-model="form.receiverName" placeholder="收货人姓名" />
      </label>
      <label>
        <span>手机号</span>
        <input v-model="form.receiverPhone" placeholder="手机号" />
      </label>
      <label>
        <span>省份</span>
        <input v-model="form.province" placeholder="省" />
      </label>
      <label>
        <span>城市</span>
        <input v-model="form.city" placeholder="市" />
      </label>
      <label>
        <span>区县</span>
        <input v-model="form.district" placeholder="区/县" />
      </label>
      <label>
        <span>详细地址</span>
        <input v-model="form.detailAddress" placeholder="详细地址" />
      </label>
      <button class="primary full" type="submit">新增地址</button>
    </form>

    <div v-if="error" class="alert">{{ error }}</div>
    <div v-if="loading" class="muted">正在同步地址数据...</div>

    <div v-if="addresses.length" class="list-stack">
      <article v-for="addr in addresses" :key="addr.id" class="list-card">
        <div>
          <strong>
            {{ addr.receiverName }}
            <span v-if="addr.isDefault === 1" class="chip">默认</span>
          </strong>
          <p class="muted">{{ addr.receiverPhone }}</p>
          <p>{{ formatAddress(addr) }}</p>
        </div>
        <div class="row">
          <button class="ghost" type="button" @click="setDefault(addr.id)">设为默认</button>
          <button class="ghost" type="button" @click="removeAddress(addr.id)">删除</button>
        </div>
      </article>
    </div>
    <p v-else class="muted">还没有地址，先新增一个收货地址吧。</p>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { addressApi } from '../api/bookmall'
import { getCurrentUser } from '../utils/session'

const addresses = ref([])
const error = ref('')
const loading = ref(false)
const form = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: ''
})

function formatAddress(addr) {
  return [addr.province, addr.city, addr.district, addr.detailAddress].filter(Boolean).join('')
}

async function loadAddresses() {
  const user = getCurrentUser()
  if (!user?.userId) {
    error.value = '请先登录后再管理地址'
    return
  }

  loading.value = true
  error.value = ''
  try {
    addresses.value = await addressApi.list(user.userId)
  } catch (e) {
    error.value = e.message || '地址加载失败'
  } finally {
    loading.value = false
  }
}

async function createAddress() {
  const user = getCurrentUser()
  if (!user?.userId) {
    error.value = '请先登录后再新增地址'
    return
  }
  try {
    await addressApi.create({ ...form, userId: user.userId, isDefault: 0 })
    Object.assign(form, {
      receiverName: '',
      receiverPhone: '',
      province: '',
      city: '',
      district: '',
      detailAddress: ''
    })
    await loadAddresses()
  } catch (e) {
    error.value = e.message || '新增地址失败'
  }
}

async function setDefault(id) {
  try {
    await addressApi.setDefault(id)
    await loadAddresses()
  } catch (e) {
    error.value = e.message || '设置默认地址失败'
  }
}

async function removeAddress(id) {
  try {
    await addressApi.remove(id)
    await loadAddresses()
  } catch (e) {
    error.value = e.message || '删除地址失败'
  }
}

onMounted(loadAddresses)
</script>

<style scoped>
.address-form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.address-form button {
  grid-column: 1 / -1;
}

.list-stack {
  display: grid;
  gap: 0.75rem;
}

.list-card {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 1rem;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.6);
}
</style>
