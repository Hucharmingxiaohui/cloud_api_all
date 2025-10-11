import { message } from 'ant-design-vue'
import request, { IPage, IWorkspaceResponse, IListWorkspaceResponse } from '/@/api/http/request'

const HTTP_PREFIX = 'control/api/v1'

export interface controlAuth {
    user_id: string;
    user_callsign: string;
  }
export interface controlData extends controlAuth {
    control_keys: string[];
}

// 申请负载操作权限
export const requestControlAuto = async function (gatewaySn: string, deviceSn:string, method:string, PilotRequest: controlAuth): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/pilotDevices/getCloudControlPermission?gatewaySn=${gatewaySn}&deviceSn=${deviceSn}&method=${method}`
  const result = await request.post(url, PilotRequest)
  return result.data
}

// 释放负载操作权限
export const deleteControlAuto = async function (gatewaySn: string, deviceSn:string, method:string, body: controlData): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/pilotDevices/releaseCloudControlPermission?gatewaySn=${gatewaySn}&deviceSn=${deviceSn}&method=${method}`
  const result = await request.post(url, body)
  return result.data
}

export interface videoType {
  video_id: string;
  video_type: string;
}
// 切换相机类型  变焦  红外
export const changeVideoType = async function (gatewaySn: string, deviceSn:string, method:string, body: videoType): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/pilotDevices/liveLensChange?gatewaySn=${gatewaySn}&deviceSn=${deviceSn}&method=${method}`
  const result = await request.post(url, body)
  return result.data
}
// 切换相机模式  照相，全景照相
export interface cameraType {
  camera_mode: string;
  payload_index: string;
}
export const changeCameraMode = async function (gatewaySn: string, deviceSn:string, method:string, body: cameraType): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/pilotDevices/cameraModeSwitch?gatewaySn=${gatewaySn}&deviceSn=${deviceSn}&method=${method}`
  const result = await request.post(url, body)
  return result.data
}

// 开始录像/停止录像
export interface cameraRecording {
  payload_index: string;
}

export const cameraRecording = async function (gatewaySn: string, deviceSn:string, method:string, body: cameraType): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/pilotDevices/cameraRecordingStart?gatewaySn=${gatewaySn}&deviceSn=${deviceSn}&method=${method}`
  const result = await request.post(url, body)
  return result.data
}

// 开始拍照/停止拍照
export const cameraPhotoTake = async function (gatewaySn: string, deviceSn:string, method:string, body: cameraType): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/pilotDevices/cameraPhotoTake?gatewaySn=${gatewaySn}&deviceSn=${deviceSn}&method=${method}`
  const result = await request.post(url, body)
  return result.data
}

// 云台操作
export interface PTZType {
  locked: boolean;
  payload_index: string;
  pitch_speed:number;
  yaw_speed:number;
}
export const PTZControl = async function (gatewaySn: string, deviceSn:string, method:string, body: PTZType): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/pilotDevices/cameraScreenDrag?gatewaySn=${gatewaySn}&deviceSn=${deviceSn}&method=${method}`
  const result = await request.post(url, body)
  return result.data
}
export interface PTZResetType {
  payload_index: string;
  reset_mode: number;
}
export const PTZReset = async function (gatewaySn: string, deviceSn:string, method:string, body: PTZResetType): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/pilotDevices/gimbalReset?gatewaySn=${gatewaySn}&deviceSn=${deviceSn}&method=${method}`
  const result = await request.post(url, body)
  return result.data
}

// 相机变焦
export interface FocalLengthType {
  camera_type: string;
  payload_index: string;
  zoom_factor:number;
}
export const FocalLengthSet = async function (gatewaySn: string, deviceSn:string, method:string, body: FocalLengthType): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/pilotDevices/cameraFocalLengthSet?gatewaySn=${gatewaySn}&deviceSn=${deviceSn}&method=${method}`
  const result = await request.post(url, body)
  return result.data
}
// 曝光值设置 模式设置
export interface ExposureType {
  camera_type: string;
  payload_index: string;
  exposure_value:number;
}
export const exposureValueSet = async function (gatewaySn: string, deviceSn:string, method:string, body: FocalLengthType): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/pilotDevices/cameraExposureSet?gatewaySn=${gatewaySn}&deviceSn=${deviceSn}&method=${method}`
  const result = await request.post(url, body)
  return result.data
}
export interface ExposureMode {
  camera_type: string;
  payload_index: string;
  exposure_mode:number;
}
export const exposureModeSet = async function (gatewaySn: string, deviceSn:string, method:string, body: FocalLengthType): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/pilotDevices/camera_exposure_mode_set?gatewaySn=${gatewaySn}&deviceSn=${deviceSn}&method=${method}`
  const result = await request.post(url, body)
  return result.data
}
