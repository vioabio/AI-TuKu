<template>
  <div id="spaceAnalyzePage">
    <h2 style="margin-bottom: 16px">
      空间图库分析 -
      <span v-if="queryAll"> 全部空间 </span>
      <span v-else-if="queryPublic"> 公共图库 </span>
      <span v-else>
        <a :href="`/space/${spaceId}`" target="_blank">id：{{ spaceId }}</a>
      </span>
    </h2>
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :md="12">
        <SpaceUsageAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
      </a-col>
      <a-col :xs="24" :md="12">
        <SpaceCategoryAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
      </a-col>
      <a-col :xs="24" :md="12">
        <SpaceTagAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
      </a-col>
      <a-col :xs="24" :md="12">
        <SpaceSizeAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
      </a-col>
      <a-col :xs="24" :md="12">
        <SpaceUserAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
      </a-col>
      <a-col v-if="isAdmin" :xs="24" :md="12">
        <SpaceRankAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import SpaceUsageAnalyze from '@/components/SpaceUsageAnalyze.vue'
import SpaceCategoryAnalyze from '@/components/SpaceCategoryAnalyze.vue'
import SpaceTagAnalyze from '@/components/SpaceTagAnalyze.vue'
import SpaceSizeAnalyze from '@/components/SpaceSizeAnalyze.vue'
import SpaceUserAnalyze from '@/components/SpaceUserAnalyze.vue'
import SpaceRankAnalyze from '@/components/SpaceRankAnalyze.vue'

const route = useRoute()

const spaceId = computed(() => route.query?.spaceId as string)
const queryAll = computed(() => !!route.query?.queryAll)
const queryPublic = computed(() => !!route.query?.queryPublic)

const loginUserStore = useLoginUserStore()
const isAdmin = computed(() => loginUserStore.loginUser?.userRole === 'admin')
</script>

<style scoped>
#spaceAnalyzePage {
  max-width: 1280px;
  margin: 0 auto;
}
</style>
