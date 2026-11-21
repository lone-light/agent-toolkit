<script setup lang="ts">
import { fetchStatus } from '@/api/status'

fetchStatus()
</script>

<template>
  <p>查询服务状态</p>
</template>
