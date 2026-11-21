<script setup lang="ts">
const emit = defineEmits<{ checked: [] }>()
const { name } = defineProps<{ name: string }>()
</script>

<template>
  <button type="button" @click="emit('cancelled')">{{ name }}</button>
</template>
