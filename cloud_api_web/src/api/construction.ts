import request from './http/request'

export interface ConstructionPoint { lng: number; lat: number }
export interface ConstructionOverallBinding { area_id: string; area_version: number; area_name?: string; waypoints: number[] }
export interface ConstructionOverallRoute {
  needs_preparation?: boolean; preparation_points?: number[]; source_wayline_id?: string; source_sign?: string; prepared_sign?: string
  overall_id: string; version: number; wayline_id: string; name: string; dock_sn: string; file_sign: string
  points: Array<ConstructionPoint & { height: number; index: number; photo_suffixes: string[] }>
  height_mode: string; bindings: ConstructionOverallBinding[]; status?: string; message?: string
}

export interface DockCoverage { dock_sn: string; radius_m: number | null }
export async function getDockCoverage (sn: string) {
  return unwrapArea<DockCoverage>(await request.get(`/api/construction/docks/${encodeURIComponent(sn)}/coverage`), '覆盖范围加载失败')
}
export async function saveDockCoverage (sn: string, radius: number) {
  return unwrapArea<DockCoverage>(await request.put(`/api/construction/docks/${encodeURIComponent(sn)}/coverage`, { radius_m: radius }), '覆盖范围保存失败')
}

export interface ConstructionAnalysis {
  batch_id?: string
  status?: string
  message?: string
  result?: Record<string, unknown>
}
export interface ConstructionResult {
  transfer_source?: { deployment: string; workspace: string; job_id: string }
  association_revision?: number
  unresolved_count?: number
  analysis_stale?: boolean
  job_id: string
  name: string
  plan_type: number
  expected_count: number | null
  received_count: number
  media_complete: boolean
  analysis_configured: boolean
  analysis: ConstructionAnalysis
  overall_route?: Partial<ConstructionOverallRoute>
  photos: Array<{ file_id: string; name: string; archived: boolean; association?: string; association_message?: string; waypoint_index?: number; area_id?: string; area_name?: string; excluded?: boolean }>
}
const base = (jobId: string) => `/api/construction/tasks/${encodeURIComponent(jobId)}`
function unwrap<T> (response: { data: { code: number; data: T; message?: string } }): T {
  if (response.data.code !== 0) throw new Error(response.data.message || '工地任务请求失败')
  return response.data.data
}
export async function getConstructionResult (jobId: string) { return unwrap<ConstructionResult>(await request.get(base(jobId))) }
export interface ConstructionTransferTask {
  job_id: string; name: string; dock_sn?: string; execute_time?: number; end_time?: number; photo_count: number; imported_at: number
  origin: { deployment: string; workspace: string; job_id: string }
}
export interface ConstructionTransfers {
  deployment: string; inbox: string; exports: string; tasks: ConstructionTransferTask[]
  receipts?: Array<{ file: string; status: string; message: string; job_id?: string }>
}
export async function getConstructionTransfers () { return unwrap<ConstructionTransfers>(await request.get('/api/construction/transfers')) }
export async function scanConstructionTransfers () { return unwrap<ConstructionTransfers>(await request.post('/api/construction/transfers/scan', null, { timeout: 600000 })) }
export async function exportConstructionTask (jobId: string) { return unwrap<{ path: string; name: string }>(await request.post(`${base(jobId)}/export`, null, { timeout: 600000 })) }
export async function archiveConstructionPhotos (jobId: string) { return unwrap<ConstructionResult>(await request.post(`${base(jobId)}/archive`)) }
export async function assignConstructionPhotos (jobId: string, body: { file_ids: string[]; expected_revision: number; area_id?: string; excluded: boolean; reason?: string }) {
  return unwrap<ConstructionResult>(await request.post(`${base(jobId)}/photo-associations`, body))
}
export async function checkConstructionHost (jobId: string) { return unwrap<{ connected: boolean }>(await request.post(`${base(jobId)}/analysis/check`)) }
export async function submitConstructionAnalysis (jobId: string, requestId: string) { return unwrap<ConstructionAnalysis>(await request.post(`${base(jobId)}/analysis`, { request_id: requestId })) }
export async function refreshConstructionAnalysis (jobId: string) { return unwrap<ConstructionAnalysis>(await request.post(`${base(jobId)}/analysis/refresh`)) }
export async function getConstructionPhoto (jobId: string, fileId: string) {
  const response = await request.get(`${base(jobId)}/media/${encodeURIComponent(fileId)}`, { responseType: 'blob' })
  return response.data as Blob
}

// ---- 施工区域（真实标定） ----

export interface ConstructionAreaRecord {
  id: string
  name: string
  category: string
  enabled: boolean
  notes: string | null
  version: number
  archived: boolean
  gmt_create?: string
  gmt_modified?: string
  boundary?: ConstructionPoint[]
  area_sqm?: number
}
export interface ConstructionAreaVersionRecord {
  id: string
  version: number
  area_sqm: number
  gmt_create: string
}
export class ConstructionAreaConflictError extends Error {}
const areasBase = '/api/construction/areas'
function unwrapArea<T> (response: { data: { code: number; data: T; message?: string } }, fallback: string): T {
  if (response.data.code === 409) throw new ConstructionAreaConflictError(response.data.message || fallback)
  if (response.data.code !== 0) throw new Error(response.data.message || fallback)
  return response.data.data
}
export async function listConstructionAreas (params: { keyword?: string; enabled?: boolean | null; archived?: boolean } = {}) {
  return unwrapArea<ConstructionAreaRecord[]>(await request.get(areasBase, { params }), '区域列表加载失败')
}
export async function getConstructionArea (id: string) {
  return unwrapArea<ConstructionAreaRecord>(await request.get(`${areasBase}/${encodeURIComponent(id)}`), '区域详情加载失败')
}
export async function createConstructionArea (body: { name: string; category: string; enabled: boolean; notes: string; boundary: ConstructionPoint[] }) {
  return unwrapArea<ConstructionAreaRecord>(await request.post(areasBase, body), '区域保存失败')
}
export async function updateConstructionArea (id: string, body: { name: string; category: string; enabled: boolean; notes: string; boundary: ConstructionPoint[]; expected_version: number }) {
  return unwrapArea<ConstructionAreaRecord>(await request.put(`${areasBase}/${encodeURIComponent(id)}`, body), '区域保存失败')
}
export async function constructionAreaHistory (id: string) {
  return unwrapArea<ConstructionAreaVersionRecord[]>(await request.get(`${areasBase}/${encodeURIComponent(id)}/versions`), '版本历史加载失败')
}
export async function archiveConstructionArea (id: string, archived: boolean) {
  return unwrapArea<null>(await request.post(`${areasBase}/${encodeURIComponent(id)}/archive`, null, { params: { archived } }), '归档操作失败')
}

export interface ConstructionWayline {
  wayline_id: string; name: string; source: string; status: string; message: string
  area_version: number; points?: Array<ConstructionPoint & { height: number }>; height_mode?: string
}
const routesBase = (id: string) => `${areasBase}/${encodeURIComponent(id)}/waylines`
export async function bindConstructionWayline (id: string, version: number, waylineId: string) {
  return unwrapArea<ConstructionWayline>(await request.post(routesBase(id) + '/bind', { expected_version: version, wayline_id: waylineId }), '航线关联失败')
}
export async function listConstructionWaylines (id: string) {
  return unwrapArea<ConstructionWayline[]>(await request.get(routesBase(id)), '航线列表加载失败')
}
export async function getConstructionWayline (id: string, wayline: string) {
  return unwrapArea<ConstructionWayline>(await request.get(`${routesBase(id)}/${encodeURIComponent(wayline)}`), '航线加载失败')
}
export async function uploadConstructionWayline (id: string, version: number, file: File, requestId: string) {
  const data = new FormData()
  data.append('file', file); data.append('expected_version', String(version)); data.append('request_id', requestId)
  return unwrapArea<ConstructionWayline>(await request.post(routesBase(id) + '/upload', data, { headers: { 'Content-Type': 'multipart/form-data' } }), '航线上传失败')
}
export async function createConstructionWayline (id: string, version: number, wayline: unknown, requestId: string) {
  return unwrapArea<ConstructionWayline>(await request.post(routesBase(id), { expected_version: version, request_id: requestId, wayline }), '航线创建失败')
}
export async function validateConstructionWayline (id: string, wayline: string) {
  return unwrapArea<ConstructionWayline>(await request.post(`${routesBase(id)}/${encodeURIComponent(wayline)}/validate`), '航线校验失败')
}
export async function unlinkConstructionWayline (id: string, wayline: string) {
  return unwrapArea<null>(await request.delete(`${routesBase(id)}/${encodeURIComponent(wayline)}`), '解除关联失败')
}

export interface ConstructionPlan {
  overall_id?: string; overall_route?: ConstructionOverallRoute; bindings?: ConstructionOverallBinding[]
  id: number; plan_id: string; name: string; file_id: string; dock_sn: string
  area_id: string; area_version: number; task_type: number; begin_time: number
  rth_altitude: number; out_of_control: number; archived: number | boolean
  status: string; message: string; create_time: number
  last_dispatch?: { status: string; job_id?: string; message?: string }
}
export interface ConstructionPlanInput {
  name: string; file_id: string; dock_sn: string; task_type: number; begin_time?: number
  rth_altitude: number; out_of_control: number; expected_version: number; request_id: string
}
export async function listConstructionPlans (area: string, archived = false) {
  return unwrapArea<ConstructionPlan[]>(await request.get(`${areasBase}/${encodeURIComponent(area)}/plans`, { params: { archived } }), '计划列表加载失败')
}
export async function createConstructionPlan (area: string, body: ConstructionPlanInput) {
  return unwrapArea<ConstructionPlan>(await request.post(`${areasBase}/${encodeURIComponent(area)}/plans`, body), '计划保存失败')
}
export async function dispatchConstructionPlan (id: string, requestId: string) {
  return unwrapArea<{ status: string; job_id?: string; message: string }>(await request.post(`/api/construction/plans/${encodeURIComponent(id)}/dispatch`, { request_id: requestId }), '计划下发失败')
}
export async function archiveConstructionPlan (id: string) {
  return unwrapArea<null>(await request.post(`/api/construction/plans/${encodeURIComponent(id)}/archive`), '计划归档失败')
}
export async function refreshConstructionDispatch (id: string) {
  return unwrapArea<ConstructionPlan>(await request.post(`/api/construction/plans/${encodeURIComponent(id)}/refresh-dispatch`), '下发状态查询失败')
}

const overallBase = '/api/construction/overall-routes'
export async function listConstructionOverallRoutes (sn: string) {
  return unwrapArea<ConstructionOverallRoute[]>(await request.get(overallBase, { params: { dock_sn: sn } }), '总体航线加载失败')
}
export async function inspectConstructionOverallRoute (sn: string, id: string) {
  return unwrapArea<ConstructionOverallRoute>(await request.get(overallBase + '/inspect', { params: { dock_sn: sn, wayline_id: id } }), '总体航线解析失败')
}
export async function prepareConstructionOverallRoute (sn: string, id: string, sign: string) {
  return unwrapArea<ConstructionOverallRoute>(await request.post(overallBase + '/prepare', { dock_sn: sn, wayline_id: id, file_sign: sign }), '执行版航线生成失败')
}
export async function saveConstructionOverallRoute (body: { dock_sn: string; wayline_id: string; file_sign: string; expected_version: number; bindings: ConstructionOverallBinding[] }) {
  return unwrapArea<ConstructionOverallRoute>(await request.post(overallBase, body), '总体航线绑定保存失败')
}
export async function listConstructionOverallPlans (id: string, archived = false) {
  return unwrapArea<ConstructionPlan[]>(await request.get(`${overallBase}/${encodeURIComponent(id)}/plans`, { params: { archived } }), '总体计划加载失败')
}
export async function createConstructionOverallPlan (id: string, body: ConstructionPlanInput) {
  return unwrapArea<ConstructionPlan>(await request.post(`${overallBase}/${encodeURIComponent(id)}/plans`, body), '总体计划保存失败')
}
