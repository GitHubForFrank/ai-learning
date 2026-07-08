// [SDD-TASK: Task001]
// [SDD-SPEC: 02-功能规范.md §1.5 + §4 BR-13 + conventions/monitoring-conventions.md §5.2]
// W3C traceparent 生成

function randomHex(bytes: number): string {
  const arr = new Uint8Array(bytes)
  crypto.getRandomValues(arr)
  return Array.from(arr, (b) => b.toString(16).padStart(2, '0')).join('')
}

/** version-traceId-spanId-flags */
export function newTraceparent(): string {
  return `00-${randomHex(16)}-${randomHex(8)}-01`
}
