import { message } from 'ant-design-vue'
import request, { IPage, IWorkspaceResponse, IListWorkspaceResponse } from '/@/api/http/request'
import { TaskType, TaskStatus, OutOfControlAction } from '/@/types/task'
import { WaylineType } from '/@/types/wayline'

const HTTP_PREFIX = 'pub/api/v1'

// 查询所有通道信息
export const getAllVideo = async function (): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/videosAddress/getAllVideosAddress`
  const result = await request.get(url)
  return result.data
}

export interface VideoInspectionData {
  id: number; // 唯一标识符
  video_name: string; // 视频名称
  sub_code: string; // 子任务代码
  workspace_id: string; // 工作空间 ID
  belonging_equipment: string; // 所属设备，如 "Airport"
  device_sn: string; // 设备序列号
  camera_type: string; // 摄像头类型，可能是 "0" 或其他
  code18: string; // 18位编码（例如设备编号）
}

// 更新视频通道
export async function updateVideo (body: VideoInspectionData): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX}/videosAddress/updateVideoAddressById`
  const result = await request.put(url, body)
  return result.data
}

// 查询所有的场站
export const getAllSub = async function (): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/pubStation/getSubStations`
  const result = await request.get(url)
  return result.data
}

// 根据场站查询设备
export const getDeviceBySub = async function (sub_code:string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/uavDevice/getUavDeviceBySubCode?sub_code=${sub_code}`
  const result = await request.get(url)
  return result.data
}

// 根据场站查询所有的设备
export const getAllDeviceBySub = async function (sub_code:string):Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX}/uavDevice/getUavDeviceBySubCode?sub_code=${sub_code}`
  const result = await request.get(url)
  return result.data
}

export interface VideoData {
    video_name: string; // 视频名称
    sub_code: string; // 子任务代码
    workspace_id: string; // 工作空间 ID
    belonging_equipment: string; // 所属设备，如 "Airport"
    device_sn: string; // 设备序列号
    camera_type: string; // 摄像头类型，可能是 "0" 或其他
    rtmpAddress: string;
  }
// 新增视频通道
export const insertvideo = async function (data:VideoData):Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX}/videosAddress/addVideoAddress`
  const result = await request.post(url, data)
  return result.data
}

// http://172.20.63.56:6789/pub/api/v1/uavDevice/getUavDeviceBySubCode?sub_code=faf3362c-3c90-2fce-0f88-b059716cb160
