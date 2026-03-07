<template>
  <div class="batch-edit-modal">
    <a-modal
      v-model:visible="visible"
      :footer="false"
      @cancel="closeModal"
      width="520px"
      centered
    >
      <template #title>
        <div class="modal-title">
          <edit-outlined style="color: #1890ff" />
          <span>批量编辑图片信息</span>
          <a-tag color="blue" style="margin-left: 8px">
            已选中 {{ pictureList?.length || 0 }} 张
          </a-tag>
        </div>
      </template>

      <a-alert
        message="仅对当前页选中的图片生效"
        type="info"
        show-icon
        style="margin-bottom: 20px; border-radius: 8px"
      >
        <template #icon><info-circle-outlined /></template>
      </a-alert>

      <a-form layout="vertical" :model="formData" @finish="handleSubmit" class="refined-form">
        <a-form-item label="统一分类" name="category">
          <template #extra>将为选中的图片设置相同的分类</template>
          <a-auto-complete
            v-model:value="formData.category"
            :options="categoryOptions"
            placeholder="搜一搜或输入新分类..."
            allowClear
          >
            <template #suffix>
              <folder-open-outlined style="color: rgba(0,0,0,0.25)" />
            </template>
          </a-auto-complete>
        </a-form-item>

        <a-form-item label="附加标签" name="tags">
          <template #extra>输入后按回车可创建新标签</template>
          <a-select
            v-model:value="formData.tags"
            :options="tagOptions"
            mode="tags"
            placeholder="请选择或输入标签"
            allowClear
            :max-tag-count="5"
          >
            <template #suffixIcon>
              <tags-outlined />
            </template>
          </a-select>
        </a-form-item>

        <a-form-item label="重命名规则" name="nameRule">
          <a-input
            v-model:value="formData.nameRule"
            placeholder="例：图库_{序号}"
            size="large"
          >
            <template #prefix>
              <form-outlined style="color: rgba(0,0,0,0.25)" />
            </template>
          </a-input>
          <div class="naming-tip">
            支持变量：<code class="variable-tag">{序号}</code>
            <span> (效果：图库_1, 图库_2...)</span>
          </div>
        </a-form-item>

        <a-divider style="margin: 12px 0" />

        <div class="form-actions">
          <a-button @click="closeModal" style="margin-right: 12px">取消</a-button>
          <a-button type="primary" html-type="submit" size="large" block style="flex: 1; border-radius: 8px">
            确认修改 {{ pictureList?.length }} 张图片
          </a-button>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  EditOutlined,
  InfoCircleOutlined,
  FolderOpenOutlined,
  TagsOutlined,
  FormOutlined
} from '@ant-design/icons-vue'
import {
  editPictureByBatchUsingPost,
  listPictureTagCategoryUsingGet
} from '@/api/pictureController'
import { message } from 'ant-design-vue'

interface Props {
  pictureList: API.PictureVO[]
  spaceId: number
  onSuccess: () => void
}

const props = withDefaults(defineProps<Props>(), {})
const visible = ref(false)

const formData = reactive({
  category: '',
  tags: [],
  nameRule: ''
})

const categoryOptions = ref<any[]>([])
const tagOptions = ref<any[]>([])

const openModal = () => { visible.value = true }
const closeModal = () => { visible.value = false }

defineExpose({ openModal })

const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => ({ value: data, label: data }))
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => ({ value: data, label: data }))
  }
}

onMounted(() => { getTagCategoryOptions() })

const handleSubmit = async (values: any) => {
  if (!props.pictureList || props.pictureList.length === 0) {
    message.warning('请先选择图片')
    return
  }
  const res = await editPictureByBatchUsingPost({
    pictureIdList: props.pictureList.map((picture) => picture.id),
    spaceId: props.spaceId,
    ...values,
  })
  if (res.data.code === 0) {
    message.success(`成功更新 ${props.pictureList.length} 张图片`)
    closeModal()
    props.onSuccess?.()
  } else {
    message.error('操作失败：' + res.data.message)
  }
}
</script>

<style scoped>
.modal-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.naming-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #8c8c8c;
}

.variable-tag {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  color: #1890ff;
  font-family: monospace;
}

.form-actions {
  display: flex;
  align-items: center;
  margin-top: 24px;
}

/* 稍微美化一下表单间距 */
.refined-form :deep(.ant-form-item) {
  margin-bottom: 20px;
}

.refined-form :deep(.ant-input),
.refined-form :deep(.ant-select-selector) {
  border-radius: 6px !important;
}
</style>
