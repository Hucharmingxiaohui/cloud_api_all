import request, { IWorkspaceResponse } from '/@/api/http/request'

const HTTP_PREFIX = '/api/cqDock/eua/plan'

export interface EuaPointGroup {
  subCode: string;
  routeId: string;
  routeName: string;
  points: { id: number; pointCode: string; pointName: string }[];
}

export interface EuaPlanPayload {
  id?: number;
  pointIds: number[];
  planName: string;
  remark: string;
  enabled?: number;
}

export interface EuaPlanRow {
  id: number;
  planName: string;
  remark: string | null;
  enabled: number | null;
  pointCodes: string | null;
  pointIds: number[];
  pointCount: number;
  routeName: string | null;
  routeId: string | null;
  subCode: string | null;
  requiresPointSelection: boolean;
  lastExecuteStatus?: number;
  lastTaskName?: string;
  lastEuaTaskId?: string;
  lastExecuteTime?: string;
}

export const getEuaPlanPage = async function (data: any): Promise<IWorkspaceResponse<{ records: EuaPlanRow[]; total: number }>> {
  const result = await request.post(`${HTTP_PREFIX}/page`, data)
  return result.data
}

export const addEuaPlan = async function (data: EuaPlanPayload): Promise<IWorkspaceResponse<any>> {
  const result = await request.post(`${HTTP_PREFIX}/add`, data)
  return result.data
}

export const updateEuaPlan = async function (data: EuaPlanPayload): Promise<IWorkspaceResponse<any>> {
  const result = await request.post(`${HTTP_PREFIX}/update`, data)
  return result.data
}

export const deleteEuaPlan = async function (id: number | string): Promise<IWorkspaceResponse<any>> {
  const result = await request.post(`${HTTP_PREFIX}/delete`, undefined, { params: { id } })
  return result.data
}

export const executeEuaPlan = async function (planId: number | string): Promise<IWorkspaceResponse<any>> {
  const result = await request.post(`${HTTP_PREFIX}/execute`, { planId, plan_id: planId, id: planId })
  return result.data
}

export const getEuaPointOptions = async function (): Promise<IWorkspaceResponse<EuaPointGroup[]>> {
  const result = await request.get(`${HTTP_PREFIX}/point-options`)
  return result.data
}

export const getEuaTaskRecordPage = async function (data: any): Promise<IWorkspaceResponse<any>> {
  const result = await request.post(`${HTTP_PREFIX}/task-record/page`, data)
  return result.data
}

export const getEuaTaskPictureList = async function (data: any): Promise<IWorkspaceResponse<any>> {
  const result = await request.post(`${HTTP_PREFIX}/task-picture/list`, data)
  return result.data
}

export const getEuaTaskPictureImage = async function (id: number | string, analysis = false): Promise<Blob> {
  const result = await request.get(`${HTTP_PREFIX}/task-picture/image`, {
    params: { id, analysis }, responseType: 'blob'
  })
  return result.data
}

export interface EuaTaskReportStatus {
  status: 'NOT_GENERATED' | 'GENERATING' | 'COMPLETED' | 'FAILED'
  message?: string
  fileName?: string
}

export const generateEuaTaskReport = async function (data: any): Promise<IWorkspaceResponse<EuaTaskReportStatus>> {
  const result = await request.post(`${HTTP_PREFIX}/task-report/generate`, data)
  return normalizeReportResponse(result.data)
}

export const getEuaTaskReportStatus = async function (params: any): Promise<IWorkspaceResponse<EuaTaskReportStatus>> {
  const result = await request.get(`${HTTP_PREFIX}/task-report/status`, { params })
  return normalizeReportResponse(result.data)
}

function normalizeReportResponse (response: any): IWorkspaceResponse<EuaTaskReportStatus> {
  return {
    ...response,
    message: response.message || response.msg || '',
    data: response.data ? { ...response.data, fileName: response.data.fileName || response.data.file_name || '' } : response.data
  }
}

export const downloadEuaTaskReport = async function (params: any): Promise<Blob> {
  const result = await request.get(`${HTTP_PREFIX}/task-report/download`, { params, responseType: 'blob' })
  return result.data
}
