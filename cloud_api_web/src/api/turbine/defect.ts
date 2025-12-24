import { message } from 'ant-design-vue'
import request, { IPage, IWorkspaceResponse, IListWorkspaceResponse } from '/@/api/http/request'
import { TaskType, TaskStatus, OutOfControlAction } from '/@/types/task'
import { WaylineType } from '/@/types/wayline'

const HTTP_PREFIX = '/api/file'

// 获取缺陷类型字典
export const getDefectTypeMapApi = async function (): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/queryDefectType`
  const result = await request.get(url)
  return result.data
}

// 更新缺陷类型
export const updateDefectTypeApi = async function (data:any): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/updateDefect`
  const result = await request.post(url, data)
  return result.data
}
