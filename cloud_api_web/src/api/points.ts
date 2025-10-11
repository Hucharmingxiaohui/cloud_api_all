import { message } from 'ant-design-vue'
import request, { IPage, IWorkspaceResponse, IListWorkspaceResponse } from '/@/api/http/request'
import { TaskType, TaskStatus, OutOfControlAction } from '/@/types/task'
import { WaylineType } from '/@/types/wayline'

const HTTP_PREFIX = '/point/api/v1'
const HTTP_PREFIX1 = '/pub/api/v1'
const HTTP_PREFIX2 = '/tem/api/v1'

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

// /home/ych/ych_ros/UAVMP/yuanma/FuJIan20241016/backup/linuxsdk/dji_thermal_sdk_v1.5_20240507/utility/bin/linux/release_x64/dji_irp
// export const insertTEMPConfig = async function (file_path: String, left_top_x:String, left_top_y:String, right_bottom_x:String, right_bottom_y:string): Promise<IWorkspaceResponse<any>> {
//   const url = `${HTTP_PREFIX1}/temPicture/getTem?file_path=${file_path}&left_top_x=${left_top_x}&left_top_y=${left_top_y}&right_bottom_x=${right_bottom_x}&right_bottom_y=${right_bottom_y}`
//   const result = await request.get(url)
//   return result.data
// }

// http://172.20.63.238:6789/tem/api/v1/workspace/getTemByWorkSpaceIdAndFileId?workspace_id=e3dea0f5-37f2-4d79-ae58-490af3228069&file_id=28d79348-55a9-4114-953f-a8abfd804d36

// export const insertTEMPConfig = async function (workspace_id:string, file_id: String, left_top_x:number, left_top_y:number, right_bottom_x:number, right_bottom_y:number): Promise<IWorkspaceResponse<any>> {
//   const url = `${HTTP_PREFIX2}/workspace/${workspace_id}/file/${file_id}/getTem?left_top_x=${left_top_x}&left_top_y=${left_top_y}&right_bottom_x=${right_bottom_x}&right_bottom_y=${right_bottom_y}`
//   const result = await request.get(url)
//   return result.data
// }

export const insertTEMPConfig = async function (workspace_id: string, file_id: String, data: RegionTem): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX2}/workspace/getTemByWorkSpaceIdAndFileId?workspace_id=${workspace_id}&file_id=${file_id}`
  const result = await request.post(url, data)
  // const result = await request.get(url, data)
  return result.data
}
// export const insertTEMPConfig = async (workspace_id:string, file_id: String, data: RegionTem): Promise<IWorkspaceResponse<any>> => {
//   const url = `${HTTP_PREFIX2}/workspace/getTemByWorkSpaceIdAndFileId?workspace_id=${workspace_id}&file_id=${file_id}`
//   const result = await request.get(url, data)
//   return result.data
// }

export const insertTEMPConfig1 = async function (workspace_id: string, file_id: String, data: PointTem): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX2}/workspace/getTemByWorkSpaceIdAndFileId?workspace_id=${workspace_id}&file_id=${file_id}`
  const result = await request.post(url, data)
  // const result = await request.get(url, data)
  return result.data
}
