<template>
  <div id="addPictureBatchPage">
    <div class="page-header">
      <h2 class="page-title"><CloudDownloadOutlined /> 批量导入图片</h2>
      <p class="page-desc">配置抓取规则，系统将自动从网络为您提取并导入相关高清图片资源。</p>
    </div>

    <a-card class="task-card" :bordered="false">
      <a-form layout="vertical" :model="formData" @finish="handleSubmit" class="modern-form">
        <a-form-item label="检索关键词" name="searchText" :rules="[{ required: true, message: '请输入抓取关键词' }]">
          <a-input
            v-model:value="formData.searchText"
            placeholder="例如：赛博朋克、自然风景、高清壁纸..."
            class="modern-input"
            size="large"
          >
            <template #prefix><SearchOutlined style="color: #bfbfbf" /></template>
          </a-input>
        </a-form-item>

        <a-row :gutter="24">
          <a-col :xs="24" :md="12">
            <a-form-item label="目标抓取数量" name="count">
              <a-input-number
                v-model:value="formData.count"
                placeholder="请输入数量"
                class="modern-input w-full"
                size="large"
                :min="1"
                :max="30"
                allow-clear
              />
              <div class="form-tip">单次最多允许抓取 30 张图片以保障系统稳定</div>
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="12">
            <a-form-item label="统一命名前缀" name="picNamePrefix">
              <a-input
                v-model:value="formData.picNamePrefix"
                placeholder="例如：风景图，将生成 风景图_1..."
                class="modern-input"
                size="large"
              >
                <template #prefix><EditOutlined style="color: #bfbfbf" /></template>
              </a-input>
            </a-form-item>
          </a-col>
        </a-row>

        <div class="form-footer">
          <a-button
            type="primary"
            html-type="submit"
            class="submit-btn"
            size="large"
            block
            :loading="loading"
          >
            <template #icon><ThunderboltOutlined v-if="!loading" /></template>
            {{ loading ? '抓取引擎全速运行中...' : '立即执行抓取任务' }}
          </a-button>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import {
  CloudDownloadOutlined, SearchOutlined,
  EditOutlined, ThunderboltOutlined
} from '@ant-design/icons-vue'
import { reactive, ref } from 'vue'
import { uploadPictureByBatchUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'

// 定义表单数据，保持与后端接口一致
const formData = reactive<API.UploadPictureByBatchRequest>({
  count: 10,
})
const loading = ref(false)
const router = useRouter()

/**
 * 提交表单执行批量抓取
 */
const handleSubmit = async () => {
  loading.value = true
  try {
    const res = await uploadPictureByBatchUsingPost({
      ...formData,
    })
    if (res.data.code === 0 && res.data.data) {
      message.success(`任务执行完毕，成功导入 ${res.data.data} 张图片！`)
      router.push({
        path: '/', // 跳转回主页或图片管理页
      })
    } else {
      message.error('任务执行失败：' + res.data.message)
    }
  } catch (error) {
    message.error('网络请求异常，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
#addPictureBatchPage {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px;
}

/* 头部样式 */
.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.page-desc {
  color: #64748b;
  font-size: 15px;
}

/* 核心表单卡片 */
.task-card {
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.04);
  padding: 16px;
}

.modern-input {
  border-radius: 10px !important;
}

.w-full {
  width: 100%;
}

.form-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 6px;
}

/* 提交按钮 */
.form-footer {
  margin-top: 32px;
}

.submit-btn {
  height: 54px;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  box-shadow: 0 8px 16px rgba(37, 99, 235, 0.2);
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 20px rgba(37, 99, 235, 0.3);
}

.submit-btn:active {
  transform: translateY(0);
}
</style>
