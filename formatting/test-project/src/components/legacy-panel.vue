<template>
  <p>{{ label }}</p>
</template>

<script lang="ts">
export default {
  data: () => ({ label: '服务状态' }),
}
</script>

<style>
p {
  color: gray;
}
</style>
