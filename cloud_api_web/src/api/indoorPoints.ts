import request, { IWorkspaceResponse } from '/@/api/http/request'

export interface IndoorPointRequest {
  name: string
  x: number
  y: number
  z: number
}

export interface IndoorPointResponse {
  id: string
  name: string
  x: number
  y: number
  z: number
}

export const addIndoorPoint = async (data: IndoorPointRequest): Promise<IWorkspaceResponse<IndoorPointResponse>> => {
  const result = await request.post('/api/points/add', data)
  return result.data
}

export const updateIndoorPoint = async (id: string, data: IndoorPointRequest): Promise<IWorkspaceResponse<IndoorPointResponse>> => {
  const result = await request.post(`/api/points/upt?id=${id}`, data)
  return result.data
}

export const deleteIndoorPoint = async (id: string): Promise<IWorkspaceResponse<any>> => {
  const result = await request.get(`/api/points/delete?id=${id}`)
  return result.data
}

export const getIndoorPointById = async (id: string): Promise<IWorkspaceResponse<IndoorPointResponse>> => {
  const result = await request.get(`/api/points/getById?id=${id}`)
  return result.data
}

export const getAllIndoorPoints = async (): Promise<IWorkspaceResponse<IndoorPointResponse[]>> => {
  const result = await request.get('/api/points')
  return result.data
}
