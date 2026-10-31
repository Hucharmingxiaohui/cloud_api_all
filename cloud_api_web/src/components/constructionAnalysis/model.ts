import type { AnalysisBatch, AnalysisPhoto, AreaResult, PhotoMetadata } from '/@/api/construction-analysis'

export const CHECKS = [
  { value: 'missing_helmet', label: '未佩戴安全帽' },
  { value: 'phone_use', label: '违规玩手机' },
  { value: 'smoking', label: '工作场所吸烟' },
  { value: 'person_fallen', label: '人员倒地' },
  { value: 'missing_safety_clothing', label: '作业中未穿着安全服' },
  { value: 'person_count', label: '人员数量统计' },
  { value: 'fire_smoke', label: '火灾烟雾' },
  { value: 'adverse_weather', label: '恶劣天气' },
  { value: 'animal_intrusion', label: '动物入侵' },
  { value: 'barrier_crossing', label: '人员违规跨栏' },
  { value: 'person_under_lifting_load', label: '塔吊吊物下方站人' },
  { value: 'missing_hook_latch', label: '防脱钩缺失' }
]
export const TYPES = [{ value: 'visible', label: '可见光' }, { value: 'infrared', label: '红外' }, { value: 'unknown', label: '未确认' }]
const labels: Record<string, string> = {
  SUBMITTING: '正在提交',
  QUEUED: '排队中',
  RUNNING: '分析中',
  SUCCEEDED: '批次已完成',
  FAILED: '批次失败',
  UNKNOWN: '状态未知，需查询确认',
  PARTIAL: '部分项目未成功 / 标注图缺失',
  success: '成功',
  unavailable: '能力未提供',
  not_applicable: '不适用',
  unassessable: '无法判断',
  failed: '执行失败',
  not_requested: '未请求'
}
export const statusText = (status: string) => labels[status] || status || '待返回'
export const checkText = (code: string) => CHECKS.find(c => c.value === code)?.label || code
export const typeText = (type: string) => TYPES.find(t => t.value === type)?.label || type
export const isRunning = (status: string) => ['SUBMITTING', 'QUEUED', 'RUNNING'].includes(status)
export function countText (area: Pick<AreaResult, 'status' | 'count'>) {
  return area.status === 'success' && typeof area.count === 'number' && Number.isInteger(area.count) && area.count >= 0 ? String(area.count) : '—'
}
export function metadata (photos: PhotoMetadata[]): PhotoMetadata[] {
  return photos.map(p => ({ id: p.id, areaId: p.areaId?.trim() || null, type: p.type, checks: [...p.checks], excluded: p.excluded }))
}
export function canAnalyze (photos: AnalysisPhoto[]) {
  const included = photos.filter(p => !p.excluded && p.checks.length > 0)
  return included.length > 0 && included.every(p => p.ready && p.checks.every(c => CHECKS.some(item => item.value === c)))
}
export function needsConfirmation (photos: AnalysisPhoto[]) {
  return photos.some(photo => !photo.excluded && photo.checks.length > 0 && !photo.ready)
}
export function canGenerateReport (batch: Pick<AnalysisBatch, 'status'> | null) {
  return batch?.status === 'SUCCEEDED'
}
export function requestId (crypto?: { randomUUID?: () => string; getRandomValues?: (array: Uint8Array) => Uint8Array }) {
  if (crypto?.randomUUID) return crypto.randomUUID()
  const bytes = new Uint8Array(16)
  if (crypto?.getRandomValues) crypto.getRandomValues(bytes)
  else for (let i = 0; i < bytes.length; i++) bytes[i] = Math.floor(Math.random() * 256)
  bytes[6] = (bytes[6] & 15) | 64; bytes[8] = (bytes[8] & 63) | 128
  const hex = Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}
export function errorText (error: unknown) {
  const e = error as { response?: { data?: { message?: string } }; message?: string }
  return e?.response?.data?.message || e?.message || '请求失败，请稍后重试'
}
