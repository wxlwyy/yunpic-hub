<template>
  <div id="categoryManagePage">
    <a-card :bordered="false" class="main-card">
      <div style="margin-bottom: 16px">
        <a-button type="primary" @click="showAddModal">
          <template #icon><PlusOutlined /></template>
          新增分类
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="categoryList"
        :loading="loading"
        :pagination="false"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'isActive'">
            <a-tag :color="record.isActive === 1 ? 'green' : 'red'">
              {{ record.isActive === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="showEditModal(record)">编辑</a-button>
            <a-popconfirm
              title="确定删除该分类？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete(record.id)"
            >
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      @ok="handleSubmit"
      @cancel="modalVisible = false"
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item label="分类名称" required>
          <a-input v-model:value="formData.name" placeholder="输入分类名称" />
        </a-form-item>
        <a-form-item label="排序权重（越小越靠前）">
          <a-input-number v-model:value="formData.sortOrder" :min="0" :max="999" placeholder="默认 99" />
        </a-form-item>
        <a-form-item v-if="isEdit" label="是否启用">
          <a-switch
            :checked="formData.isActive === 1"
            @change="(val: boolean) => (formData.isActive = val ? 1 : 0)"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import {
  listCategoriesUsingGet,
  addCategoryUsingPost,
  updateCategoryUsingPost,
  deleteCategoryUsingPost,
} from '@/api/adminCategoryController'

const loading = ref(false)
const categoryList = ref<API.PictureCategory[]>([])
const modalVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number>()

const formData = reactive<API.UpdateCategoryRequest>({
  id: undefined,
  name: '',
  sortOrder: 99,
  isActive: 1,
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 100 },
  { title: '分类名称', dataIndex: 'name', key: 'name' },
  { title: '排序权重', dataIndex: 'sortOrder', key: 'sortOrder', width: 120 },
  { title: '状态', key: 'isActive', width: 100 },
  { title: '操作', key: 'action', width: 180 },
]

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listCategoriesUsingGet()
    if (res.data.code === 0 && res.data.data) {
      categoryList.value = res.data.data
    }
  } finally {
    loading.value = false
  }
}

const showAddModal = () => {
  isEdit.value = false
  editId.value = undefined
  formData.id = undefined
  formData.name = ''
  formData.sortOrder = 99
  formData.isActive = 1
  modalVisible.value = true
}

const showEditModal = (record: API.PictureCategory) => {
  isEdit.value = true
  editId.value = record.id
  formData.id = record.id
  formData.name = record.name ?? ''
  formData.sortOrder = record.sortOrder ?? 99
  formData.isActive = record.isActive ?? 1
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.name?.trim()) {
    message.error('分类名称不能为空')
    return
  }
  try {
    if (isEdit.value && editId.value) {
      await updateCategoryUsingPost({
        id: editId.value,
        name: formData.name.trim(),
        sortOrder: formData.sortOrder,
        isActive: formData.isActive,
      })
      message.success('更新成功')
    } else {
      await addCategoryUsingPost({
        name: formData.name.trim(),
        sortOrder: formData.sortOrder,
      })
      message.success('新增成功')
    }
    modalVisible.value = false
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await deleteCategoryUsingPost({ id })
    message.success('删除成功')
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}

onMounted(() => {
  fetchList()
})
</script>
