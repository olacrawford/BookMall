<template>
  <section class="after-sale-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">After-Sale Console</p>
        <h3>售后中心</h3>
        <p class="muted">创建售后工单，查看状态、AI 证据和审批待办。</p>
      </div>
      <div class="head-actions">
        <span class="status-pill">{{ isApprover ? '审批角色' : '用户角色' }}</span>
        <button class="ghost" type="button" :disabled="loading" @click="refreshAll">刷新</button>
      </div>
    </div>

    <div v-if="error" class="alert">{{ error }}</div>
    <div v-if="notice" class="notice">{{ notice }}</div>

    <div class="workspace-grid">
      <section class="panel create-panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">New Ticket</p>
            <h4>创建售后单</h4>
          </div>
        </div>

        <form class="form-stack" @submit.prevent="submitAfterSale">
          <label>
            <span>订单 ID</span>
            <input
              v-model.number="form.orderId"
              type="number"
              min="1"
              list="after-sale-orders"
              placeholder="选择或输入订单 ID"
            />
            <datalist id="after-sale-orders">
              <option v-for="order in orders" :key="order.id" :value="order.id">
                {{ order.orderNo }} · ￥{{ order.totalAmount }}
              </option>
            </datalist>
          </label>

          <label>
            <span>售后类型</span>
            <select v-model="form.type">
              <option value="LOGISTICS_NOT_RECEIVED">物流未收到</option>
              <option value="DAMAGED">商品损坏</option>
              <option value="MISSING_ITEM">少件缺货</option>
              <option value="REFUND_REQUEST">退款请求</option>
              <option value="GENERAL_INQUIRY">一般咨询</option>
            </select>
          </label>

          <label>
            <span>问题描述</span>
            <textarea v-model="form.description" rows="3" placeholder="描述发生了什么问题"></textarea>
          </label>

          <label>
            <span>证据（一行一条，或逗号分隔）</span>
            <textarea v-model="form.evidenceText" rows="2" placeholder="例如：驿站无包裹，门卫未签收"></textarea>
          </label>

          <label>
            <span>期望处理</span>
            <select v-model="form.requestedAction">
              <option value="REFUND">退款</option>
              <option value="REPLACE">换货</option>
              <option value="RETURN">退货</option>
              <option value="OTHER">其他</option>
            </select>
          </label>

          <button class="primary full" type="submit" :disabled="submitting">
            {{ submitting ? '提交中...' : '提交售后单' }}
          </button>
        </form>
      </section>

      <section class="panel list-panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">My Tickets</p>
            <h4>我的售后单</h4>
          </div>
          <span class="count-pill">{{ afterSales.length }}</span>
        </div>

        <div v-if="loading" class="muted">正在同步售后数据...</div>
        <p v-else-if="!afterSales.length" class="muted">暂无售后单，可先创建一个。</p>

        <div v-else class="ticket-list">
          <article v-for="item in afterSales" :key="item.afterSaleId" class="ticket-row">
            <div class="ticket-main">
              <strong>#{{ item.afterSaleId }}</strong>
              <span class="muted">{{ item.afterSaleNo }}</span>
            </div>
            <div class="ticket-meta">
              <span>订单 {{ item.orderId }}</span>
              <span>￥{{ item.amount }}</span>
              <span :class="['state', `state-${normalizeState(item.status)}`]">{{ statusText(item.status) }}</span>
            </div>
            <button class="ghost" type="button" @click="loadDetail(item.afterSaleId)">详情</button>
          </article>
        </div>
      </section>
    </div>

    <section v-if="detail" class="panel detail-panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Detail</p>
          <h4>售后单 #{{ detail.afterSaleId }}</h4>
        </div>
        <div class="row-gap">
          <button class="ghost" type="button" @click="loadAnalysis(detail.afterSaleId)">查看 AI 分析</button>
        </div>
      </div>

      <div class="detail-grid">
        <div class="metric">
          <span>状态</span>
          <strong>{{ statusText(detail.status) }}</strong>
        </div>
        <div class="metric">
          <span>流程</span>
          <strong>{{ detail.workflowStatus || '未启动' }}</strong>
        </div>
        <div class="metric">
          <span>策略动作</span>
          <strong>{{ detail.policyAction || detail.decisionStatus || '等待判定' }}</strong>
        </div>
        <div class="metric">
          <span>金额</span>
          <strong>￥{{ detail.amount }}</strong>
        </div>
        <div class="metric">
          <span>创建时间</span>
          <strong>{{ formatTime(detail.createTime) }}</strong>
        </div>
        <div class="metric">
          <span>工单</span>
          <strong>{{ detail.ticketNo || detail.ticketId }}</strong>
        </div>
      </div>
    </section>

    <section v-if="analysis" class="panel analysis-panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">AI Evidence</p>
          <h4>AI 分析证据</h4>
        </div>
        <span class="status-pill">{{ analysis.validationStatus }}</span>
      </div>

      <div class="metric-row">
        <div class="metric">
          <span>意图</span>
          <strong>{{ analysis.intent }}</strong>
        </div>
        <div class="metric">
          <span>建议动作</span>
          <strong>{{ analysis.decision?.action }}</strong>
        </div>
        <div class="metric">
          <span>风险等级</span>
          <strong>{{ analysis.decision?.riskLevel }}</strong>
        </div>
        <div class="metric">
          <span>规则版本</span>
          <strong>{{ analysis.decision?.policyVersion }}</strong>
        </div>
      </div>

      <p class="muted">{{ analysis.decision?.reason }}</p>

      <div v-if="analysis.toolResults?.length" class="evidence-grid">
        <article v-for="tool in analysis.toolResults" :key="tool.toolName" class="evidence-item">
          <strong>{{ tool.toolName }}</strong>
          <span :class="tool.success ? 'ok' : 'bad'">{{ tool.success ? '成功' : tool.errorCode }}</span>
        </article>
      </div>

      <div v-if="analysis.ruleHits?.length" class="rule-list">
        <article v-for="(rule, index) in analysis.ruleHits" :key="`${rule.documentCode}-${index}`">
          <strong>{{ rule.documentCode }} v{{ rule.policyVersion }}</strong>
          <p class="muted">{{ rule.content }}</p>
        </article>
      </div>
    </section>

    <section v-if="isApprover" class="panel approval-panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Approval Queue</p>
          <h4>待审批任务</h4>
        </div>
        <button class="ghost" type="button" @click="loadApprovals">刷新待办</button>
      </div>

      <div v-if="approvalLoading" class="muted">正在加载审批队列...</div>
      <p v-else-if="!approvalTasks.length" class="muted">当前没有可处理的审批任务。</p>

      <div v-else class="ticket-list">
        <article v-for="task in approvalTasks" :key="task.id" class="approval-row">
          <div class="ticket-main">
            <strong>任务 #{{ task.id }}</strong>
            <span class="muted">售后单 #{{ task.afterSaleId }} · 订单 #{{ task.orderId }}</span>
          </div>
          <div class="approval-actions">
            <input v-model="task.reviewComment" placeholder="审批备注" />
            <button class="primary" type="button" @click="decideTask(task, 'approve')">批准</button>
            <button class="ghost danger" type="button" @click="decideTask(task, 'reject')">驳回</button>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { afterSaleApi, orderApi } from '../api/bookmall'
import { getCurrentUser } from '../utils/session'

const currentUser = computed(() => getCurrentUser())
const isApprover = computed(() => currentUser.value?.role === 'APPROVER' || currentUser.value?.role === 'ADMIN')

const afterSales = ref([])
const orders = ref([])
const detail = ref(null)
const analysis = ref(null)
const approvalTasks = ref([])
const error = ref('')
const notice = ref('')
const loading = ref(false)
const submitting = ref(false)
const approvalLoading = ref(false)

const form = reactive({
  orderId: null,
  type: 'LOGISTICS_NOT_RECEIVED',
  description: '',
  evidenceText: '',
  requestedAction: 'REFUND'
})

function normalizeState(status) {
  return String(status || '').toLowerCase()
}

function statusText(status) {
  const map = {
    UNDER_REVIEW: '审核中',
    WAITING_APPROVAL: '待审批',
    WAITING_HUMAN: '待人工',
    RISK_REVIEW: '风控审核',
    PROCESSING: '处理中',
    AUTO_HANDLED: '自动处理',
    COMPLETED: '已完成',
    REJECTED: '已驳回',
    CLOSED: '已关闭'
  }
  return map[status] || status || '未知'
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function makeIdempotencyKey() {
  return `WEB-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

async function refreshAll() {
  error.value = ''
  notice.value = ''
  loading.value = true
  try {
    await Promise.allSettled([loadAfterSales(), loadOrders(), isApprover.value ? loadApprovals() : Promise.resolve()])
  } finally {
    loading.value = false
  }
}

async function loadAfterSales() {
  try {
    afterSales.value = await afterSaleApi.list()
  } catch (e) {
    error.value = e.message || '售后单加载失败'
  }
}

async function loadOrders() {
  try {
    orders.value = await orderApi.list()
  } catch {
    orders.value = []
  }
}

async function loadDetail(id) {
  error.value = ''
  notice.value = ''
  detail.value = null
  analysis.value = null
  try {
    detail.value = await afterSaleApi.detail(id)
  } catch (e) {
    error.value = e.message || '售后详情加载失败'
  }
}

async function loadAnalysis(id) {
  error.value = ''
  try {
    analysis.value = await afterSaleApi.analysis(id)
  } catch (e) {
    error.value = e.message || 'AI 分析加载失败'
  }
}

async function submitAfterSale() {
  error.value = ''
  notice.value = ''
  if (!form.orderId) {
    error.value = '请先选择或输入订单 ID'
    return
  }
  if (!form.description.trim()) {
    error.value = '请填写问题描述'
    return
  }

  const evidence = form.evidenceText
    .split(/[\n,，]+/)
    .map((item) => item.trim())
    .filter(Boolean)

  submitting.value = true
  try {
    const created = await afterSaleApi.create(
      {
        orderId: form.orderId,
        type: form.type,
        description: form.description.trim(),
        evidence,
        requestedAction: form.requestedAction
      },
      makeIdempotencyKey()
    )
    notice.value = `售后单 #${created.afterSaleId || created.id} 已创建`
    form.orderId = null
    form.description = ''
    form.evidenceText = ''
    detail.value = null
    analysis.value = null
    await Promise.allSettled([loadAfterSales(), isApprover.value ? loadApprovals() : Promise.resolve()])
  } catch (e) {
    error.value = e.message || '售后单创建失败'
  } finally {
    submitting.value = false
  }
}

async function loadApprovals() {
  approvalLoading.value = true
  error.value = ''
  try {
    const queue = await afterSaleApi.approvalQueue('WAITING')
    const items = Array.isArray(queue?.items) ? queue.items : []
    approvalTasks.value = items
      .filter((task) => task.afterSaleId || task.orderId || task.userId)
      .slice(0, 50)
  } catch (e) {
    if (e.code !== 403) {
      error.value = e.message || '审批队列加载失败'
    }
  } finally {
    approvalLoading.value = false
  }
}

async function decideTask(task, decision) {
  error.value = ''
  const comment = task.reviewComment || (decision === 'approve' ? '前端批准' : '前端驳回')
  try {
    if (decision === 'approve') {
      await afterSaleApi.approve(task.id, comment)
    } else {
      await afterSaleApi.reject(task.id, comment)
    }
    notice.value = `审批任务 #${task.id} 已${decision === 'approve' ? '批准' : '驳回'}`
    await Promise.allSettled([loadApprovals(), loadAfterSales()])
  } catch (e) {
    error.value = e.message || '审批操作失败'
  }
}

onMounted(refreshAll)
</script>

<style scoped>
.after-sale-page {
  display: grid;
  gap: 1rem;
  max-width: 1280px;
}

.page-head,
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.page-head h3,
.panel-head h4 {
  margin: 0;
}

.head-actions,
.row-gap {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.status-pill,
.count-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  min-height: 34px;
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  font-size: 0.82rem;
  background: rgba(15, 118, 110, 0.1);
  border: 1px solid rgba(15, 118, 110, 0.25);
  color: #0f766e;
}

.count-pill {
  min-width: 34px;
  color: #4338ca;
  background: rgba(67, 56, 202, 0.08);
  border-color: rgba(67, 56, 202, 0.18);
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(300px, 0.85fr) minmax(460px, 1.15fr);
  gap: 1rem;
  align-items: start;
}

.panel {
  min-width: 0;
  padding: 1.1rem;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 253, 247, 0.9);
  box-shadow: var(--shadow);
}

.form-stack {
  display: grid;
  gap: 0.85rem;
  margin-top: 1rem;
}

label {
  display: grid;
  gap: 0.35rem;
  font-size: 0.9rem;
  color: var(--ink);
}

input,
select,
textarea {
  width: 100%;
  min-height: 40px;
  padding: 0.55rem 0.7rem;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
  outline: none;
}

textarea {
  resize: vertical;
  min-height: 64px;
}

input:focus,
select:focus,
textarea:focus {
  border-color: rgba(15, 118, 110, 0.45);
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.08);
}

.ticket-list {
  display: grid;
  gap: 0.6rem;
  margin-top: 0.9rem;
  max-height: 560px;
  overflow: auto;
  padding-right: 0.15rem;
}

.ticket-row,
.approval-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  padding: 0.85rem;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(245, 239, 227, 0.42);
}

.ticket-main {
  display: grid;
  gap: 0.1rem;
  min-width: 0;
}

.ticket-meta {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
  font-size: 0.85rem;
  color: var(--muted);
}

.state {
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  font-size: 0.78rem;
  background: rgba(67, 56, 202, 0.08);
  color: #4338ca;
}

.state-completed {
  background: rgba(21, 128, 61, 0.1);
  color: #15803d;
}

.state-waiting_approval,
.state-risk_review,
.state-waiting_human {
  background: rgba(180, 83, 9, 0.1);
  color: #b45309;
}

.detail-grid,
.metric-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.7rem;
  margin-top: 1rem;
}

.metric {
  display: grid;
  gap: 0.2rem;
  padding: 0.8rem;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: rgba(245, 239, 227, 0.42);
}

.metric span {
  color: var(--muted);
  font-size: 0.78rem;
}

.metric strong {
  font-size: 0.95rem;
}

.evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.6rem;
  margin-top: 1rem;
}

.evidence-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
  padding: 0.7rem;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: rgba(245, 239, 227, 0.42);
}

.evidence-item .ok {
  color: #15803d;
}

.evidence-item .bad {
  color: #b91c1c;
}

.rule-list {
  display: grid;
  gap: 0.6rem;
  margin-top: 1rem;
}

.rule-list article {
  padding: 0.8rem;
  border: 1px solid var(--line);
  border-left: 3px solid rgba(15, 118, 110, 0.6);
  border-radius: 10px;
  background: rgba(245, 239, 227, 0.42);
}

.rule-list p {
  margin: 0.25rem 0 0;
  font-size: 0.88rem;
}

.approval-actions {
  display: grid;
  grid-template-columns: minmax(160px, 1.4fr) auto auto;
  gap: 0.55rem;
  align-items: center;
}

.approval-actions input {
  min-height: 38px;
}

.danger {
  color: #b91c1c;
}

@media (max-width: 900px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .approval-actions {
    grid-template-columns: 1fr;
    width: 100%;
  }

  .ticket-row,
  .approval-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
