import { message } from 'ant-design-vue'
import request, { IPage, IWorkspaceResponse, IListWorkspaceResponse } from '/@/api/http/request'
import { TaskType, TaskStatus, OutOfControlAction } from '/@/types/task'
import { WaylineType } from '/@/types/wayline'

const HTTP_PREFIX = '/wayline/api/v1'
const HTTP_PREFIX1 = '/media/api/v1'
const HTTP_PREFIX2 = '/mqtt/v1'
const HTTP_PREFIX3 = '/pub/api/v1'

// 获取航线
export const getWaylineFiles = async function (wid: string, body: {}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${wid}/waylines?order_by=${body.order_by}&page=${body.page}&page_size=${body.page_size}`
  const result = await request.get(url)
  return result.data
}

// 搜索航线
export const searchWaylineFiles = async function (wid: string, body: {}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${wid}/waylines/${body.search_value}?order_by=${body.order_by}`
  const result = await request.get(url)
  return result.data
}
// 下载航线
export const downloadWaylineFile = async function (workspaceId: string, waylineId: string): Promise<any> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}/waylines/${waylineId}/url`
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

// 删除航线
export const deleteWaylineFile = async function (workspaceId: string, waylineId: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}/waylines/${waylineId}`
  const result = await request.delete(url)
  return result.data
}
// 查询航线站点信息,可视化航线
export const gethWaylineInfo = async function (wid: string, waylineId: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${wid}/waylines/${waylineId}/llPoint`
  const result = await request.get(url)
  return result.data
}

// 查询航点坐标信息
export const getWayPointInfo = async function (wid: string, waylineId: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${wid}/waylines/${waylineId}/xyzPoint`
  const result = await request.get(url)
  return result.data
}

// 解析航线
// http://172.20.63.56:6789/wayline/api/v1/thirdKmz/:workspace_id/waylines/:wayline_id/getKmzWaypointWayLineInfo

export const getWaypoints = async function (wid: string, waylineId: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/thirdKmz/${wid}/waylines/${waylineId}/getKmzWaypointWayLineInfo`
  const result = await request.get(url)
  return result.data
}

// 查询航线站点信息,编辑航线
export const editWaylineInfo = async function (wid: string, waylineId: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${wid}/waylines/${waylineId}/getKmzWaypointWayLineInfo`
  const result = await request.get(url)
  return result.data
}

// export interface Coordinates {
//   coordinates: string,
// }
// 定义 ActionGroup 接口
export interface ActionGroup {
  actionActuatorFunc: string;
}

// 定义 PlaceMark 接口
export interface PlaceMark {
  height: number;
  waypointHeadingAngle: number;
  gimbalPitchAngle: number;
  waypointSpeed: number;
  coordinates: string;
  actionGroup: ActionGroup[];
}

// 定义 DroneInfo 接口
export interface DroneInfo {
  droneEnumValue: number;
  droneSubEnumValue: number;
}

// 定义 PayloadInfo 接口
export interface PayloadInfo {
  payloadEnumValue: number;
  payloadSubEnumValue: number;
  payloadPositionIndex: number;
}

// 定义 MissionConfig 接口
export interface MissionConfig {
  finishAction: string;
  takeOffSecurityHeight: number;
  globalTransitionalSpeed: number;
  droneInfo: DroneInfo;
  payloadInfo: PayloadInfo;
  fileName: string;
}

// 定义 Folder 接口
export interface Folder {
  autoFlightSpeed: number;
  globalHeight: number;
  globalWaypointTurnMode: string;
  placeMarks: PlaceMark[];
}

// 定义数据结构接口
export interface Data {
  missionConfig: MissionConfig;
  folder: Folder;
}
// 制作航线，M3E提交
export const commitWaylineFile = async function (workspace_id: string, data: Data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspace_id}/waylines/wayPointRoutePlanning?creator=pilot`
  const result = await request.post(url, data)
  return result.data
}

// 定义 ActionTrigger 接口
export interface ActionTrigger {
  actionTriggerType: string;
  actionTriggerParam: number;
}

// 定义 ActionActuatorFuncParam 接口
export interface ActionActuatorFuncParam {
  takePhoto: Record<string, unknown>; // 动作参数可以更具体
}

// 定义 Action 接口
export interface Action {
  actionActuatorFunc: string;
  actionActuatorFuncParam: ActionActuatorFuncParam;
}

// 定义 ActionGroup 接口
export interface ActionGroup1 {
  actionGroupStartIndex: number;
  actionGroupEndIndex: number;
  actionTrigger: ActionTrigger;
  actionList: Action[];
}
// 定义 PlaceMark 接口
export interface PlaceMark1 {
  point: {
    coordinates: string;
  };
  ellipsoidHeight: string;
  height: number;
  waypointSpeed: number;
  waypointHeadingParam: {
    waypointHeadingAngle: number;
  };
  waypointTurnParam: string; // 可更详细
  gimbalPitchAngle: number;
  actionGroup: ActionGroup1;
}
// 定义 MissionConfig 接口
export interface MissionConfig1 {
  fileName: string;
  finishAction?: string; // 可选属性
  takeOffSecurityHeight: number;
  globalTransitionalSpeed: number;
  droneInfo: DroneInfo;
  payloadInfo: PayloadInfo;
}
// 定义 Folder 接口
export interface Folder1 {
  autoFlightSpeed: number;
  globalHeight: number;
  globalWaypointTurnMode: string;
  placeMarks: PlaceMark1[];
  payloadParam: Record<string, unknown>; // 可更详细
}

// 定义 Data 接口
export interface M30Data {
  missionConfig: MissionConfig1;
  folder: Folder1;
}
// 制作航线，M30T提交

// 制作航线，M3E提交
export const commitWaylineFile1 = async function (workspace_id: string, data: M30Data): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspace_id}/waylines/kmzWayPointRoutePlanning?creator=pilot`
  const result = await request.post(url, data)
  return result.data
}

// 通过任务id 查询图片
// export const getTaskResult = async function (wid: string, job_id: string, start_time: Number, end_time: Number): Promise<IWorkspaceResponse<any>> {
//   const url = `${HTTP_PREFIX1}/files/${wid}/files/${job_id}/jobIdUrl?page=1&page_size=1000&start_time=${start_time}&end_time=${end_time}`
//   const result = await request.get(url)
//   return result.data
// }
export interface InspectionTask {
  sub_code: string; // 任务的子代码
  major: string; // 任务类型，如 "变电"
  plan_source: string; // 任务来源，如 "系统创建"
  name: string; // 任务名称，如 "红外绝缘子巡检"
  file_id: string; // 文件 ID
  dock_sn: string; // 对接序列号
  workspace_id: string; // 工作空间 ID
  task_type: number; // 任务类型，可能是枚举或数字表示
  wayline_type: number; // 轨迹类型，可能是枚举或数字表示
  begin_time: number; // 任务开始时间，时间戳
  end_time: number; // 任务结束时间，时间戳
  execute_time:number[],
  status: number; // 任务状态，可能是枚举或数字表示
  rth_altitude: number; // 返回起飞点的高度
  out_of_control: number; // 是否失控，可能是 0 或 1
  enable_status: number; // 启用状态，可能是 0 或 1
  plan_priority: number; // 任务优先级，数字表示
}
// http://172.20.63.56:6789/pub/api/v1/waylinePlan/createWaylinePlan

// 提交计划
export const createFlyPlan = async function (data: InspectionTask): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX3}/waylinePlan/createWaylinePlan`
  const result = await request.post(url, data)
  return result.data
}

export interface FlightTestPlan {
  id: number; // 唯一标识符，通常是数字
  plan_id: string; // 计划ID，通常是UUID字符串
  sub_code: string; // 子代码，通常是UUID字符串
  major: string; // 专业或类别，字符串类型
  plan_source: string; // 计划来源，字符串类型
  name: string; // 计划名称，字符串类型
  file_id: string; // 文件ID，通常是UUID字符串
  dock_sn: string; // 驾驶舱序列号，字符串类型
  workspace_id: string; // 工作空间ID，通常是UUID字符串
  task_type: number; // 任务类型，数字类型
  wayline_type: number; // 航路类型，数字类型
  username: string; // 用户名，可以为空字符串
  rth_altitude: number; // 返航高度，数字类型
  out_of_control: number; // 是否超出控制，数字类型（通常是0或1）
  create_time: number; // 创建时间，时间戳（毫秒数）
  update_time: number; // 更新时间，时间戳（毫秒数）
  enable_status: number; // 启用状态，数字类型（通常是0或1）
  plan_priority: number; // 计划优先级，数字类型
}
// 下发计划
export const DistributeFlyPlan = async function (data: FlightTestPlan): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX3}/waylinePlan/expressPlan`
  const result = await request.post(url, data)
  return result.data
}
// faf3362c-3c90-2fce-0f88-b059716cb160
// 查询计划
export const getFlyPlan = async function (sub_code: string, page: IPage): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX3}/waylinePlan/sub_code/${sub_code}/getPlanBySubCode?page=${page.page}&page_size=${page.page_size}`
  const result = await request.get(url)
  return result.data
}

// 按id 删除计划
// http://172.20.63.56:6789/pub/api/v1/waylinePlan/deletePlanById?id=13
export async function deleteFlyPlan (planId: string): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX3}/waylinePlan/deletePlanById?id=${planId}`
  const result = await request.delete(url)
  return result.data
}

export const getTaskResult = async function (wid: string, job_id: string): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX1}/files/${wid}/files/${job_id}/jobIdUrl?page=1&page_size=1000`
  const result = await request.get(url)
  return result.data
}

export interface CreatePlan {
  name: string,
  file_id: string,
  dock_sn: string,
  task_type: TaskType, // 任务类型
  wayline_type: WaylineType, // 航线类型
  task_days: number[] // 执行任务的日期（秒）
  task_periods: number[][] // 执行任务的时间点（秒）
  rth_altitude: number // 相对机场返航高度 20 - 500
  out_of_control_action: OutOfControlAction // 失控动作
  min_battery_capacity?: number, // 飞机的最小电池容量。
  min_storage_capacity?: number, // 码头和飞机的最小存储容量。
}

// 创建飞行任务
export const createPlan = async function (workspaceId: string, plan: CreatePlan): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}/flight-tasks`
  const result = await request.post(url, plan)
  return result.data
}

export interface Task {
  job_id: string,
  job_name: string,
  task_type: TaskType, // 任务类型
  file_id: string, // 航线文件id
  file_name: string, // 航线名称
  wayline_type: WaylineType, // 航线类型
  dock_sn: string,
  dock_name: string,
  workspace_id: string,
  username: string,
  begin_time: string,
  end_time: string,
  execute_time: string,
  completed_time: string,
  status: TaskStatus, // 任务状态
  progress: number, // 执行进度
  code: number, // 错误码
  rth_altitude: number // 相对机场返航高度 20 - 500
  out_of_control_action: OutOfControlAction // 失控动作
  media_count: number // 媒体数量
  uploading: boolean // 是否正在上传媒体
  uploaded_count: number // 已上传媒体数量
}

// 获取飞行任务
export const getWaylineJobs = async function (workspaceId: string, page: IPage): Promise<IListWorkspaceResponse<Task>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}/jobs?page=${page.page}&page_size=${page.page_size}`
  const result = await request.get(url)
  return result.data
}

export interface DeleteTaskParams {
  job_id: string
}

//  删除机场任务
export async function deleteTask (workspaceId: string, params: DeleteTaskParams): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}/jobs`
  const result = await request.delete(url, {
    params: params
  })
  return result.data
}

// 删除其他状态的机场任务
export async function deleteOtherTask (job_id: string): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX3}/waylinePlan/deleteJobByJobId?job_id=${job_id}`
  const result = await request.delete(url)
  return result.data
}

// http://172.20.63.56:6789/pub/api/v1/waylinePlan/deleteJobByJobId?job_id=

export enum UpdateTaskStatus {
  Suspend = 0, // 暂停
  Resume = 1, // 恢复
}
export interface UpdateTaskStatusBody {
  job_id: string
  status: UpdateTaskStatus
}

// 更新机场任务状态
export async function updateTaskStatus (workspaceId: string, body: UpdateTaskStatusBody): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}/jobs/${body.job_id}`
  const result = await request.put(url, {
    status: body.status
  })
  return result.data
}

// 断电续飞
export async function poweroffCf (workspaceId: string, job_id: string): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}/jobs/${job_id}`
  const result = await request.put(url)
  return result.data
}

// 任务状态回传
export async function taskFeedback (workspaceId: string, job_id: string): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}/jobs/${job_id}/status`
  const result = await request.get(url)
  return result.data
}
// 更新航线文件
export const importKmzFile = async function (workspaceId: string, file: {}): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}/waylines/file/upload`
  const result = await request.post(url, file, {
    headers: {
      'Content-Type': 'multipart/form-data',
    }
  })
  return result.data
}

// 导入变电站航线文件
export const importSubKmzFile = async function (workspaceId: string, file: {}): Promise<IWorkspaceResponse<any>> {
  const url = `/importNoValiKmz/workspaces/${workspaceId}/waylines/file/upload`
  const result = await request.post(url, file, {
    headers: {
      'Content-Type': 'multipart/form-data',
    }
  })
  return result.data
}

export interface waylineBindInfo {
  wayline_id: string,
  wayline_name: string,
  sub_code: TaskType,
  major: string,
  workspace_id: string,
}
// 绑定航线\场站\专业
export const bindWaylineAndSub = async function (data: waylineBindInfo): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/pubWayline/addWayline`
  const result = await request.post(url, data)
  return result.data
}

// 媒体立即上传
export const uploadMediaFileNow = async function (workspaceId: string, jobId: string): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX}/workspaces/${workspaceId}/jobs/${jobId}/media-highest`
  const result = await request.post(url)
  return result.data
}

// 查询设备信息
export const getLocation = async function (deviceOsdTopic: string, workspaceId: string, deviceSn: string): Promise<IWorkspaceResponse<{}>> {
  const url = `${HTTP_PREFIX2}/sub/subDeviceOsd?deviceOsdTopic=${deviceOsdTopic}/osd&workspace_id=${workspaceId}&deviceSn=${deviceSn}`
  const result = await request.post(url)
  return result.data
}
