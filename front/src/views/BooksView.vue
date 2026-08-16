<template>
  <section class="card stack">
    <div class="section-head books-head">
      <div>
        <p class="eyebrow">Books</p>
        <h3>图书列表</h3>
      </div>
    </div>

    <div class="toolbar">
      <button class="ghost" type="button" :class="{ active: mode === 'list' }" @click="setMode('list')">列表</button>
      <button class="ghost" type="button" :class="{ active: mode === 'search' }" @click="setMode('search')">搜索</button>
      <button class="ghost" type="button" :class="{ active: mode === 'category' }" @click="setMode('category')">分类</button>
    </div>

    <div v-if="mode === 'search'" class="sub-card search-panel">
      <div class="search-form">
        <input v-model="keyword" placeholder="搜索书名或作者" @keyup.enter="searchBooks" />
        <button class="primary" type="button" @click="searchBooks">搜索</button>
        <button class="ghost" type="button" @click="clearSearch">清空</button>
      </div>
    </div>

    <section v-if="mode === 'category'" class="sub-card category-showcase stack">
      <div class="category-hero">
        <div>
          <p class="eyebrow">Category Mall</p>
          <h4>图书分类</h4>
        </div>
        <button class="ghost" type="button" @click="loadCategories">刷新分类</button>
      </div>

      <div v-if="categoryLoading" class="muted">正在加载分类...</div>

      <template v-else>
        <div v-if="rootCategories.length" class="category-shell">
          <aside class="category-sidebar">
            <button
              v-for="item in rootCategories"
              :key="item.id"
              class="category-nav-item"
              :class="{ active: selectedCategory?.id === item.id }"
              type="button"
              @click="selectCategory(item)"
            >
              <span>{{ item.name }}</span>
              <small>{{ item.children?.length || 0 }} 个分区</small>
            </button>
          </aside>

          <section v-if="selectedCategory" class="category-stage">
            <div class="category-stage-head">
              <div>
                <div class="category-breadcrumb">
                  <span>图书分类</span>
                  <span>/</span>
                  <strong>{{ selectedCategory.name }}</strong>
                </div>
                <h4>{{ selectedCategory.name }}</h4>
              </div>
              <button class="primary" type="button" @click="filterByCategory(selectedCategory)">查看全部</button>
            </div>

            <div v-if="featuredChildren.length" class="featured-strip">
              <button
                v-for="item in featuredChildren"
                :key="item.id"
                class="featured-chip"
                type="button"
                @click="openCategory(item)"
              >
                {{ item.name }}
              </button>
            </div>

            <div v-if="childCategories.length" class="subcategory-grid">
              <article v-for="child in childCategories" :key="child.id" class="subcategory-card">
                <div class="subcategory-top">
                  <div>
                    <h5>{{ child.name }}</h5>
                  </div>
                  <button class="ghost" type="button" @click="filterByCategory(child)">进入分类</button>
                </div>

                <div v-if="child.children?.length" class="subcategory-links">
                  <button
                    v-for="leaf in child.children"
                    :key="leaf.id"
                    class="leaf-link"
                    type="button"
                    @click="filterByCategory(leaf)"
                  >
                    {{ leaf.name }}
                  </button>
                </div>
              </article>
            </div>
          </section>
        </div>
        <p v-else class="muted">暂无分类数据。</p>
      </template>
    </section>

    <div v-if="error" class="alert">{{ error }}</div>

    <div class="meta-line" v-if="metaText">{{ metaText }}</div>

    <div v-if="loading" class="muted">正在加载图书...</div>

    <div class="grid cards" v-else>
      <article v-for="book in books" :key="book.id" class="book-card">
        <div class="cover">{{ (book.title || 'BK').slice(0, 2) }}</div>
        <h4>{{ book.title }}</h4>
        <p>{{ book.author || '匿名作者' }}</p>
        <p class="muted" v-if="book.description">{{ book.description }}</p>
        <div class="row">
          <strong>￥{{ book.price }}</strong>
          <button class="ghost" type="button" @click="addToCart(book)">加入购物车</button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { bookApi, cartApi } from '../api/bookmall'
import { getCurrentUser } from '../utils/session'

const books = ref([])
const keyword = ref('')
const error = ref('')
const loading = ref(false)
const categoryLoading = ref(false)
const mode = ref('list')
const metaText = ref('')
const categoryTree = ref([])
const selectedCategory = ref(null)

const rootCategories = computed(() => categoryTree.value)
const childCategories = computed(() => selectedCategory.value?.children || [])
const featuredChildren = computed(() => childCategories.value.slice(0, 6))

function setMode(next) {
  mode.value = next
  error.value = ''

  if (next === 'list') {
    loadBooks()
    return
  }

  if (next === 'search') {
    metaText.value = keyword.value ? `搜索结果：${keyword.value}` : ''
    return
  }

  if (next === 'category') {
    loadCategories()
  }
}

async function loadBooks(params) {
  error.value = ''
  loading.value = true
  try {
    books.value = params ? await bookApi.search(params) : await bookApi.list()
    if (params?.keyword) {
      mode.value = 'search'
      metaText.value = `搜索结果：${params.keyword}`
    } else if (params?.categoryId) {
      mode.value = 'category'
      metaText.value = `分类：${params.categoryName || ''}`
    } else {
      mode.value = 'list'
      metaText.value = '最新图书列表'
    }
  } catch (e) {
    error.value = e.message || '加载图书失败'
  } finally {
    loading.value = false
  }
}

async function searchBooks() {
  if (!keyword.value.trim()) {
    error.value = '请输入搜索关键词'
    return
  }
  await loadBooks({ keyword: keyword.value.trim() })
}

async function clearSearch() {
  keyword.value = ''
  await loadBooks()
}

async function loadCategories() {
  error.value = ''
  categoryLoading.value = true
  try {
    categoryTree.value = await bookApi.categoryTree()
    selectedCategory.value = selectedCategory.value
      ? findCategoryById(categoryTree.value, selectedCategory.value.id) || categoryTree.value[0] || null
      : categoryTree.value[0] || null
    if (!metaText.value.startsWith('分类：')) {
      metaText.value = ''
    }
  } catch (e) {
    error.value = e.message || '加载分类失败'
  } finally {
    categoryLoading.value = false
  }
}

function findCategoryById(list, id) {
  for (const item of list) {
    if (item.id === id) return item
    if (item.children?.length) {
      const matched = findCategoryById(item.children, id)
      if (matched) return matched
    }
  }
  return null
}

function selectCategory(item) {
  selectedCategory.value = item
}

function openCategory(item) {
  if (item.children?.length) {
    selectCategory(item)
    return
  }
  filterByCategory(item)
}

async function filterByCategory(item) {
  selectedCategory.value = item.children?.length ? item : selectedCategory.value
  await loadBooks({ categoryId: item.id, categoryName: item.name })
}

async function addToCart(book) {
  const user = getCurrentUser()
  if (!user?.userId) {
    error.value = '请先登录'
    return
  }

  try {
    await cartApi.add({ userId: user.userId, bookId: book.id, quantity: 1 })
    error.value = ''
    metaText.value = `已加入购物车：${book.title}`
  } catch (e) {
    error.value = e.message || '加入购物车失败'
  }
}

onMounted(loadBooks)
</script>

<style scoped>
.books-head {
  align-items: flex-end;
}

.toolbar {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.toolbar .active {
  background: rgba(180, 83, 9, 0.14);
  border-color: rgba(180, 83, 9, 0.22);
}

.search-panel {
  display: grid;
  gap: 1rem;
}

.search-form {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.search-form input {
  flex: 1;
  min-width: 220px;
}

.meta-line {
  color: var(--muted);
  font-size: 0.95rem;
}

.sub-card {
  padding: 1rem;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.62);
}

.category-showcase {
  gap: 1.1rem;
  padding: 1.25rem;
  background: linear-gradient(180deg, rgba(255, 251, 245, 0.96), rgba(252, 247, 238, 0.92));
}

.category-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.category-hero h4 {
  margin: 0;
  font-size: 1.4rem;
}

.category-shell {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}

.category-sidebar {
  display: grid;
  gap: 0.55rem;
  padding: 0.85rem;
  border-radius: 22px;
  background: rgba(37, 31, 21, 0.96);
}

.category-nav-item {
  text-align: left;
  display: grid;
  gap: 0.2rem;
  padding: 0.9rem 1rem;
  border-radius: 16px;
  border: 1px solid transparent;
  background: transparent;
  color: #f5ebda;
}

.category-nav-item span {
  font-weight: 600;
}

.category-nav-item small {
  color: rgba(245, 235, 218, 0.62);
}

.category-nav-item.active {
  background: linear-gradient(135deg, rgba(180, 83, 9, 0.92), rgba(154, 71, 8, 0.92));
  color: #fff;
}

.category-nav-item.active small {
  color: rgba(255, 255, 255, 0.8);
}

.category-stage {
  display: grid;
  gap: 1rem;
  min-width: 0;
}

.category-stage-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 1.15rem 1.25rem;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(180, 83, 9, 0.12);
}

.category-stage-head h4 {
  margin: 0.2rem 0 0;
  font-size: 1.5rem;
}

.category-breadcrumb {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  color: var(--muted);
  font-size: 0.88rem;
}

.featured-strip {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.featured-chip {
  padding: 0.7rem 1rem;
  border-radius: 999px;
  border: 1px solid rgba(180, 83, 9, 0.16);
  background: rgba(255, 255, 255, 0.92);
  color: var(--accent);
}

.subcategory-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.subcategory-card {
  display: grid;
  gap: 0.9rem;
  padding: 1.1rem;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.92);
}

.subcategory-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.subcategory-top h5 {
  margin: 0;
  font-size: 1rem;
}

.subcategory-links {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.leaf-link {
  padding: 0.5rem 0.8rem;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(180, 83, 9, 0.08);
  color: #8f4107;
}

@media (max-width: 980px) {
  .category-shell {
    grid-template-columns: 1fr;
  }

  .category-sidebar {
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  }
}

@media (max-width: 720px) {
  .search-form {
    flex-direction: column;
  }

  .subcategory-grid {
    grid-template-columns: 1fr;
  }

  .subcategory-top {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
