import { message } from 'ant-design-vue'
import request, { IPage, IWorkspaceResponse, IListWorkspaceResponse } from '/@/api/http/request'
import { TaskType, TaskStatus, OutOfControlAction } from '/@/types/task'
import { WaylineType } from '/@/types/wayline'

const HTTP_PREFIX = '/point/api/v1'
const HTTP_PREFIX1 = '/pub/api/v1'
const HTTP_PREFIX2 = '/tem/api/v1'

/**
 * 河北变电站点位管理
 */

// 查询点位列表
export const getPointList = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = `/api/point/selectList?page=${data.pageNo}&pageSize=${data.pageSize}&id=${data.id}&pointName=${data.pointName}&picType=${data.picType}&waylineId=${data.waylineId}`
  const result = await request.get(url)
  return result.data
}

// 导出模板文件
export const exportPointTemplate = async function (): Promise<any> {
  const url = '/api/point/export'
  const result = await request.get(url, { responseType: 'blob' })
  if (result.data.type === 'application/json') {
    const reader = new FileReader()
    reader.onload = function (e) {
      const text = reader.result as string
      const result = JSON.parse(text)
      message.error(result.message)
    }
    reader.readAsText(result.data, 'utf-8')
  } else {
    return result.data
  }
}

// 导入点位
export const importPointList = async function (file: {}): Promise<IWorkspaceResponse<any>> {
  const url = '/api/point/import'
  const result = await request.post(url, file, {
    headers: {
      'Content-Type': 'multipart/form-data',
    }
  })
  return result.data
}

// 删除点位
export const deletePointListapi = async function (data): Promise<IWorkspaceResponse<any>> {
  const url = '/api/point/batchDelete'
  const result = await request.post(url, data)
  return result.data
}

// 点位标注区域绑定
export const bindPointsApi = async function (data:any): Promise<IWorkspaceResponse<any>> {
  const url = '/tem/api/v1/workspace/bindPoint'
  const result = await request.post(url, data)
  return result.data
}
/**
 * 暂时不用
 */

// 导入台账
export interface PointData {
  area_name: string; // 区域名称
  area_id: string; // 区域ID
  bay_name: string; // 开关单元名称
  bay_id: string; // 开关单元ID
  device_name: string; // 设备名称
  device_id: string; // 设备ID
  device_type: string; // 设备类型
  component_name: string; // 组件名称
  component_type_code: string; // 组件类型代码
  phase: string; // 相别，如 A相、B相、C相等
  point_describe: string; // 点描述
  point_name: string; // 点名称
  component_id: string; // 组件ID
  point_analyse_type: string; // 点分析类型（如设备外观查看、表计读取等）
  waypoint_name: string; // 路点名称
}
export const insertPointsByXlsx = async function (sub_code: String, data: Array<PointData>): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/points/addPoints?sub_code=${sub_code}`
  const result = await request.post(url, data)
  return result.data
}

// 查询所有台账
export const getAllPoints = async function (body: IPage): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/points/all?page=${body.page}&page_size=${body.page_size}`
  const result = await request.get(url)
  return result.data
}

// 按照id删除台账
// 删除媒体
export const deletePoint = async function (id: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/points/deletePointById?id=${id}`
  const result = await request.delete(url)
  return result.data
}

// 按场站编码查询台账
export const getPointsBySub = async function (sub_code: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/points/getPointBySubCode?sub_code=${sub_code}`
  const result = await request.get(url)
  return result.data
}

// 查询区域通过场站编码
export const getAreaBySub = async function (sub_code: string): Promise<IWorkspaceResponse<any>> {
  const url = `/pub/api/v1/area/getAreasBySubCode?sub_code=${sub_code}`
  const result = await request.get(url)
  return result.data
}

// 查询区域通过场站编码
export const getBayByAreaId = async function (area_id: string): Promise<IWorkspaceResponse<any>> {
  const url = `/pub/api/v1/bay/getBaysByAreaId?area_id=${area_id}`
  const result = await request.get(url)
  return result.data
}

// 查询设备通过间隔编码
export const getDeviceByBayId = async function (bay_id: string): Promise<IWorkspaceResponse<any>> {
  const url = `/pub/api/v1/device/getDevicesByBayId?bay_id=${bay_id}`
  const result = await request.get(url)
  return result.data
}

// 查询部位通过设备编码
export const getComponentByDeviceId = async function (device_id: string): Promise<IWorkspaceResponse<any>> {
  const url = `/pub/api/v1/component/getComponentsByDeviceId?device_id=${device_id}`
  const result = await request.get(url)
  return result.data
}

// 查询点位通过部位编码
export const getPointsByComponentId = async function (component_id: string): Promise<IWorkspaceResponse<any>> {
  const url = `/point/api/v1/points/getPointsByComponentId?component_id=${component_id}`
  const result = await request.get(url)
  return result.data
}

// 查询所有场站的信息
// http://172.20.63.56:6789/pub/api/v1/pubStation/getSubStations
export const getAllSub = async function (): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX1}/pubStation/getSubStations`
  const result = await request.get(url)
  return result.data
}

// 编辑台账
export interface PointDetailData {
  id: number; // 唯一标识符
  point_code: string; // 点编码
  sub_code: string; // 子编码
  area_name: string; // 区域名称
  area_id: string; // 区域ID
  bay_name: string; // 开关单元名称
  bay_id: string; // 开关单元ID
  device_name: string; // 设备名称
  device_id: string; // 设备ID
  device_type: string; // 设备类型
  component_name: string; // 组件名称
  component_type_code: string; // 组件类型代码
  phase: string; // 相别，如 A相、B相、C相等
  point_describe: number; // 点描述，数字类型（可能代表状态码、等级等）
  point_name: string; // 点名称
  component_id: string; // 组件ID
  point_analyse_type: string; // 点分析类型（如设备外观查看、表计读取等）
  waypoint_name: string; // 路点名称
  wayline_id: string; // 路线ID
  waypoint_sequence: string; // 路点序列（如"1-1-1"表示路径中的第一个路点）
  tem_type: number; // 温度类型，数字类型（可能表示不同的温度设定或状态）
  tem_conf: string; // 温度配置（可能表示温度的具体配置或设置）
}

export async function updatePoints (body: PointDetailData): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX}/points/update`
  const result = await request.put(url, body)
  return result.data
}
// 红外测温
interface RegionTem {
  left_top_x: number; // 可以是数字或字符串，具体根据实际需要调整
  left_top_y: number;
  right_bottom_x: number;
  right_bottom_y: number;
}
interface PointTem {
  point_x: number;
  point_y: number;
}

export const insertTEMPConfig = async function (workspace_id: string, file_id: String, data: RegionTem): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX2}/workspace/getTemByWorkSpaceIdAndFileId?workspace_id=${workspace_id}&file_id=${file_id}`
  const result = await request.post(url, data)
  // const result = await request.get(url, data)
  return result.data
}

export const insertTEMPConfig1 = async function (workspace_id: string, file_id: String, data: PointTem): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX2}/workspace/getTemByWorkSpaceIdAndFileId?workspace_id=${workspace_id}&file_id=${file_id}`
  const result = await request.post(url, data)
  // const result = await request.get(url, data)
  return result.data
}
