import request, { IWorkspaceResponse } from '/@/api/http/request'

const HTTP_PREFIX = '/api/cqDock/eua/plan'

export const getEuaPlanPage = async function (data: any): Promise<IWorkspaceResponse<any>> {
  const result = await request.post(`${HTTP_PREFIX}/page`, data)
  return result.data
}

export const addEuaPlan = async function (data: any): Promise<IWorkspaceResponse<any>> {
  const result = await request.post(`${HTTP_PREFIX}/add`, data)
  return result.data
}

export const updateEuaPlan = async function (data: any): Promise<IWorkspaceResponse<any>> {
  const result = await request.post(`${HTTP_PREFIX}/update`, data)
  return result.data
}

export const deleteEuaPlan = async function (id: number | string): Promise<IWorkspaceResponse<any>> {
  const result = await request.post(`${HTTP_PREFIX}/delete`, undefined, { params: { id } })
  return result.data
}

export const executeEuaPlan = async function (planId: number | string): Promise<IWorkspaceResponse<any>> {
  const result = await request.post(`${HTTP_PREFIX}/execute`, { planId })
  return result.data
}

export const getEuaBayOptions = async function (): Promise<IWorkspaceResponse<any>> {
  const result = await request.get(`${HTTP_PREFIX}/bay-options`)
  return result.data
}
