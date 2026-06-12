<template>
  <a-modal
    class="share-modal"
    v-model:visible="visible"
    :title="title"
    :footer="false"
    @cancel="closeModal"
    width="480px"
    centered
  >
    <div class="share-content">
      <div class="qr-section">
        <div class="qr-container">
          <a-qrcode :value="link" :size="180" color="#1890ff" />
          <div class="qr-tip">
            <mobile-outlined /> 手机扫码快速查看
          </div>
        </div>
        <a-button type="link" size="small" @click="downloadQRCode">
          <template #icon><download-outlined /></template>
          保存二维码图片
        </a-button>
      </div>

      <a-divider />

      <div class="link-section">
        <h4 class="section-title"><link-outlined /> 复制分享链接</h4>
        <div class="copy-box">
          <a-input-search
            readonly
            :value="link"
            enter-button="复制链接"
            size="large"
            @search="onCopyLink"
          >
            <template #prefix>
              <share-alt-outlined style="color: rgba(0, 0, 0, 0.45)" />
            </template>
          </a-input-search>
        </div>
      </div>

      <div class="social-section">
        <div class="social-label">一键分享至：</div>
        <a-space size="middle">
          <div class="social-icon wechat" title="微信"><wechat-filled /></div>
          <div class="social-icon weibo" title="微博"><weibo-circle-filled /></div>
          <div class="social-icon qq" title="QQ"><qq-circle-filled /></div>
        </a-space>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, defineExpose } from 'vue' // ✅ Vue 的归 Vue
import { message } from 'ant-design-vue' // ✅ Ant Design 的归 Ant Design
import {
  MobileOutlined,
  DownloadOutlined,
  LinkOutlined,
  ShareAltOutlined,
  WechatFilled,
  WeiboCircleFilled,
  QqCircleFilled
} from '@ant-design/icons-vue'

/**
 * 定义组件属性
 */
interface Props {
  title?: string
  link?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '分享图片',
  link: 'https://laoyujianli.com/share/default',
})

const visible = ref(false)

// 复制链接回调
const onCopyLink = () => {
  navigator.clipboard.writeText(props.link)
  message.success('链接已复制到剪贴板，快去分享吧！')
}

// 模拟下载二维码
const downloadQRCode = () => {
  const canvas = document.querySelector('.share-modal canvas') as HTMLCanvasElement
  if (canvas) {
    const url = canvas.toDataURL('image/png')
    const a = document.createElement('a')
    a.download = 'share-qrcode.png'
    a.href = url
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    message.success('二维码已保存')
  }
}

const openModal = () => { visible.value = true }
const closeModal = () => { visible.value = false }

defineExpose({ openModal })
</script>

<style scoped>
.share-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
}

.qr-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.qr-container {
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  text-align: center;
}

.qr-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #64748b;
}

.link-section {
  width: 100%;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
}

.copy-box :deep(.ant-input-group-addon .ant-btn) {
  border-top-right-radius: 8px !important;
  border-bottom-right-radius: 8px !important;
}

.copy-box :deep(.ant-input) {
  border-top-left-radius: 8px !important;
  border-bottom-left-radius: 8px !important;
}

.social-section {
  margin-top: 24px;
  text-align: center;
}

.social-label {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 12px;
}

.social-icon {
  font-size: 24px;
  cursor: pointer;
  transition: transform 0.2s, color 0.2s;
  color: #cbd5e1;
}

.social-icon:hover {
  transform: scale(1.2);
}

.wechat:hover { color: #07c160; }
.weibo:hover { color: #f91d1d; }
.qq:hover { color: #12b7f5; }
</style>
