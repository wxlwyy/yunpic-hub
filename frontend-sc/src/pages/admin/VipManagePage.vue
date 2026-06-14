<template>
  <div id="vipManagePage">
    <a-card :bordered="false" class="main-card">
      <a-tabs v-model:activeKey="activeTab">
        <!-- Tab 1: 兑换码管理 -->
        <a-tab-pane key="code" tab="兑换码管理">
          <div style="margin-bottom: 16px">
            <a-button type="primary" @click="showGenerateModal">
              <template #icon><PlusOutlined /></template>
              生成兑换码
            </a-button>
          </div>

          <!-- 搜索区 -->
          <a-card class="search-card" :bordered="false">
            <a-form layout="inline" :model="codeSearchParams" @finish="searchCodes">
              <a-form-item label="兑换码">
                <a-input v-model:value="codeSearchParams.code" placeholder="输入兑换码" allow-clear class="modern-input" />
              </a-form-item>
              <a-form-item label="状态">
                <a-select v-model:value="codeSearchParams.status" placeholder="选择状态" allow-clear style="width: 120px">
                  <a-select-option :value="0">未使用</a-select-option>
                  <a-select-option :value="1">已使用</a-select-option>
                  <a-select-option :value="2">已禁用</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="VIP时长">
                <a-select v-model:value="codeSearchParams.vipDuration" placeholder="选择时长" allow-clear style="width: 120px">
                  <a-select-option :value="30">30天</a-select-option>
                  <a-select-option :value="90">90天</a-select-option>
                  <a-select-option :value="365">365天</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="描述">
                <a-input v-model:value="codeSearchParams.description" placeholder="搜索描述" allow-clear class="modern-input" />
              </a-form-item>
              <a-form-item>
                <a-space>
                  <a-button type="primary" html-type="submit" class="search-btn">
                    <template #icon><SearchOutlined /></template>
                    搜索
                  </a-button>
                  <a-button @click="resetCodeSearch" class="reset-btn">重置</a-button>
                </a-space>
              </a-form-item>
            </a-form>
          </a-card>

          <div style="margin-bottom: 24px" />

          <!-- 兑换码表格 -->
          <a-table
            :columns="codeColumns"
            :data-source="codeList"
            :pagination="codePagination"
            :scroll="{ x: 1200 }"
            @change="handleCodeTableChange"
            :loading="codeLoading"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'code'">
                <a-typography-text copyable>{{ record.code }}</a-typography-text>
              </template>

              <template v-else-if="column.dataIndex === 'status'">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>

              <template v-else-if="column.dataIndex === 'vipDuration'">
                {{ record.vipDuration }} 天
              </template>

              <template v-else-if="column.dataIndex === 'expirationTime'">
                <span class="time-text">{{ formatTime(record.expirationTime) }}</span>
              </template>

              <template v-else-if="column.dataIndex === 'createTime'">
                <span class="time-text">{{ formatTime(record.createTime) }}</span>
              </template>

              <template v-else-if="column.key === 'action'">
                <a-button
                  v-if="record.status !== 1"
                  type="link"
                  size="small"
                  @click="toggleCodeStatus(record.id)"
                >
                  {{ record.status === 0 ? '禁用' : '启用' }}
                </a-button>
                <span v-else style="color: #999">-</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- Tab 2: 兑换记录 -->
        <a-tab-pane key="order" tab="兑换记录">
          <!-- 搜索区 -->
          <a-card class="search-card" :bordered="false">
            <a-form layout="inline" :model="orderSearchParams" @finish="searchOrders">
              <a-form-item label="用户ID">
                <a-input v-model:value="orderSearchParams.userId" placeholder="输入用户ID" allow-clear class="modern-input" />
              </a-form-item>
              <a-form-item label="兑换码">
                <a-input v-model:value="orderSearchParams.code" placeholder="输入兑换码" allow-clear class="modern-input" />
              </a-form-item>
              <a-form-item label="兑换时间">
                <a-range-picker
                  v-model:value="orderTimeRange"
                  show-time
                  format="YYYY-MM-DD HH:mm:ss"
                  style="width: 380px"
                />
              </a-form-item>
              <a-form-item>
                <a-space>
                  <a-button type="primary" html-type="submit" class="search-btn">
                    <template #icon><SearchOutlined /></template>
                    搜索
                  </a-button>
                  <a-button @click="resetOrderSearch" class="reset-btn">重置</a-button>
                </a-space>
              </a-form-item>
            </a-form>
          </a-card>

          <div style="margin-bottom: 24px" />

          <!-- 兑换记录表格 -->
          <a-table
            :columns="orderColumns"
            :data-source="orderList"
            :pagination="orderPagination"
            :scroll="{ x: 1400 }"
            @change="handleOrderTableChange"
            :loading="orderLoading"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'code'">
                <a-typography-text copyable>{{ record.code }}</a-typography-text>
              </template>

              <template v-else-if="column.dataIndex === 'vipDuration'">
                {{ record.vipDuration }} 天
              </template>

              <template v-else-if="column.dataIndex === 'oldExpireTime'">
                <span class="time-text">{{ formatTime(record.oldExpireTime) || '-' }}</span>
              </template>

              <template v-else-if="column.dataIndex === 'newExpireTime'">
                <span class="time-text">{{ formatTime(record.newExpireTime) }}</span>
              </template>

              <template v-else-if="column.dataIndex === 'createTime'">
                <span class="time-text">{{ formatTime(record.createTime) }}</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 生成兑换码弹窗 -->
    <a-modal
      v-model:open="generateModalVisible"
      title="生成 VIP 兑换码"
      :confirm-loading="generating"
      @ok="handleGenerate"
      width="600px"
    >
      <a-form :model="generateForm" layout="vertical">
        <a-form-item label="生成数量" required>
          <a-input-number v-model:value="generateForm.count" :min="1" :max="100" style="width: 100%" placeholder="输入生成数量" />
        </a-form-item>
        <a-form-item label="VIP 时长（天）" required>
          <a-input-number v-model:value="generateForm.vipDuration" :min="1" style="width: 100%" placeholder="例如：30" />
        </a-form-item>
        <a-form-item label="有效期（天）" required>
          <a-input-number v-model:value="generateForm.validDays" :min="1" style="width: 100%" placeholder="例如：90" />
        </a-form-item>
        <a-form-item label="兑换码前缀（可选）">
          <a-input v-model:value="generateForm.prefix" placeholder="例如：TUKU2024" />
        </a-form-item>
        <a-form-item label="描述（可选）">
          <a-textarea v-model:value="generateForm.description" :rows="3" placeholder="例如：春节活动兑换码" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 生成结果弹窗 -->
    <a-modal
      v-model:open="resultModalVisible"
      title="生成成功"
      :footer="null"
      width="700px"
    >
      <a-alert message="兑换码已生成，请妥善保管" type="success" show-icon style="margin-bottom: 16px" />
      <a-list :data-source="generatedCodes" bordered>
        <template #renderItem="{ item }">
          <a-list-item>
            <a-typography-text copyable code>{{ item }}</a-typography-text>
          </a-list-item>
        </template>
      </a-list>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watchEffect } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, SearchOutlined
} from '@ant-design/icons-vue'
import {
  generateVipCodeUsingPost,
  listVipCodeUsingGet,
  toggleVipCodeUsingPost,
  listVipOrderUsingGet
} from '@/api/vipController'
import dayjs, { Dayjs } from 'dayjs'

// Tab 状态
const activeTab = ref('code')

// ==================== 兑换码管理 ====================
const codeList = ref<API.VipCode[]>([])
const codeTotal = ref(0)
const codeLoading = ref(false)

const codeSearchParams = reactive({
  current: 1,
  pageSize: 10,
  code: undefined as string | undefined,
  status: undefined as number | undefined,
  vipDuration: undefined as number | undefined,
  description: undefined as string | undefined,
})

const codeColumns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '兑换码', dataIndex: 'code', width: 200 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: 'VIP时长', dataIndex: 'vipDuration', width: 100 },
  { title: '过期时间', dataIndex: 'expirationTime', width: 180 },
  { title: '描述', dataIndex: 'description', width: 200, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' },
]

const codePagination = computed(() => ({
  current: codeSearchParams.current,
  pageSize: codeSearchParams.pageSize,
  total: codeTotal.value,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条记录`,
}))

const fetchCodes = async () => {
  codeLoading.value = true
  try {
    const res = await listVipCodeUsingGet({ ...codeSearchParams })
    if (res.data.code === 0 && res.data.data) {
      codeList.value = res.data.data.records ?? []
      codeTotal.value = res.data.data.total ?? 0
    } else {
      message.error('查询失败：' + res.data.message)
    }
  } finally {
    codeLoading.value = false
  }
}

const searchCodes = () => {
  codeSearchParams.current = 1
  fetchCodes()
}

const resetCodeSearch = () => {
  codeSearchParams.code = undefined
  codeSearchParams.status = undefined
  codeSearchParams.vipDuration = undefined
  codeSearchParams.description = undefined
  searchCodes()
}

const handleCodeTableChange = (pag: any) => {
  codeSearchParams.current = pag.current
  codeSearchParams.pageSize = pag.pageSize
  fetchCodes()
}

// 状态映射
const getStatusColor = (status: number) => {
  const map: Record<number, string> = {
    0: 'green',
    1: 'default',
    2: 'red',
  }
  return map[status] || 'default'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '未使用',
    1: '已使用',
    2: '已禁用',
  }
  return map[status] || '未知'
}

// 启用/禁用
const toggleCodeStatus = async (id: number) => {
  try {
    const res = await toggleVipCodeUsingPost({ id })
    if (res.data.code === 0) {
      message.success('操作成功')
      fetchCodes()
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch (error) {
    message.error('操作失败')
  }
}

// 生成兑换码
const generateModalVisible = ref(false)
const generating = ref(false)
const generateForm = reactive({
  count: 1,
  vipDuration: 30,
  validDays: 90,
  prefix: '',
  description: '',
})

const showGenerateModal = () => {
  generateModalVisible.value = true
}

const resultModalVisible = ref(false)
const generatedCodes = ref<string[]>([])

const handleGenerate = async () => {
  if (!generateForm.count || !generateForm.vipDuration || !generateForm.validDays) {
    message.warning('请填写必填项')
    return
  }

  generating.value = true
  try {
    const res = await generateVipCodeUsingPost({
      count: generateForm.count,
      vipDuration: generateForm.vipDuration,
      validDays: generateForm.validDays,
      prefix: generateForm.prefix || undefined,
      description: generateForm.description || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      generatedCodes.value = res.data.data
      generateModalVisible.value = false
      resultModalVisible.value = true
      fetchCodes()
    } else {
      message.error('生成失败：' + res.data.message)
    }
  } finally {
    generating.value = false
  }
}

// ==================== 兑换记录 ====================
const orderList = ref<API.VipOrder[]>([])
const orderTotal = ref(0)
const orderLoading = ref(false)
const orderTimeRange = ref<[Dayjs, Dayjs]>()

const orderSearchParams = reactive({
  current: 1,
  pageSize: 10,
  userId: undefined as number | undefined,
  code: undefined as string | undefined,
  startCreateTime: undefined as string | undefined,
  endCreateTime: undefined as string | undefined,
})

const orderColumns = [
  { title: '订单ID', dataIndex: 'id', width: 80 },
  { title: '用户ID', dataIndex: 'userId', width: 120 },
  { title: '兑换码', dataIndex: 'code', width: 200 },
  { title: 'VIP时长', dataIndex: 'vipDuration', width: 100 },
  { title: '兑换前过期时间', dataIndex: 'oldExpireTime', width: 180 },
  { title: '兑换后过期时间', dataIndex: 'newExpireTime', width: 180 },
  { title: '兑换时间', dataIndex: 'createTime', width: 180 },
]

const orderPagination = computed(() => ({
  current: orderSearchParams.current,
  pageSize: orderSearchParams.pageSize,
  total: orderTotal.value,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条记录`,
}))

const fetchOrders = async () => {
  orderLoading.value = true
  try {
    // 处理时间范围（转换为 ISO 8601 格式）
    if (orderTimeRange.value && orderTimeRange.value.length === 2) {
      orderSearchParams.startCreateTime = orderTimeRange.value[0].format('YYYY-MM-DDTHH:mm:ss')
      orderSearchParams.endCreateTime = orderTimeRange.value[1].format('YYYY-MM-DDTHH:mm:ss')
    } else {
      orderSearchParams.startCreateTime = undefined
      orderSearchParams.endCreateTime = undefined
    }

    const res = await listVipOrderUsingGet({ ...orderSearchParams })
    if (res.data.code === 0 && res.data.data) {
      orderList.value = res.data.data.records ?? []
      orderTotal.value = res.data.data.total ?? 0
    } else {
      message.error('查询失败：' + res.data.message)
    }
  } finally {
    orderLoading.value = false
  }
}

const searchOrders = () => {
  orderSearchParams.current = 1
  fetchOrders()
}

const resetOrderSearch = () => {
  orderSearchParams.userId = undefined
  orderSearchParams.code = undefined
  orderTimeRange.value = undefined
  searchOrders()
}

const handleOrderTableChange = (pag: any) => {
  orderSearchParams.current = pag.current
  orderSearchParams.pageSize = pag.pageSize
  fetchOrders()
}

// 时间格式化
const formatTime = (time: string | null | undefined) => {
  if (!time) return ''
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 初始化
onMounted(() => {
  fetchCodes()
})

// 监听 Tab 切换
watchEffect(() => {
  if (activeTab.value === 'order') {
    fetchOrders()
  }
})
</script>

<style scoped>
#vipManagePage {
  padding: 0 4px;
}

.main-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.search-card {
  background: #fafafa;
  border-radius: 8px;
}

.modern-input {
  border-radius: 8px;
  min-width: 180px;
}

.search-btn {
  border-radius: 8px;
  padding-left: 20px;
  padding-right: 20px;
  font-weight: 600;
}

.reset-btn {
  border-radius: 8px;
}

.time-text {
  font-size: 13px;
  color: #666;
}
</style>
