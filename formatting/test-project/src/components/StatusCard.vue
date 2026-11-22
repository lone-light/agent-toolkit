<script setup lang="ts">
import { fetchStatus } from '@/api/status'

const { name } = defineProps<{
  /**
   * 按钮上显示的服务名称
   */
  name: string
}>()
const emit = defineEmits<{
  /**
   * 完成查询时触发，传出服务是否可用
   */
  checked: [available: boolean]
}>()

/**
 * 查询服务并通知父组件查询结果
 */
async function checkStatus() {
  emit('checked', await fetchStatus())
}
</script>

<template>
  <button class="status-card" type="button" @click="checkStatus">{{ name }}</button>
</template>

<style scoped>
.status-card {
  padding: 0.5rem;
}
</style>
