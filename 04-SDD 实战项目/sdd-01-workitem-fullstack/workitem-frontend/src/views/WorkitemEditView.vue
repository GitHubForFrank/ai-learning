<script setup lang="ts">
// [SDD-TASK: Task001][SDD-SPEC: 02-功能规范.md §7.2.4 + §8 FE-04]
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { WorkitemVO, WorkitemStatus, WorkitemPriority, WorkitemUpdateReq } from '@/types/workitem'
import { getWorkitemById, updateWorkitem } from '@/api/workitem'

const props = defineProps<{ id: string }>()
const router = useRouter()
const original = ref<WorkitemVO | null>(null)
const loading = ref(true)
const saving = ref(false)

// 表单字段(可空,Service 层 BR-05 校验"至少一个变化")
const title = ref('')
const description = ref('')
const status = ref<WorkitemStatus>('TODO')
const priority = ref<WorkitemPriority>('MEDIUM')
const dueDate = ref('')

// [SDD-SPEC: §4 BR-06] 软约束:DONE 任务 priority/dueDate 输入框 disable
const lockedFields = computed(() => original.value?.status === 'DONE')

// [SDD-SPEC: §4 BR-05 前端侧] 至少一个字段变化才能保存
const dirty = computed(() => {
  if (!original.value) return false
  return (
    title.value !== original.value.title ||
    (description.value || '') !== (original.value.description || '') ||
    status.value !== original.value.status ||
    priority.value !== original.value.priority ||
    (dueDate.value || '') !== (original.value.dueDate || '')
  )
})

async function load() {
  loading.value = true
  try {
    const t = await getWorkitemById(Number(props.id))
    original.value = t
    title.value = t.title
    description.value = t.description || ''
    status.value = t.status
    priority.value = t.priority
    dueDate.value = t.dueDate || ''
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!original.value) return
  // 只提交变化字段
  const req: WorkitemUpdateReq = {}
  if (title.value !== original.value.title) req.title = title.value
  if ((description.value || '') !== (original.value.description || '')) req.description = description.value
  if (status.value !== original.value.status) req.status = status.value
  if (!lockedFields.value) {
    if (priority.value !== original.value.priority) req.priority = priority.value
    if ((dueDate.value || '') !== (original.value.dueDate || '')) req.dueDate = dueDate.value
  }
  saving.value = true
  try {
    await updateWorkitem(Number(props.id), req)
    router.push(`/workitems/${props.id}`)
  } catch {
    // toast 已由拦截器处理(BR-06 → 1003 toast)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h2>编辑工作项 #{{ props.id }}</h2>
    <div v-if="loading" class="muted">加载中...</div>
    <div v-else class="card">
      <p v-if="lockedFields" class="muted">
        ⚠️ 当前状态为 DONE,优先级和截止日期已锁定(可改回其他状态后再调整)。
      </p>

      <form @submit.prevent="submit">
        <div class="field">
          <label>标题</label>
          <input v-model="title" :disabled="saving" />
        </div>
        <div class="field">
          <label>描述</label>
          <textarea v-model="description" rows="3" :disabled="saving"></textarea>
        </div>
        <div class="field">
          <label>状态</label>
          <select v-model="status" :disabled="saving">
            <option value="TODO">TODO</option>
            <option value="DOING">DOING</option>
            <option value="DONE">DONE</option>
          </select>
        </div>
        <div class="field">
          <label>优先级</label>
          <select v-model="priority" :disabled="saving || lockedFields">
            <option value="LOW">LOW</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HIGH">HIGH</option>
          </select>
        </div>
        <div class="field">
          <label>截止日期</label>
          <input v-model="dueDate" type="date" :disabled="saving || lockedFields" />
        </div>
        <div class="row">
          <button type="submit" class="primary" :disabled="saving || !dirty">
            {{ saving ? '保存中...' : '保存' }}
          </button>
          <button type="button" @click="router.push(`/workitems/${props.id}`)">取消</button>
        </div>
      </form>
    </div>
  </div>
</template>
