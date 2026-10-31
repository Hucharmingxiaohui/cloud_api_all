import type * as Api from '/@/api/construction-analysis'
import { canAnalyze, canGenerateReport, errorText, isRunning, metadata } from './model'

type Service = Pick<typeof Api, 'getTask' | 'savePhotos' | 'listBatches' | 'startBatch' | 'getBatch' | 'generateReport' | 'downloadReport'>
export function initialState () {
  return {
    taskId: '',
    loaded: false,
    photos: [] as Api.AnalysisPhoto[],
    batches: [] as Api.AnalysisBatch[],
    batch: null as Api.AnalysisBatch | null,
    saved: false,
    busy: false,
    batchBusy: false,
    reportBusy: false,
    error: '',
    pollError: '',
    pendingRequestId: '',
    selectedBatchId: ''
  }
}
export function createController (state: ReturnType<typeof initialState>, api: Service, runtime: { uuid: () => string; schedule: (callback: () => void) => unknown; cancel: (timer: any) => void }) {
  let generation = 0; let selection = 0; let timer: unknown
  const pending = new Map<string, string>()
  function stop () { selection++; if (timer !== undefined) runtime.cancel(timer); timer = undefined }
  function current (g: number, s?: number) { return g === generation && (s === undefined || s === selection) }
  function locked () { return state.busy || !!state.pendingRequestId || state.batches.some(b => isRunning(b.status) || b.status === 'UNKNOWN') }
  function accept (batch: Api.AnalysisBatch) {
    state.batch = batch; state.selectedBatchId = batch.batchId
    state.batches = [batch, ...state.batches.filter(b => b.batchId !== batch.batchId)]
    if (batch.requestId === state.pendingRequestId) { pending.delete(state.taskId); state.pendingRequestId = '' }
  }
  function schedule (g: number, s: number) {
    if (current(g, s) && state.batch && isRunning(state.batch.status)) timer = runtime.schedule(() => poll(g, s))
  }
  async function poll (g: number, s: number) {
    if (!current(g, s) || !state.batch) return
    try {
      const batch = await api.getBatch(state.taskId, state.batch.batchId)
      if (!current(g, s)) return
      accept(batch); state.pollError = ''
    } catch (error) { if (current(g, s)) state.pollError = '进度查询失败，将继续查询原批次：' + errorText(error) }
    schedule(g, s)
  }
  async function select (batchId: string) {
    stop(); const g = generation; const s = selection
    state.selectedBatchId = batchId; state.batchBusy = true; state.pollError = ''
    try {
      const batch = await api.getBatch(state.taskId, batchId)
      if (!current(g, s)) return
      accept(batch); schedule(g, s)
    } catch (error) { if (current(g, s)) { state.batch = null; state.pollError = '批次加载失败，请重新查询：' + errorText(error) } } finally { if (current(g, s)) state.batchBusy = false }
  }
  async function load (taskId: string) {
    stop(); const g = ++generation
    Object.assign(state, initialState(), { taskId, busy: true, pendingRequestId: pending.get(taskId) || '' })
    if (!taskId) { state.busy = false; return }
    try {
      const task = await api.getTask(taskId)
      if (!current(g)) return
      const batches = task.batches
      state.photos = task.photos; state.batches = batches; state.loaded = true; state.saved = true
      const found = batches.find(b => b.requestId === state.pendingRequestId)
      if (found) { pending.delete(taskId); state.pendingRequestId = '' }
      const active = found || batches.find(b => isRunning(b.status) || b.status === 'UNKNOWN') || batches[0]
      if (active) await select(active.batchId)
    } catch (error) { if (current(g)) state.error = '任务目录加载失败：' + errorText(error) } finally { if (current(g)) state.busy = false }
  }
  function edit () { if (!locked()) state.saved = false }
  async function save () {
    if (locked() || !state.loaded) return
    const g = generation; state.busy = true; state.error = ''; state.saved = false
    try {
      const task = await api.savePhotos(state.taskId, metadata(state.photos))
      if (current(g)) { state.photos = task.photos; state.saved = true }
    } catch (error) { if (current(g)) state.error = '照片配置保存失败：' + errorText(error) } finally { if (current(g)) state.busy = false }
  }
  async function start () {
    if (state.busy || !state.loaded || (!state.pendingRequestId && (locked() || !state.saved || !canAnalyze(state.photos)))) return
    const g = generation; const taskId = state.taskId
    state.busy = true; state.error = ''
    try {
      if (!state.pendingRequestId) { state.pendingRequestId = runtime.uuid(); pending.set(taskId, state.pendingRequestId) }
      const batch = await api.startBatch(taskId, state.pendingRequestId)
      if (!current(g)) return
      stop(); accept(batch); schedule(g, selection)
    } catch (error) {
      if (current(g)) state.error = '提交未确认，请先刷新历史；重试仅复用原请求标识，勿另建批次：' + errorText(error)
    } finally { if (current(g)) state.busy = false }
  }
  async function refreshHistory () {
    if (state.busy || !state.loaded) return
    const g = generation; state.busy = true; state.error = ''
    try {
      const batches = await api.listBatches(state.taskId)
      if (!current(g)) return
      state.batches = batches
      const found = batches.find(b => b.requestId === state.pendingRequestId)
      if (found) { state.pendingRequestId = ''; pending.delete(state.taskId) }
      const id = found?.batchId || state.selectedBatchId || batches[0]?.batchId
      if (id) await select(id)
    } catch (error) { if (current(g)) state.error = '历史批次刷新失败：' + errorText(error) } finally { if (current(g)) state.busy = false }
  }
  async function report () {
    if (!state.batch || state.batchBusy || state.reportBusy || !canGenerateReport(state.batch)) return
    const g = generation; const s = selection; const batch = state.batch
    state.reportBusy = true; state.error = ''
    try {
      const result = await api.generateReport(state.taskId, batch.batchId)
      if (current(g, s)) accept('batchId' in result ? result : { ...batch, reportReady: result.reportReady })
    } catch (error) { if (current(g, s)) state.error = 'Word 报告生成失败：' + errorText(error) } finally { if (current(g)) state.reportBusy = false }
  }
  async function download () {
    if (!state.batch?.reportReady || state.batchBusy || state.reportBusy) return
    const g = generation; const s = selection; state.reportBusy = true; state.error = ''
    try { const blob = await api.downloadReport(state.taskId, state.batch.batchId); if (current(g, s)) return blob } catch (error) { if (current(g, s)) state.error = 'Word 报告下载失败：' + errorText(error) } finally { if (current(g)) state.reportBusy = false }
  }
  function dispose () { generation++; stop() }
  return { load, select, edit, save, start, refreshHistory, report, download, dispose, locked }
}
