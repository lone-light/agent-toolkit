<script setup lang="ts">
import { ref } from 'vue'
import { normalizeDraft, SubmissionCounter } from './draft'

const props = withDefaults(
  defineProps<{
    /**
     * 输入框的初始文字，省略时从空草稿开始
     */
    initialText?: string

    /**
     * 草稿允许输入的最大长度
     */
    maxLength: number
  }>(),
  { initialText: '' },
)

const emit = defineEmits<{
  /**
   * 草稿校验通过时触发，传出整理后的文字和累计提交次数
   */
  saved: [text: string, submissionCount: number]
}>()

const draft = ref(props.initialText)
const input = ref<HTMLInputElement | null>(null)
const submissions = new SubmissionCounter()

/**
 * 校验草稿，通过后触发 `saved` 事件
 */
function save(): void {
  const text = normalizeDraft(draft.value)
  if (!text || text.length > props.maxLength) return

  emit('saved', text, submissions.record())
}

/**
 * 将草稿恢复为父组件传入的初始文字
 */
const reset = (): void => {
  draft.value = props.initialText
}

/**
 * 将焦点移到输入框，供父组件通过模板引用调用
 */
function focus(): void {
  input.value?.focus()
}

defineExpose({ focus })
</script>

<template>
  <input ref="input" v-model="draft" :maxlength="props.maxLength" />
  <button type="button" @click="save">保存</button>
  <button type="button" @click="reset">重置</button>
</template>
