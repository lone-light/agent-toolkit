<script setup lang="ts">
import { fetchStatus } from '@/api/status'

const { name } = defineProps<{ name: string }>()
const emit = defineEmits<{ checked: [available: boolean] }>()

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
