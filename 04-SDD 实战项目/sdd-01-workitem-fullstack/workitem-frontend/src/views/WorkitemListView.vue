<script setup lang="ts">
// [SDD-TASK: Task001 (BR-15 鉴权前置)]
// [SDD-SPEC: 02-功能规范.md §7.2.1 + §8 FE-01]
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { WorkitemVO, WorkitemStatus, PageVO } from '@/types/workitem'
import { listWorkitemPage, softDeleteWorkitem } from '@/api/workitem'

const router = useRouter()
const page = ref<PageVO<WorkitemVO>>({ page: 1, size: 10, total: 0, totalPages: 0, list: [] })
const status = ref<WorkitemStatus | ''>('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    page.value = await listWorkitemPage({
      page: page.value.page,
      size: page.value.size,
      status: status.value || undefined,
    })
  } finally {
    loading.value = false
  }
}

function changePage(target: number) {
  if (target < 1 || target > page.value.totalPages) return
  page.value.page = target
  load()
}

watch(status, () => {
  page.value.page = 1
  load()
})

async function onDelete(id: number) {
  if (!confirm(`确认删除工作项 #${id}?(逻辑删除,可恢复)`)) return
  await softDeleteWorkitem(id)
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="row between" style="margin-bottom:14px;">
      <h2 style="margin:0;">工作项列表</h2>
      <div class="row">
        <label class="muted">筛选状态</label>
        <select v-model="status">
          <option value="">全部</option>
          <option value="TODO">TODO</option>
          <option value="DOING">DOING</option>
          <option value="DONE">DONE</option>
        </select>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" class="muted">加载中...</div>
      <div v-else-if="page.list.length === 0" class="muted">暂无工作项,去 <router-link to="/workitems/new">新建</router-link> 一个吧。</div>
      <table v-else>
        <thead>
          <tr>
            <th>ID</th><th>标题</th><th>状态</th><th>优先级</th><th>截止</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in page.list" :key="t.id">
            <td>#{{ t.id }}</td>
            <td>{{ t.title }}</td>
            <td><span class="badge" :class="t.status.toLowerCase()">{{ t.status }}</span></td>
            <td>{{ t.priority }}</td>
            <td>{{ t.dueDate || '-' }}</td>
            <td class="row" style="justify-content:flex-end;">
              <button @click="router.push(`/workitems/${t.id}`)">详情</button>
              <button @click="router.push(`/workitems/${t.id}/edit`)">编辑</button>
              <button class="danger" @click="onDelete(t.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="page.totalPages > 1" class="row" style="justify-content:center;">
      <button :disabled="page.page <= 1" @click="changePage(page.page - 1)">上一页</button>
      <span class="muted">{{ page.page }} / {{ page.totalPages }} (共 {{ page.total }} 条)</span>
      <button :disabled="page.page >= page.totalPages" @click="changePage(page.page + 1)">下一页</button>
    </div>
  </div>
</template>
