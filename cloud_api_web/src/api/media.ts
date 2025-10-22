import { message } from 'ant-design-vue'
import request, { CommonListResponse, IPage, IWorkspaceResponse } from '/@/api/http/request'
const HTTP_PREFIX = '/media/api/v1'

// Get Media Files
export const getMediaFiles = async function (wid: string, pagination: IPage): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/files/${wid}/files?page=${pagination.page}&page_size=${pagination.page_size}`
  const result = await request.get(url)
  return result.data
}
// Download Media File
export const downloadMediaFile = async function (workspaceId: string, fileId: string): Promise<any> {
  const url = `${HTTP_PREFIX}/files/${workspaceId}/file/${fileId}/url`
  const result = await request.get(url, { responseType: 'blob' })
  if (result.data.type === 'application/json') {
    const reader = new FileReader()
    reader.onload = function (e) {
      const text = reader.result as string
      const result = JSON.parse(text)
      message.error(result.message)
    }
    reader.readAsText(result.data, 'utf-8')
    console.log('json')
  } else {
    console.log('nojson')
    return result.data
  }
}

export const downloadThumbnail = async function (workspaceId: string, fileId: string): Promise<any> {
  const url = `${HTTP_PREFIX}/files/${workspaceId}/file/${fileId}/getthumbnail`
  const result = await request.get(url, { responseType: 'blob' })
  if (result.data.type === 'application/json') {
    const reader = new FileReader()
    reader.onload = function (e) {
      const text = reader.result as string
      const result = JSON.parse(text)
      message.error(result.message)
    }
    reader.readAsText(result.data, 'utf-8')
    console.log('json')
  } else {
    console.log('nojson')
    return result.data
  }
}

// 模糊查询当前图片
export const getOneImage = async function (wid:string, fileName:string, pagination: IPage) : Promise<CommonListResponse<any>> {
  const url = `${HTTP_PREFIX}/files/${wid}/files/${fileName}?page=${pagination.page}&page_size=${pagination.page_size}`
  const result = await request.get(url)
  return result.data
}

// 删除媒体
export const deleteOneImage = async function (wid:string, fileId: string) : Promise<CommonListResponse<any>> {
  const url = `${HTTP_PREFIX}/files/${wid}/files/${fileId}`
  const result = await request.delete(url)
  return result.data
}

// 根据任务id和航线id查询结果
export async function getTaskResultById (job_id: string, workspace_id:string, wayline_id:string): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX}/files/getMediaDileByJobId?job_id=${job_id}&workspace_id=${workspace_id}&wayline_id=${wayline_id}`
  const result = await request.get(url)
  return result.data
}

// 根据任务id和航线id查询结果飞行结果 改
export async function getFlyTaskResultApi (job_id: string, workspace_id:string, wayline_id:string): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX}/files/getMediaFileByJobId?job_id=${job_id}&workspace_id=${workspace_id}&wayline_id=${wayline_id}`
  const result = await request.get(url)
  return result.data
}

// 测试接口   根据worckspaceId 和fileId生成缩略图
export async function getThumbnailById (file_id: string, workspace_id:string): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX}/files/getThumbnailByJobId?file_id=${file_id}&workspace_id=${workspace_id}`
  const result = await request.get(url)
  return result.data
}
