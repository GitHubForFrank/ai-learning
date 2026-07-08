<script setup lang="ts">
// [SDD-TASK: Task001][SDD-SPEC: 02-功能规范.md §7.2.3 + §8 FE-03]
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { WorkitemVO } from '@/types/workitem'
import { getWorkitemById, softDeleteWorkitem } from '@/api/workitem'

const props = defineProps<{ id: string }>()
const router = useRouter()
const workitem = ref<WorkitemVO | null>(null)
const notFound = ref(false)
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    workitem.value = await getWorkitemById(Number(props.id))
  } catch (e) {
    if ((e as { code?: number }).code === 1002) notFound.value = true
  } finally {
    loading.value = false
  }
}

async function onDelete() {
  if (!confirm(`确认删除工作项 #${props.id}?`)) return
  await softDeleteWorkitem(Number(props.id))
  router.push('/workitems')
}

onMounted(load)
</script>

<template>
  <div>
    <div class="row between" style="margin-bottom:14px;">
      <h2 style="margin:0;">工作项详情</h2>
      <button @click="router.push('/workitems')">返回列表</button>
    </div>

    <div v-if="loading" class="muted">加载中...</div>
    <div v-else-if="notFound" class="card">
      <p>工作项不存在或已删除。</p>
    </div>
    <div v-else-if="workitem" class="card">
      <div class="field"><label>ID</label><div>#{{ workitem.id }}</div></div>
      <div class="field"><label>标题</label><div>{{ workitem.title }}</div></div>
      <div class="field"><label>描述</label><div>{{ workitem.description || '-' }}</div></div>
      <div class="field"><label>状态</label><div><span class="badge" :class="workitem.status.toLowerCase()">{{ workitem.status }}</span></div></div>
      <div class="field"><label>优先级</label><div>{{ workitem.priority }}</div></div>
      <div class="field"><label>截止日期</label><div>{{ workitem.dueDate || '-' }}</div></div>
      <div class="field"><label>创建时间</label><div class="muted">{{ workitem.createdAt }}</div></div>
      <div class="field"><label>更新时间</label><div class="muted">{{ workitem.updatedAt }}</div></div>

      <div class="row">
        <button class="primary" @click="router.push(`/workitems/${workitem.id}/edit`)">编辑</button>
        <button class="danger" @click="onDelete">删除</button>
      </div>
    </div>
  </div>
</template>
