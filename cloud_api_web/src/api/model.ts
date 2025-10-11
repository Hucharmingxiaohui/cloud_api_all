import { message } from 'ant-design-vue'
import request, { IPage, IWorkspaceResponse, IListWorkspaceResponse } from '/@/api/http/request'
import { TaskType, TaskStatus, OutOfControlAction } from '/@/types/task'
import { WaylineType } from '/@/types/wayline'

const HTTP_PREFIX = '/pub/api/v1'

// 导入模型文件
export const importModelFile = async function (workspaceId: string, file: {}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/importModel/workspaces/${workspaceId}/file/upload`
  const result = await request.post(url, file, {
    headers: {
      'Content-Type': 'multipart/form-data',
    }
  })
  return result.data
}

// 按模型名称查询数据
export async function getModelInfoByName (modelName: string): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX}/modelName/${modelName}/getModelInfo`
  const result = await request.get(url)
  return result.data
}

// 查询所有的模型
export const getAllModels = async function (body: IPage): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/models/all?page=${body.page}&page_size=${body.page_size}`
  const result = await request.get(url)
  return result.data
}

export const deleteModel = async function (workspaceId: string, id: number): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}/deleteModelById?id=${id}`
  const result = await request.delete(url)
  return result.data
}
