import { message } from 'ant-design-vue'
import request, { IPage, IWorkspaceResponse, IListWorkspaceResponse } from '/@/api/http/request'
import { TaskType, TaskStatus, OutOfControlAction } from '/@/types/task'
import { WaylineType } from '/@/types/wayline'

const HTTP_PREFIX = '/api/windTurbine'
const HTTP_PREFIX_TWO = '/api/WindTurbineWayline'
const HTTP_PREFIX_POINT = '/api/pointOfInterest'

/**
 * 风机管理
 * @param data
 * @returns
 */
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

// 生成点位
export const createWindTurbinePointsApi = async function (id:string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/addPointsById?id=${id} `
  const result = await request.get(url)
  return result.data
}

// 查询点位
export const getWindTurbinePointsApi = async function (data:any): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/getPointsById?id=${data.id}&pageSize=${data.pageSize}&page=${data.pageNo} `
  const result = await request.get(url)
  return result.data
}

/**
 * 兴趣点管理
 * @param data
 * @returns
 */
// 获取所有的风机参数
export const getAllInserestPointApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_POINT}/selectList?pointName=${data.point_name}&id=${data.id}&pageSize=${data.pageSize}&page=${data.pageNo}`
  const result = await request.get(url)
  return result.data
}

// 新增兴趣点参数配置
export const addInserestPointApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_POINT}/save`
  const result = await request.post(url, data)
  return result.data
}

// 更新兴趣点参数配置
export const updateInserestPointApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_POINT}/update`
  const result = await request.post(url, data)
  return result.data
}

// 删除兴趣点参数配置
export const deleteInserestPointApi = async function (id): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_POINT}/delete?id=${id} `
  const result = await request.get(url)
  return result.data
}

/**
 * 光伏板管理
 */
const HTTP_PREFIX_SOLAR = '/api/solarPanelArea'
const HTTP_PREFIX_SOLAR1 = '/api/solarPanel'

// 获取所有的光伏板区域参数
export const getAllSolarPanelApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_SOLAR}/selectList?solarPanelAreaName=${data.solar_panel_area_name}&id=${data.id}&pageSize=${data.pageSize}&page=${data.pageNo}`
  const result = await request.get(url)
  return result.data
}

// 新增光伏板区域配置
export const addSolarPanelConfigApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_SOLAR}/save`
  const result = await request.post(url, data)
  return result.data
}

// 更新光伏板区域配置
export const updateSolarPanelConfigApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_SOLAR}/update`
  const result = await request.post(url, data)
  return result.data
}

// 删除光伏板区域配置
export const deleteSolarPanelApi = async function (id): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_SOLAR}/delete?id=${id}`
  const result = await request.get(url)
  return result.data
}

// 根据ID查询光伏板参数
export const getSolarPanelByIdApi = async function (id): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_SOLAR}/getById?id=${id}`
  const result = await request.get(url)
  return result.data
}

// 根据ID查询光伏板位置参数
export const getSolarPanelPostionByIdApi = async function (id): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_SOLAR1}/getById?id=${id}`
  const result = await request.get(url)
  return result.data
}

/**
 * 光伏区域绘制接口
 */

// 1. 获取正射图列表
export const getOrthophotoListApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `/api/Orthophoto/selectList?pageSize=${data.pageSize}&page=${data.pageNo}`
  const result = await request.get(url)
  return result.data
}

// 2. 根据地址获取URL文件
export const getOrthophotoByUrlApi = async function (path): Promise<IWorkspaceResponse<any>> {
  const encodedPath = encodeURIComponent(path)
  const url = `/api/file/defect?path=${encodedPath}`
  const result = await request.get(url, { responseType: 'blob', })
  return result.data
}

// 3. 新增光伏区绘制
export const insertSolarPanelApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_SOLAR}/detectAreaGenSolar`
  const result = await request.post(url, data)
  return result.data
}

// 4. 导入正射图
export const importSolarPanelImgApi = async function (file): Promise<IWorkspaceResponse<any>> {
  const url = '/api/Orthophoto/import'
  const result = await request.post(url, file, {
    headers: {
      'Content-Type': 'multipart/form-data',
    }
  })
  return result.data
}

// 5. 根据id查询正射图

export const getSolarPanelImgByIdApi = async function (id): Promise<IWorkspaceResponse<any>> {
  const url = 'api/Orthophoto/selectById?id=' + id
  const result = await request.get(url)
  return result.data
}

/**
 * 光伏板设备管理接口 (巡视设备)
 */
const HTTP_PREFIX_INSPECTION = '/api/inspectionDevice'

// 获取所有的巡视设备参数
export const getAllInspectionDeviceApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_INSPECTION}/selectList?deviceName=${data.device_name}&id=${data.id}&pageSize=${data.pageSize}&page=${data.pageNo}`
  const result = await request.get(url)
  return result.data
}

// 新增巡视设备参数配置
export const addInspectionDeviceConfigApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_INSPECTION}/save`
  const result = await request.post(url, data)
  return result.data
}

// 更新巡视设备参数配置
export const updateInspectionDeviceConfigApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_INSPECTION}/update`
  const result = await request.post(url, data)
  return result.data
}

// 删除巡视设备参数配置
export const deleteInspectionDeviceApi = async function (id): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_INSPECTION}/delete?id=${id}`
  const result = await request.get(url)
  return result.data
}

// 根据ID查询巡视设备参数
export const getInspectionDeviceByIdApi = async function (id): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX_INSPECTION}/getById?id=${id}`
  const result = await request.get(url)
  return result.data
}

/**
 * 光伏区域组件管理
 */

// 1. 获取组件地理位置
export const getComponentListByIdApi = async function (id): Promise<IWorkspaceResponse<any>> {
  const url = `/api/solarPanelComponent/selectListByComponentId?componentId=${id}`
  const result = await request.get(url)
  return result.data
}

// 2. 删除正射图
export const deleteSolarImgByIdApi = async function (id): Promise<IWorkspaceResponse<any>> {
  const url = `/api/Orthophoto/deleteOrthophoto?id=${id}`
  const result = await request.get(url)
  return result.data
}

// 3. 识别正射图中的组件
export const detecSolarImgByIdApi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = '/api/solarPanel/add'
  const result = await request.post(url, data)
  return result.data
}
