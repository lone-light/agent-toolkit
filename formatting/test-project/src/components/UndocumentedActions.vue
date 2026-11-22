<script setup lang="ts">
import { ref } from 'vue'

const draft = ref('')

function trimDraft(): void {
  draft.value = draft.value.trim()
}

const clearDraft = (): void => {
  draft.value = ''
}
</script>

<template>
  <input v-model="draft" />
  <button type="button" @click="trimDraft">整理</button>
  <button type="button" @click="clearDraft">清空</button>
</template>
