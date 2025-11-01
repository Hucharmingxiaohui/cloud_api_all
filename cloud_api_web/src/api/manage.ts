import { Firmware, FirmwareQueryParam, FirmwareUploadParam } from '/@/types/device-firmware'
import request, { CommonListResponse, IListWorkspaceResponse, IPage, IWorkspaceResponse } from '/@/api/http/request'
import { Device } from '/@/types/device'
// import exp from 'constants'
import { log } from 'console'

const HTTP_PREFIX = '/manage/api/v1'
const HTTP_PREFIX1 = '/pub/api/v1'
const HTTP_PREFIX_TWO = '/fjReport/api/v1'
// login
export interface LoginBody {
 username: string,
 password: string,
 flag: number,
}
export interface BindBody {
  device_sn: string,
  user_id: string,
  workspace_id: string,
  domain?: string
}
export interface HmsQueryBody {
  sns: string[],
  children_sn: string,
  device_sn: string,
  language: string,
  level: number | string,
  begin_time: number,
  end_time: number,
  message: string,
  domain: number,
}
// 后端提交登陆
export const login = async function (body: LoginBody): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/login`
  const result = await request.post(url, body)
  return result.data
}

// Refresh Token
export const refreshToken = async function (body: {}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/token/refresh`
  const result = await request.post(url, body)
  return result.data
}

// Get Platform Info
export const getPlatformInfo = async function (): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/current`
  const result = await request.get(url)
  return result.data
}

// Get User Info
export const getUserInfo = async function (): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/users/current`
  const result = await request.get(url)
  return result.data
}

// Get Device Topo
export const getDeviceTopo = async function (workspace_id: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/devices/${workspace_id}/devices`
  const result = await request.get(url)
  return result.data
}

// Get Livestream Capacity
export const getLiveCapacity = async function (body: {}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/live/capacity`
  const result = await request.get(url, body)
  return result.data
}

// Start Livestream
export const startLivestream = async function (body: {}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/live/streams/start`
  const result = await request.post(url, body)
  return result.data
}

// Stop Livestream
export const stopLivestream = async function (body: {}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/live/streams/stop`
  const result = await request.post(url, body)
  return result.data
}
// Update Quality
export const setLivestreamQuality = async function (body: {}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/live/streams/update`
  const result = await request.post(url, body)
  return result.data
}

// user!!!
export const getAllUsersInfo = async function (wid: string, body: IPage): Promise<CommonListResponse<any>> {
  const url = `${HTTP_PREFIX}/users/${wid}/users?&page=${body.page}&page_size=${body.page_size}`
  const result = await request.get(url)
  return result.data
}

export const updateUserInfo = async function (wid: string, user_id: string, body: {}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/users/${wid}/users/${user_id}`
  const result = await request.put(url, body)
  return result.data
}

// 删除当前工作空间用户的信息
export const deleteUser = async function (wid:string, user_id:string) : Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/users/${wid}/users/${user_id}`
  const result = await request.delete(url)
  return result.data
}
// 添加一个用户
export const addUser = async function (wid:string, body:{}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/users/${wid}/users`
  const result = await request.post(url, body)
  return result.data
}
/// {workspace_id}/{user_name}
// 模糊查询当前工作空间用户
export const getOneUser = async function (wid:string, username:string, body: IPage) : Promise<CommonListResponse<any>> {
  const url = `${HTTP_PREFIX}/users/${wid}/users/${username}?&page=${body.page}&page_size=${body.page_size}`
  const result = await request.get(url)
  return result.data
}

export const bindDevice = async function (body: BindBody): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/devices/${body.device_sn}/binding`
  const result = await request.post(url, body)
  return result.data
}

export const unbindDevice = async function (device_sn: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/devices/${device_sn}/unbinding`
  const result = await request.delete(url)
  return result.data
}

export const getDeviceBySn = async function (workspace_id: string, device_sn: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/devices/${workspace_id}/devices/${device_sn}`
  const result = await request.get(url)
  return result.data
}

/**
 * 获取绑定设备信息
 * @param workspace_id
 * @param body
 * @param domain
 * @returns
 */
export const getBindingDevices = async function (workspace_id: string, body: IPage, domain: number): Promise<IListWorkspaceResponse<Device>> {
  const url = `${HTTP_PREFIX}/devices/${workspace_id}/devices/bound?&page=${body.page}&page_size=${body.page_size}&domain=${domain}`
  const result = await request.get(url)
  return result.data
}

export const updateDevice = async function (body: {}, workspace_id: string, device_sn: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/devices/${workspace_id}/devices/${device_sn}`
  const result = await request.put(url, body)
  return result.data
}

export const getUnreadDeviceHms = async function (workspace_id: string, device_sn: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/devices/${workspace_id}/devices/hms/${device_sn}`
  const result = await request.get(url)
  return result.data
}

export const updateDeviceHms = async function (workspace_id: string, device_sn: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/devices/${workspace_id}/devices/hms/${device_sn}`
  const result = await request.put(url)
  return result.data
}

export const getDeviceHms = async function (body: HmsQueryBody, workspace_id: string, pagination: IPage): Promise<IListWorkspaceResponse<any>> {
  let url = `${HTTP_PREFIX}/devices/${workspace_id}/devices/hms?page=${pagination.page}&page_size=${pagination.page_size}` +
    `&level=${body.level ?? ''}&begin_time=${body.begin_time ?? ''}&end_time=${body.end_time ?? ''}&message=${body.message ?? ''}&language=${body.language}`
  body.sns.forEach((sn: string) => {
    if (sn !== '') {
      url = url.concat(`&device_sn=${sn}`)
    }
  })
  const result = await request.get(url)
  return result.data
}

export const changeLivestreamLens = async function (body: {}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/live/streams/switch`
  const result = await request.post(url, body)
  return result.data
}

export const getFirmwares = async function (workspace_id: string, page: IPage, body: FirmwareQueryParam): Promise<IListWorkspaceResponse<Firmware>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspace_id}/firmwares?page=${page.page}&page_size=${page.page_size}` +
    `&device_name=${body.device_name}&product_version=${body.product_version}&status=${body.firmware_status ?? ''}`
  const result = await request.get(url)
  return result.data
}

export const importFirmareFile = async function (workspaceId: string, param: FormData): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}/firmwares/file/upload`
  const result = await request.post(url, param)
  return result.data
}

export const changeFirmareStatus = async function (workspaceId: string, firmwareId: string, param: {status: boolean}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}/firmwares/${firmwareId}`
  const result = await request.put(url, param)
  return result.data
}
//! !!工作空间相关路由

// 查询所有的工作空间
export const getAllWorkspaceInfo = async function (): Promise<CommonListResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/all`
  const result = await request.get(url)
  return result.data
}

// 获取所有的工作空间
export const getAllWorkspaceInfo1 = async function (user_id: string): Promise<CommonListResponse<any>> {
  const url = `${HTTP_PREFIX}/grade/getAllWorkspaces?user_id=${user_id}`
  const result = await request.get(url)
  return result.data
}

// 更新工作空间数据
export const updateWorksoaceInfo = async function (workspaceId: string, body: {}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}`
  const result = await request.put(url, body)
  return result.data
}

// 删除工作空间命令
export const deleteWorkspaceInfo = async function (workspaceId: string):Promise<CommonListResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}`
  const result = await request.delete(url)
  return result.data
}

// 按名称查询工作空间命令
export const getWorkspaceByName = async function (params: { workspaceName: string }): Promise<CommonListResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/findByName`
  const result = await request.get(url, { params })
  return result.data
}
// 创建工作空间
export const createWorkspace = async function (body:{}):Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/add`
  const result = await request.post(url, body)
  return result.data
}

/**
 * 场站管理接口
 * @param workspace_id
 * @param body
 * @param domain
 * @returns
 */

export interface insertSubBody {
  sub_name: string,
  workspace_id: string,
 }
export const addSub = async function (body:insertSubBody):Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX1}/pubStation/addSubStation`
  const result = await request.post(url, body)
  return result.data
}

// 删除变电站
export const deleteSub = async function (id: string):Promise<CommonListResponse<any>> {
  const url = `${HTTP_PREFIX1}/pubStation/deleteSubStationById?id=${id}`
  const result = await request.delete(url)
  return result.data
}

export interface updateSubBody {
  id:string,
  sub_name: string,
  workspace_id: string,
 }
// 更新变电站
export const updateSub = async function (body: updateSubBody): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX1}/pubStation/updateSubStationById`
  const result = await request.put(url, body)
  return result.data
}

/**
 * 任务分析
 */

// 分析结果
export async function startTaskAnasisyApi (data): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX_TWO}/pictureSave`
  const result = await request.post(url, data)
  return result.data
}

// 判断是否已经分析
export async function isAnalyzedApi (jobId): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX_TWO}/isAnalyzed?jobId=${jobId}`
  const result = await request.get(url)
  return result.data
}
