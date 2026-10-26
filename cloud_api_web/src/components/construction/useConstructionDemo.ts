import { ref } from 'vue'
import type { ConstructionArea, ConstructionPlan, ConstructionData } from '../../types/construction'
import { clone, makePreviewTask, parseSavedData, seedData, validateArea, validatePlan } from './constructionModel'

// Demo adapter only. No flight, upload, algorithm or real task API is called.
export function useConstructionDemo () {
  const data = ref<ConstructionData>(seedData())
  const storageError = ref('')
  let key = 'construction-demo-v1:local'
  let writable = true
  try {
    key = `construction-demo-v1:${localStorage.getItem('workspace_id') || 'local'}`
    const raw = localStorage.getItem(key)
    if (raw) data.value = parseSavedData(raw)
  } catch (error) {
    writable = false
    storageError.value = '浏览器演示数据无法读取。当前显示内存示例，原数据未覆盖；可导出后恢复示例数据。'
  }
  function commit (next: ConstructionData) {
    if (!writable) throw new Error('演示存储不可用，请先恢复示例数据')
    try { localStorage.setItem(key, JSON.stringify(next)) } catch (error) { throw new Error('浏览器存储写入失败，修改尚未保存。请检查存储权限或空间。') }
    data.value = next
  }
  function saveArea (area: ConstructionArea) {
    const error = validateArea(area)
    if (error) throw new Error(error)
    const next = clone(data.value)
    const index = next.areas.findIndex(a => a.id === area.id)
    if (index >= 0 && next.areas[index].version !== area.version) throw new Error('区域版本已变化，请重新打开')
    const saved = { ...clone(area), name: area.name.trim(), version: index >= 0 ? area.version + 1 : 1, updatedAt: new Date().toISOString() }
    if (index >= 0) next.areas[index] = saved
    else next.areas.push(saved)
    commit(next)
    return saved
  }
  function savePlan (plan: ConstructionPlan) {
    const error = validatePlan(plan, data.value.areas)
    if (error) throw new Error(error)
    const next = clone(data.value)
    const index = next.plans.findIndex(p => p.id === plan.id)
    const saved = { ...clone(plan), name: plan.name.trim() }
    if (index >= 0) next.plans[index] = saved
    else next.plans.push(saved)
    commit(next)
    return saved
  }
  function previewTask (plan: ConstructionPlan) {
    const task = makePreviewTask(plan, data.value.areas)
    const next = clone(data.value)
    next.tasks.unshift(task)
    commit(next)
    return task
  }
  function reset () {
    const next = seedData()
    try { localStorage.setItem(key, JSON.stringify(next)) } catch (error) { throw new Error('浏览器存储不可用，无法恢复示例') }
    writable = true
    storageError.value = ''
    data.value = next
  }
  function exportRaw () { return localStorage.getItem(key) || JSON.stringify(data.value) }
  return { data, storageError, saveArea, savePlan, previewTask, reset, exportRaw }
}
