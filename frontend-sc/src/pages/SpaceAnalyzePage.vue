<template>
  <div id="spaceAnalyzePage">
    <div class="analyze-header">
      <div class="title-section">
        <a-button type="link" class="back-btn" @click="handleBack">
          <template #icon><LeftOutlined /></template>
          返回
        </a-button>
        <BarChartOutlined class="header-icon" />
        <div class="text-group">
          <h2>
            图库数据分析
            <small class="subtitle" v-if="queryAll"> [ 全站总览 ] </small>
            <small class="subtitle" v-else-if="queryPublic"> [ 公共图库 ] </small>
            <small class="subtitle" v-else-if="space">
              [ {{ space.spaceName }} ]
            </small>
            <small class="subtitle" v-else-if="spaceId">
              [ 加载中... ]
            </small>
          </h2>
        </div>
      </div>

      <a-tag v-if="spaceId" color="blue" class="id-tag" @click="handleBack">
        <template #icon><LinkOutlined /></template>
        空间 ID: {{ spaceId }}
      </a-tag>
    </div>

    <div style="margin-bottom: 24px" />

    <a-row :gutter="[24, 24]">
      <a-col :xs="24" :md="12">
        <a-card title="存储空间占用" :bordered="false" class="analyze-card">
          <SpaceUsageAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
        </a-card>
      </a-col>
      <a-col :xs="24" :md="12">
        <a-card title="图片分类分布" :bordered="false" class="analyze-card">
          <SpaceCategoryAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
        </a-card>
      </a-col>

      <a-col :span="24">
        <a-card title="上传活跃趋势" :bordered="false" class="analyze-card trend-card">
          <SpaceUserAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
        </a-card>
      </a-col>

      <a-col :xs="24" :md="12">
        <a-card title="热门标签统计" :bordered="false" class="analyze-card">
          <SpaceTagAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
        </a-card>
      </a-col>
      <a-col :xs="24" :md="12">
        <a-card title="资源大小分布" :bordered="false" class="analyze-card">
          <SpaceSizeAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
        </a-card>
      </a-col>

      <a-col v-if="isAdmin" :span="24">
        <a-card title="全站空间资源消耗排行Top 10" :bordered="false" class="analyze-card rank-card">
          <SpaceRankAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { BarChartOutlined, LinkOutlined, LeftOutlined } from '@ant-design/icons-vue'
import SpaceUsageAnalyze from '@/components/analyze/SpaceUsageAnalyze.vue'
import SpaceCategoryAnalyze from '@/components/analyze/SpaceCategoryAnalyze.vue'
import SpaceTagAnalyze from '@/components/analyze/SpaceTagAnalyze.vue'
import SpaceSizeAnalyze from '@/components/analyze/SpaceSizeAnalyze.vue'
import SpaceUserAnalyze from '@/components/analyze/SpaceUserAnalyze.vue'
import SpaceRankAnalyze from '@/components/analyze/SpaceRankAnalyze.vue'
import { computed, ref, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import router from "@/router"
import { getSpaceVoByIdUsingGet } from "@/api/spaceController.ts"

// 1. 先定义路由和基础参数 (这是所有逻辑的源头)
const route = useRoute()

// 空间 id
const spaceId = computed(() => {
  return route.query?.spaceId as string
})

// 是否查询所有空间
const queryAll = computed(() => {
  return !!route.query?.queryAll
})

// 是否查询公共空间
const queryPublic = computed(() => {
  return !!route.query?.queryPublic
})

// 用户权限相关
const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser
const isAdmin = computed(() => {
  return loginUser.userRole === 'admin'
})

// 2. 定义存放数据的状态
const space = ref<API.SpaceVO>()

// 3. 定义获取数据的方法
const fetchSpace = async () => {
  if (spaceId.value) {
    const res = await getSpaceVoByIdUsingGet({ id: spaceId.value as any })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    }
  }
}

const handleBack = () => {
  // 1. 全局或公共视角：这是纯粹的管理功能，必须回管理后台
  if (queryAll.value || queryPublic.value) {
    router.push('/admin/spaceManage')
    return
  }

  // 2. 针对特定空间的分析（spaceId 存在）
  if (spaceId.value) {
    // 🚀 核心修复：如果是管理员，得看这空间是不是他自己的
    if (isAdmin.value) {
      // 只有空间数据已加载，且 space 里的 userId 等于当前登录者的 id，才叫“回自己家”
      // 注意：如果数据还没加载完 (space 为空)，管理员默认回管理页，安全第一
      if (space.value && space.value.userId === loginUser.id) {
        router.push(`/space/${spaceId.value}`)
      } else {
        // 分析的是别人的空间，管理员点击返回，应该回【管理后台】继续查下一个人
        router.push('/admin/spaceManage')
      }
    } else {
      // 普通用户：没那么多心眼，直接回自己的空间详情
      router.push(`/space/${spaceId.value}`)
    }
    return
  }

  // 3. 兜底逻辑：回上一页
  router.back()
}

// 4. 最后开启监听 (此时 spaceId 和 fetchSpace 都已经就绪了)
watchEffect(() => {
  if (spaceId.value) {
    fetchSpace()
  } else {
    space.value = undefined
  }
})
</script>

<style scoped>
#spaceAnalyzePage {
  padding: 0 10px;
}

/* 头部样式美化 */
.analyze-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 12px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.back-btn {
  color: #8c8c8c;
  font-size: 16px;
  padding-left: 0;
}

.back-btn:hover {
  color: #1890ff;
}

/* 垂直分割线样式 */
:deep(.ant-divider-vertical) {
  height: 20px;
  margin: 0 16px;
  background-color: rgba(0, 0, 0, 0.06);
}

/* 确保卡片内部的内容区域也能撑开 */
:deep(.ant-card-body) {
  flex: 1;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 28px;
  color: #1890ff;
  background: #e6f7ff;
  padding: 8px;
  border-radius: 8px;
}

.subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin-left: 8px;
  font-weight: normal;
}

.id-tag {
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 6px;
  transition: all 0.3s;
}

.id-tag:hover {
  opacity: 0.8;
  transform: translateY(-1px);
}

.trend-card {
  min-height: 400px;
}

/* 卡片阴影与圆角 */
.analyze-card {
  height: 100%;
  border-radius: 12px;
  transition: all 0.3s;
}
.analyze-card:hover {
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
}

.rank-card {
  background: linear-gradient(to bottom right, #ffffff, #fafafa);
}
</style>
