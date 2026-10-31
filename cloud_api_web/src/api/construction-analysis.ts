import request from './http/request'

export type PhotoType = 'visible' | 'infrared' | 'unknown'
export interface PhotoMetadata { id: string; areaId: string | null; type: PhotoType; checks: string[]; excluded: boolean }
export interface AnalysisPhoto extends PhotoMetadata { fileName: string; metadataSource: string; ready: boolean; originalUrl: string }
export interface CheckResult {
  code: string; status: string; count?: number | null; message?: string
  boxes?: Array<{ rect: number[]; score: number; label?: string }>; label?: string | null; score?: number
}
export interface PhotoResult { checks: CheckResult[]; image?: string | null; image_error?: string; [key: string]: unknown }
export interface BatchPhoto extends PhotoMetadata { status: string; message?: string; result: PhotoResult | null; imageUrl?: string }
export interface AreaResult { area_id: string; status: string; count: number | null; message?: string }
export interface AnalysisBatch {
  batchId: string; requestId: string; taskId: string; remoteId: string | null
  status: 'SUBMITTING' | 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'UNKNOWN'
  finished: number; total: number; message?: string; createdAt: string
  areas: AreaResult[]; photos: BatchPhoto[]; reportReady: boolean
}
export interface AnalysisTask { taskId: string; photos: AnalysisPhoto[]; batches: AnalysisBatch[] }
export interface AnalysisHealth { configured: boolean; connected: boolean; message: string }
const prefix = '/api/construction-analysis'
const taskPath = (taskId: string) => `${prefix}/tasks/${encodeURIComponent(taskId)}`
const batchPath = (taskId: string, batchId: string) => `${taskPath(taskId)}/batches/${encodeURIComponent(batchId)}`
export async function getTask (taskId: string): Promise<AnalysisTask> { return (await request.get(taskPath(taskId))).data }
export async function savePhotos (taskId: string, photos: PhotoMetadata[]): Promise<AnalysisTask> { return (await request.put(`${taskPath(taskId)}/photos`, { photos })).data }
export async function startBatch (taskId: string, requestId: string): Promise<AnalysisBatch> { return (await request.post(`${taskPath(taskId)}/batches`, { requestId })).data }
export async function listBatches (taskId: string): Promise<AnalysisBatch[]> { return (await request.get(`${taskPath(taskId)}/batches`)).data }
export async function getBatch (taskId: string, batchId: string): Promise<AnalysisBatch> { return (await request.get(batchPath(taskId, batchId))).data }
export async function health (): Promise<AnalysisHealth> { return (await request.get(`${prefix}/health`)).data }
export async function getOriginal (taskId: string, imageId: string): Promise<Blob> { return (await request.get(`${taskPath(taskId)}/photos/${encodeURIComponent(imageId)}/original`, { responseType: 'blob' })).data }
export async function getResultImage (taskId: string, batchId: string, imageId: string): Promise<Blob> { return (await request.get(`${batchPath(taskId, batchId)}/photos/${encodeURIComponent(imageId)}/result`, { responseType: 'blob' })).data }
export async function generateReport (taskId: string, batchId: string): Promise<AnalysisBatch | { reportReady: boolean }> { return (await request.post(`${batchPath(taskId, batchId)}/report`)).data }
export async function downloadReport (taskId: string, batchId: string): Promise<Blob> { return (await request.get(`${batchPath(taskId, batchId)}/report`, { responseType: 'blob' })).data }
