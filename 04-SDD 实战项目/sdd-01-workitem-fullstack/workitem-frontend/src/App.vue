<script setup lang="ts">
// [SDD-TASK: Task001][SDD-SPEC: 02-功能规范.md §7]
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'
import { setToken, getToken } from '@/utils/http'
import { toastStore } from '@/utils/toast'

const router = useRouter()
const route = useRoute()

const showNav = computed(() => route.path !== '/login' && !!getToken())

function logout() {
  setToken(null)
  router.push('/login')
}
</script>

<template>
  <div class="layout">
    <header v-if="showNav" class="row between" style="margin-bottom:16px;">
      <div class="row" style="gap:18px;">
        <strong>sdd-01 Workitem Demo</strong>
        <router-link to="/workitems">工作项列表</router-link>
        <router-link to="/workitems/new">新建工作项</router-link>
      </div>
      <button @click="logout">登出</button>
    </header>
    <router-view />
    <div v-if="toastStore.visible" class="toast">{{ toastStore.message }}</div>
  </div>
</template>
