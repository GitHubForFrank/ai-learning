// [SDD-SPEC: 02-功能规范.md §7.3 F-06] 极简 toast(教学用,生产可换 UI 库)
import { reactive } from 'vue'

export const toastStore = reactive({
  message: '' as string,
  visible: false as boolean,
  timer: null as number | null,
})

export function showToast(message: string, duration = 3000) {
  toastStore.message = message
  toastStore.visible = true
  if (toastStore.timer != null) {
    window.clearTimeout(toastStore.timer)
  }
  toastStore.timer = window.setTimeout(() => {
    toastStore.visible = false
  }, duration)
}
