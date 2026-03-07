<template>
  <a-modal
    class="image-cropper-modal"
    v-model:visible="visible"
    title="专业图片编辑"
    :footer="false"
    @cancel="closeModal"
    width="800px"
  >
    <div class="cropper-container">
      <div v-if="isTeamSpace" class="collab-status-bar">
        <div v-if="editingUser" class="editing-badge">
          <span class="pulse-dot"></span>
          <a-avatar :size="20" :src="editingUser.userAvatar" />
          <span class="user-name">{{ editingUser.userName }} 正在编辑中...</span>
        </div>
        <div v-else class="idle-badge">
          <CheckCircleOutlined />
          暂无人编辑，点击“进入编辑”获取权限
        </div>
      </div>

      <div class="cropper-workspace">
        <vue-cropper
          :key="cropperKey"
          ref="cropperRef"
          :img="imageUrl"
          :autoCrop="true"
          :fixedBox="false"
          :centerBox="true"
          :canMoveBox="true"
          :info="true"
          :full="true"
          :maxImgSize="99999"
          outputType="jpeg"
        />
      </div>

      <div class="cropper-footer">
        <div class="edit-control-row" v-if="isTeamSpace">
          <a-space>
            <a-button v-if="canEnterEdit" type="primary" shape="round" @click="enterEdit">
              <template #icon>
                <EditOutlined />
              </template>
              进入编辑
            </a-button>
            <a-button v-if="canExitEdit" danger ghost shape="round" @click="exitEdit">
              <template #icon>
                <LogoutOutlined />
              </template>
              退出编辑
            </a-button>
          </a-space>
        </div>

        <div class="tool-bar">
          <a-space size="middle">
            <a-button-group>
              <a-button :disabled="!canEdit" @click="rotateLeft" title="向左旋转">
                <RotateLeftOutlined />
              </a-button>
              <a-button :disabled="!canEdit" @click="rotateRight" title="向右旋转">
                <RotateRightOutlined />
              </a-button>
            </a-button-group>
            <a-button-group>
              <a-button :disabled="!canEdit" @click="changeScale(1)" title="放大">
                <ZoomInOutlined />
              </a-button>
              <a-button :disabled="!canEdit" @click="changeScale(-1)" title="缩小">
                <ZoomOutOutlined />
              </a-button>
            </a-button-group>
            <a-button
              type="primary"
              :loading="loading"
              :disabled="!canEdit"
              @click="handleConfirm"
              class="confirm-btn"
            >
              保存修改
            </a-button>
          </a-space>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import {
  RotateLeftOutlined,
  RotateRightOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  EditOutlined,
  LogoutOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons-vue'
import { computed, onUnmounted, ref, watchEffect } from 'vue'
import { uploadPictureUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import PictureEditWebSocket from '@/utils/pictureEditWebSocket.ts'
import { PICTURE_EDIT_ACTION_ENUM, PICTURE_EDIT_MESSAGE_TYPE_ENUM } from '@/constants/picture.ts'
import { SPACE_TYPE_ENUM } from '@/constants/space.ts'

const props = defineProps<{
  imageUrl?: string
  picture?: API.PictureVO
  spaceId?: number
  space?: API.SpaceVO
  onSuccess?: (newPicture: API.PictureVO) => void
}>()

const visible = ref(false)
const cropperKey = ref(0) // 解决残留的关键
const cropperRef = ref()
const loading = ref(false)

// 关键逻辑：每次打开弹窗，自增 key，强制重置编辑器
const openModal = () => {
  cropperKey.value++
  visible.value = true
}

const closeModal = () => {
  visible.value = false
  if (websocket) websocket.disconnect()
  editingUser.value = undefined
}

const isTeamSpace = computed(() => props.space?.spaceType === SPACE_TYPE_ENUM.TEAM)
const loginUserStore = useLoginUserStore()
let loginUser = loginUserStore.loginUser
const editingUser = ref<API.UserVO>()

const canEnterEdit = computed(() => !editingUser.value)
const canExitEdit = computed(() => editingUser.value?.id === loginUser.id)
const canEdit = computed(() => !isTeamSpace.value || editingUser.value?.id === loginUser.id)

// WebSocket 逻辑保持原版，不做任何改动
let websocket: PictureEditWebSocket | null

const initWebsocket = () => {
  if (!props.picture?.id || !visible.value) return
  if (websocket) websocket.disconnect()
  websocket = new PictureEditWebSocket(props.picture.id)
  websocket.connect()

  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.INFO, (msg) => message.info(msg.message))
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ERROR, (msg) => message.error(msg.message))
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT, (msg) => (editingUser.value = msg.userVO))
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION, (msg) => {
    if (!cropperRef.value) return
    switch (msg.editAction) {
      case PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT:
        cropperRef.value.rotateLeft()
        break
      case PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT:
        cropperRef.value.rotateRight()
        break
      case PICTURE_EDIT_ACTION_ENUM.ZOOM_IN:
        cropperRef.value.changeScale(1)
        break
      case PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT:
        cropperRef.value.changeScale(-1)
        break
    }
  })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT, () => (editingUser.value = undefined))
}

watchEffect(() => {
  if (isTeamSpace.value) initWebsocket()
})
onUnmounted(() => {
  if (websocket) websocket.disconnect()
})

const enterEdit = () => websocket?.sendMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT })
const exitEdit = () => websocket?.sendMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT })
const editAction = (action: string) =>
  websocket?.sendMessage({
    type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION,
    editAction: action,
  })

const rotateLeft = () => {
  cropperRef.value.rotateLeft()
  editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT)
}
const rotateRight = () => {
  cropperRef.value.rotateRight()
  editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT)
}
const changeScale = (num: number) => {
  cropperRef.value.changeScale(num)
  editAction(num > 0 ? PICTURE_EDIT_ACTION_ENUM.ZOOM_IN : PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT)
}

const handleConfirm = () => {
  cropperRef.value.getCropBlob((blob: Blob) => {
    // 🚀 后缀改为 .jpg
    const fileName = (props.picture?.name || 'img') + '.jpg'
    handleUpload({
      file: new File([blob], fileName, { type: 'image/jpeg' })
    })
  })
}

const handleUpload = async ({ file }: any) => {
  loading.value = true
  try {
    const params: API.UploadPictureRequest = props.picture ? { id: props.picture.id } : {}
    params.spaceId = props.spaceId
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0) {
      message.success('修改成功')
      props.onSuccess?.(res.data.data)
      closeModal()
    }
  } finally {
    loading.value = false
  }
}

defineExpose({ openModal })
</script>

<style scoped>
.cropper-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.collab-status-bar {
  height: 32px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.editing-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff1f0;
  border: 1px solid #ffa39e;
  padding: 4px 12px;
  border-radius: 20px;
  color: #cf1322;
  font-size: 13px;
  font-weight: 600;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background: #ff4d4f;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(255, 77, 79, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(255, 77, 79, 0);
  }
}

.cropper-workspace {
  background: #1e293b;
  padding: 20px;
  border-radius: 12px;
}

.vue-cropper {
  height: 450px !important;
}

.cropper-footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.tool-bar {
  padding: 12px 24px;
  background: #f8fafc;
  border-radius: 50px;
  border: 1px solid #e2e8f0;
}

/* 保存修改按钮的增强样式 */
.confirm-btn {
  padding-left: 24px;
  padding-right: 24px;
  font-weight: 700;
  /* 正常状态下的渐变蓝 */
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  border: none;
  color: white;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
  transition: all 0.3s;
}

/* 关键修复：当按钮被禁用（锁定）时的样式 */
.confirm-btn[disabled],
.confirm-btn[disabled]:hover {
  background: #f5f5f5 !important; /* 灰白底色 */
  color: rgba(0, 0, 0, 0.25) !important; /* 暗淡文字 */
  border: 1px solid #d9d9d9 !important; /* 灰色边框 */
  box-shadow: none !important; /* 去掉阴影 */
  cursor: not-allowed; /* 变成禁用图标 */
  background-image: none !important; /* 强制去掉渐变 */
}
</style>
