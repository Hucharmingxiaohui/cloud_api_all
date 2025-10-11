import { message } from 'ant-design-vue'
import request, { CommonListResponse, IPage, IWorkspaceResponse } from '/@/api/http/request'
const HTTP_PREFIX = ''
// 定义 objectList 数组中的每一项
export interface ObjectListItem {
  objectId: string;
  typeList: string[];
  imagePathList: string[];
}
export interface RequestContent {
  requestHostIp: string;
  requestHostPort: string;
  requestId: string;
  objectList: ObjectListItem[];
}

// 发送算法请求
export const toRequest = async function (requestContent: RequestContent): Promise<IWorkspaceResponse<any>> {
  const url = `${HTTP_PREFIX}/picAnalyse`
  const result = await request.post(url, requestContent)
  return result.data
}
