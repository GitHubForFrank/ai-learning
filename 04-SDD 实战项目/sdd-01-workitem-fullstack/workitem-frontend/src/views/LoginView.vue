<script setup lang="ts">
// [SDD-TASK: Task001]
// [SDD-SPEC: 02-功能规范.md §7.2 + §3.6 + §8 FE-07]
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginApi } from '@/api/auth'
import { setToken } from '@/utils/http'

const router = useRouter()

const username = ref('admin')
const password = ref('')
const loading = ref(false)
const errors = ref<{ username?: string; password?: string }>({})

function validate(): boolean {
  errors.value = {}
  const u = username.value.trim()
  if (u.length < 3 || u.length > 50) errors.value.username = '长度需在 3~50'
  if (!password.value) errors.value.password = '不能为空'
  return Object.keys(errors.value).length === 0
}

async function submit() {
  if (!validate()) return
  loading.value = true
  try {
    const r = await loginApi({ username: username.value.trim(), password: password.value })
    setToken(r.token)
    router.push('/workitems')
  } catch {
    // 错误 toast 由拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="max-width:360px; margin:60px auto;">
    <div class="card">
      <h2 style="margin-top:0;">登录</h2>
      <p class="muted">默认账号 admin / 默认密码 12346#@&amp; (Task001 教学用)</p>

      <form @submit.prevent="submit">
        <div class="field">
          <label>用户名</label>
          <input v-model="username" autocomplete="username" :disabled="loading" />
          <div v-if="errors.username" class="err">{{ errors.username }}</div>
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="password" type="password" autocomplete="current-password" :disabled="loading" />
          <div v-if="errors.password" class="err">{{ errors.password }}</div>
        </div>
        <button type="submit" class="primary" :disabled="loading" style="width:100%;">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
    </div>
  </div>
</template>
