<script setup lang="ts">
// [SDD-TASK: Task001][SDD-SPEC: 02-功能规范.md §7.2.2 + §8 FE-02]
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import type { WorkitemPriority } from '@/types/workitem'
import { createWorkitem } from '@/api/workitem'

const router = useRouter()
const title = ref('')
const description = ref('')
const priority = ref<WorkitemPriority>('MEDIUM')
const dueDate = ref('')
const loading = ref(false)
const errors = ref<{ title?: string; description?: string }>({})

function validate(): boolean {
  errors.value = {}
  // [SDD-SPEC: §3.1] title 1~100,description ≤500
  const t = title.value.trim()
  if (!t || t.length < 1 || t.length > 100) errors.value.title = '长度需 1~100'
  if (description.value.length > 500) errors.value.description = '长度需 ≤ 500'
  return Object.keys(errors.value).length === 0
}

async function submit() {
  if (!validate()) return
  loading.value = true
  try {
    await createWorkitem({
      title: title.value.trim(),
      description: description.value || undefined,
      priority: priority.value,
      dueDate: dueDate.value || undefined,
    })
    router.push('/workitems')
  } catch {
    // toast 已由拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <h2>新建工作项</h2>
    <div class="card">
      <form @submit.prevent="submit">
        <div class="field">
          <label>标题 *</label>
          <input v-model="title" :disabled="loading" />
          <div v-if="errors.title" class="err">{{ errors.title }}</div>
        </div>
        <div class="field">
          <label>描述</label>
          <textarea v-model="description" rows="3" :disabled="loading"></textarea>
          <div v-if="errors.description" class="err">{{ errors.description }}</div>
        </div>
        <div class="field">
          <label>优先级</label>
          <select v-model="priority" :disabled="loading">
            <option value="LOW">LOW</option>
            <option value="MEDIUM">MEDIUM (默认)</option>
            <option value="HIGH">HIGH</option>
          </select>
        </div>
        <div class="field">
          <label>截止日期(可选)</label>
          <input v-model="dueDate" type="date" :disabled="loading" />
        </div>
        <div class="row">
          <button type="submit" class="primary" :disabled="loading">{{ loading ? '提交中...' : '保存' }}</button>
          <button type="button" @click="router.push('/workitems')">取消</button>
        </div>
      </form>
    </div>
  </div>
</template>
