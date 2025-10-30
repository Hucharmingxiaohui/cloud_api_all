import { message } from 'ant-design-vue'
import request, { IPage, IWorkspaceResponse, IListWorkspaceResponse } from '/@/api/http/request'
import { TaskType, TaskStatus, OutOfControlAction } from '/@/types/task'
import { WaylineType } from '/@/types/wayline'

const HTTP_PREFIX = '/api/windTurbine'
const HTTP_PREFIX_TWO = '/api/WindTurbineWayline'

// 获取航线
// export const getWaylineFiles = async function (wid: string, body: {}): Promise<IWorkspaceResponse<any>> {
//   const url = `${HTTP_PREFIX}/workspaces/${wid}/waylines?order_by=${body.order_by}&page=${body.page}&page_size=${body.page_size}`
//   const result = await request.get(url)
//   return result.data
// }

// 获取所有的风机参数
export const getAllWindTurbineApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/selectList?turbineName=${data.turbine_name}&id=${data.id}&pageSize=${data.pageSize}&page=${data.pageNo}`
  const result = await request.get(url)
  return result.data
}

// 新增风机参数配置
export const addWindTurbineConfigApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/save`
  const result = await request.post(url, data)
  return result.data
}

// 更新风机参数配置
export const updateWindTurbineConfigApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/update`
  const result = await request.post(url, data)
  return result.data
}

// 删除风机参数配置
export const deleteWindTurbineApi = async function (id): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/delete?id=${id} `
  const result = await request.get(url)
  return result.data
}

// 根据风机参数，自动执行飞行任务
export const executeFlyTaskApi = async function (data: any): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_TWO}/excute`
  const result = await request.post(url, data)
  return result.data
}
