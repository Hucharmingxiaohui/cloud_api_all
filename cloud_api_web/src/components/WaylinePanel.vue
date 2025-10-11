<template>
  <div class="container">
    <!-- <div class="header">航线管理</div> -->
    <div class="operation">
      <span class="label">航线名称:</span>
      <el-input placeholder="请输入文件名称" class="custom-select" style="width: 200px;" v-model="searchValue"></el-input>
      <!-- 查询按钮 -->
      <el-button class="new_btn iconfont icon-chaxunhangxian" type="primary" style="margin-left: 30px; width: 100px;"
        @click="SearchWayline">
        <span style="margin-left: 5px; font-size: 14px;">查询航线</span>
      </el-button>
      <!-- 新建按钮 -->
      <!-- <el-button class="new_btn iconfont icon-xinjianhangxian" type="primary" style="margin-left: 10px; width: 100px;"
        @click="selectDroneModel">
        <span style="margin-left: 5px; font-size: 14px;">新建航线</span>
      </el-button> -->
      <router-link to="/wayline/Model">
        <el-button class="new_btn iconfont icon-xinjianhangxian" type="primary" style="margin-left: 10px; width: 100px;">
          <span style="margin-left: 5px; font-size: 14px;">三维航线</span>
        </el-button>
      </router-link>

      <!-- 导入按钮 -->
      <el-button class="new_btn iconfont icon-daoruhangxian" type="primary" style="margin-left: 10px; width: 100px;"
        @click="openWaylineDialog">
        <span style="margin-left: 5px; font-size: 14px;">导入航线</span>
      </el-button>
      <el-dialog v-model="isImportWayline" title="导入航线" center width="400px" top="10%" class="selectDialog"
        :close-on-click-modal="false">
        <div style="display: flex; flex-direction: column; align-items: center; color: aliceblue;">
          <!-- 选择专业 和 选择框在同一行 -->
          <div style="display: flex; align-items: center; margin-bottom: 10px;">
            <span style="font-size: 18px; margin-right: 10px; width: 150px;">选择专业:</span>
            <el-select v-model="selectedMajor" placeholder="请选择" size="large" class="select-operation"
              :teleported='false'>
              <el-option v-for="item in MajorOption" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>

          <!-- 选择场站 和 选择框在同一行 -->
          <div style="display: flex; align-items: center; margin-top: 10px;">
            <span style="font-size: 18px; margin-right: 10px;width: 150px; ">选择场站:</span>
            <el-select v-model="selectedStation" placeholder="请选择" size="large" class="select-operation"
              :teleported='false'>
              <el-option v-for="item in StationOption" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
        </div>

        <!-- 上传文件的组件 -->
        <div style="margin: 20px 0 0 15px; text-align: center;">
          <el-upload class="upload-demo" :before-upload="beforeUpload" :show-file-list="true" :http-request="uploadFile"
            ref="upload">
            <el-button class="btn">导入航线</el-button>
          </el-upload>
        </div>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="closeImportWayline" class="nobtn">取消</el-button>
            <el-button type="primary" @click="uploadWayline" class="okbtn">确定</el-button>
          </div>
        </template>
      </el-dialog>
    </div>
    <div>
      <el-dialog v-model="showSelectDialog" title="选择机型" center width="20%" top="10%" class="selectDialog"
        :close-on-click-modal="false">
        <div style="display: flex; justify-content: center; align-items:center;color: aliceblue;">
          <span style="display: flex; justify-content: center;align-content: center; margin-right: 10px;">无人机机型:</span>
          <el-cascader v-model="selectedDroneModel" :options="droneOption" @change="updateDroneOption"
            class="custom-select" />
        </div>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="showSelectDialog = false" class="nobtn">取消</el-button>
            <router-link to="/wayline/createWayline">
              <el-button type="primary" @click="showSelectDialog = false" class="okbtn">确定</el-button>
            </router-link>

          </div>
        </template>
      </el-dialog>
    </div>
    <div class="content">
      <div class="table-container">
        <el-table :data="waylinesData.data" stripe>

          <el-table-column label="序号" align='center' width="60">
            <template #default="scope">
              {{ scope.$index + (paginationProp.current - 1) * paginationProp.pageSize + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="航线编号" show-overflow-tooltip="true">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.id }}</div>
            </template>
          </el-table-column>
          <el-table-column label="航线名称" show-overflow-tooltip="true">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.name }}</div>
            </template>
          </el-table-column>
          <!-- <el-table-column label="专业类型">
            <template #default="scope">
              <div class="ellipsis">{{ scope.row.major_type }}</div>
            </template>
          </el-table-column> -->
          <el-table-column label="更新时间">
            <template #default="scope">
              <div class="ellipsis">{{ new Date(scope.row.update_time).toLocaleString() }}</div>
            </template>
          </el-table-column>
          <el-table-column label="无人机">
            <template #default="scope">
              <div class="ellipsis">{{ DEVICE_NAME[scope.row.drone_model_key] }}</div>
            </template>
          </el-table-column>
          <el-table-column label="相机">
            <template #default="scope">
              <div class="ellipsis">{{ DEVICE_NAME[scope.row.payload_model_keys] }}</div>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="330px">
            <template #default="scope">
              <div class="action-buttons">
                <el-button size="small" link type="primary" class="download"
                  @click="downloadWayline(scope.row.id, scope.row.name)">下载</el-button>
                <el-button size="small" link type="primary" class="preview"
                  @click="openDrag(scope.row.id, scope.row.template_types[0])">预览</el-button>
                <el-button size="small" link type="primary" class="waylipot"
                  @click="openWaylinePoints(scope.row)">航点</el-button>
                <el-button size="small" link type="primary" class="wayliedit"
                  @click="editDrag(scope.row.id, scope.row.name, scope.row.template_types[0])">编辑</el-button>
                <!-- <el-button size="small" type="text" @click="downloadWayline(scope.row.id, scope.row.name)">查看</el-button> -->
                <el-popconfirm width="220" confirm-button-text="确定" cancel-button-text="不，谢谢" icon-color="#626AEF"
                  title="航线文件一旦删除就无法恢复,是否继续？" @confirm="deleteWayline(scope.row.id)">
                  <template #reference>
                    <el-button size="small" link type="primary" class="delete">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <!-- <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange" :current-page="currentPage"
        :page-sizes="[10, 20, 30, 40]" :page-size="pageSize" :total="total"
        layout="total, sizes, prev, pager, next, jumper"></el-pagination> -->
    </div>
    <div class="pagination-container">
      <!-- 分页 -->
      <el-pagination v-model:current-page="paginationProp.current" v-model:page-size="paginationProp.pageSize"
        :page-sizes="paginationProp.pageSizeOptions" :total="paginationProp.total"
        layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
        @current-change="handleCurrentChange"></el-pagination>
    </div>
    <!-- 弹窗 -->
    <div class="live" v-show="showMap" v-drag-window>
      <div style="height: 40px; width: 100%" class="drag-title"></div>
      <a style="position: absolute; right: 10px; top: 10px; font-size: 16px; color: white;" @click="closeDrag">
        <CloseOutlined />
      </a>
      <ttt ref="wayLineid"></ttt>
    </div>
    <!-- 测温弹窗 -->
  </div>
  <!-- </div> -->
  <!-- <div class="project-wayline-wrapper height-100">
    <a-spin :spinning="loading" :delay="300" tip="downloading" size="large">

      <div :style="{ height: height + 'px' }" class="scrollbar">
      </div>
    </a-spin>
  </div> -->
</template>

<script lang="ts" setup>
import { reactive } from '@vue/reactivity'
import { message } from 'ant-design-vue'
import { ElButton, ElDialog, ElUpload, ElMessage } from 'element-plus'
import { onMounted, onUpdated, ref, computed, nextTick } from 'vue'
import { TableState } from 'ant-design-vue/lib/table/interface'
import { bindWaylineAndSub, getLocation, deleteWaylineFile, downloadWaylineFile, getWaylineFiles, importKmzFile, searchWaylineFiles, gethWaylineInfo, editWaylineInfo, importSubKmzFile } from '/@/api/wayline'
import { ELocalStorageKey, ERouterName } from '/@/types'

import { EllipsisOutlined, RocketOutlined, CameraFilled, UserOutlined, SelectOutlined, SearchOutlined, CloseOutlined } from '@ant-design/icons-vue'
import { DEVICE_NAME } from '/@/types/device'
import { useMyStore } from '/@/store'
import { WaylineFile } from '/@/types/wayline'
import { downloadFile } from '/@/utils/common'
import { IPage } from '/@/api/http/type'
import { CURRENT_CONFIG } from '/@/api/http/config'
import { load } from '@amap/amap-jsapi-loader'
import { getRoot } from '/@/root'
import ttt from '/@/components/g-map/mapPanel.vue'
import { useRouter } from 'vue-router'
import { uuidv4 } from '/@/utils/uuid'
import { GeojsonCoordinate } from '/@/types/map'
import { gcj02towgs84, wgs84togcj02 } from '/@/vendors/coordtransform'
import { PostElementsBody } from '/@/types/mapLayer'
import { useGMapCover } from '/@/hooks/use-g-map-cover'
import { getDeviceTopo, getUnreadDeviceHms, updateDeviceHms, getPlatformInfo, getAllWorkspaceInfo } from '/@/api/manage'
import CustomTree from '/@/components/substationTree.vue'

import { insertTEMPConfig, getAllSub, PointData, getAllPoints } from '/@/api/points'
import {
  generateLineContent,
  generatePointContent,
  generatePolyContent
} from '/@/utils/map-layer-utils'
const router = useRouter()
const searchValue = ref('') // 搜索内容
const loading = ref(false)
const store = useMyStore()
const useGMapCoverHook = useGMapCover(store)
const userId = ref(localStorage.getItem(ELocalStorageKey.UserId)!)
let workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
const body: IPage = {
  page: 1,
  total: -1,
  page_size: 10
}

// --------------------------------航线导入-------------------------------------------------------
const isImportWayline = ref(false)
const MajorOption = [
  {
    label: '输电',
    value: '输电'
  },
  {
    label: '配电',
    value: '配电'
  },
  {
    label: '变电',
    value: '变电'
  },
]
const StationOption = ref([])
const selectedMajor = ref<any>(null) // 选择的专业
const selectedStation = ref<any>(null) // 选择的场站
const uploadWaylineId = ref(null)
const uploadWaylineName = ref(null)

/**
 * @description: 查询所有的场站
 * @param {string} waylineInfo 航线信息
 * */
function getSubInfo () {
  getAllSub().then(res => {
    if (res.code !== 0) {
      return
    }
    StationOption.value = res.data.map(item => ({
      value: item.sub_code,
      label: item.sub_name
    }))
  })
}

/**
 * @description: 插入航线绑定信息
 * @param {string} wayline_id 航线id
 * @param {string} wayline_name 航线名称
 * @param {string} sub_code 场站编码
 * @param {string} major 专业
 * @param {string} workspace_id 工作空间 id
 * */
function insertWayline () {
  nextTick(() => {
    const obj = {
      wayline_id: uploadWaylineId.value,
      wayline_name: uploadWaylineName.value,
      sub_code: selectedStation.value,
      major: selectedMajor.value,
      workspace_id: workspaceId,
    }
    bindWaylineAndSub(obj).then(res => {
      // 转换数据格式
      if (res.code !== 0) {
        return
      }
      body.total = 0
      body.page = 1
      waylinesData.data = []
      getWaylines()
      isImportWayline.value = false
    })
  })
}

/**
 * @description: 打开导入弹窗
 * */
function openWaylineDialog () {
  isImportWayline.value = true
  getSubInfo()
}

/**
 * @description: 关闭航线导入弹窗
 * */
function closeImportWayline () {
  isImportWayline.value = false
}

/**
 * @description: 航线导入弹窗数据确定提交方法
 * */
const uploadWayline = () => {
  if (!selectedMajor.value || !selectedStation.value) {
    ElMessage.error('请选择专业和场站')
    return
  }
  insertWayline()
}
function updateMajor (val) {
  selectedMajor.value = val
}
function updateStation (val) {
  selectedStation.value = val
}
function addStationData () {
  let workspaces = null
  getAllWorkspaceInfo(userId.value).then(res => {
    // 转换数据格式
    workspaces = res.data
    if (workspaces) {
      StationOption.value = workspaces.map(item => ({
        value: item.workspace_id, // 使用 workspace_id 作为 value
        label: item.workspace_name // 使用 workspace_name 作为 label
      }))
    }
  })
}
// --------------------------------------------------------------------------------------------
const paginationProp = reactive({
  pageSizeOptions: ['10', '20', '30', '40'],
  showQuickJumper: true,
  showSizeChanger: true,
  pageSize: 10,
  current: 1,
  total: 0
})

const waylinesData = reactive({
  data: [] as WaylineFile[]
})

const root = getRoot()

const deleteTip = ref(false)
const deleteWaylineId = ref<string>('')
const canRefresh = ref(true)
const importVisible = ref<boolean>(root.$router.currentRoute.value.name === ERouterName.NEW_WAYLINE)
const height = ref()

onMounted(() => {
  // const parent = document.getElementsByClassName('scrollbar').item(0)?.parentNode as HTMLDivElement
  // height.value = document.body.clientHeight - parent.firstElementChild!.clientHeight
  getWaylines()

  // 添加树形图数据
  getTreeData()
  // 添加场站选择数据
  // addStationData()
})

//= ===========================================添加树形图==========================================================================

const selectedNode = ref(null)

const treeData = ref([
  {
    title: '区域1',
    key: '1',
  }
])

function getTreeData () {
  let workspaces = null
  getAllWorkspaceInfo(userId.value).then(res => {
    // 转换数据格式
    workspaces = res.data
    if (workspaces) {
      clearLeafNodesAndAddData(treeData.value, workspaces)
    }
  })
}
// 添加树形图方法
const clearLeafNodesAndAddData = (treeData, data) => {
  treeData.forEach(node => {
    // 如果有子节点，则递归遍历
    if (node.children && node.children.length > 0) {
      clearLeafNodesAndAddData(node.children, data)
    }

    // 如果是叶子节点，清空现有数据并添加 data 中的数据
    if (!node.children || node.children.length === 0) {
      node.children = data.map(item => ({
        title: item.workspace_name,
        key: item.workspace_id, // 使用 workspace_id 作为 key
        workspace_id: item.workspace_id,
        workspace_desc: item.workspace_desc,
        platform_name: item.platform_name,
        bind_code: item.bind_code,
        isLeaf: true // 显式标记为叶子节点
      }))
    }
  })
}
// 树形图选中方法
const handleNodeChange = (node) => {
  // selectedNode.value = node

  workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!
  getWaylines()
  console.log('Node changed in parent:', node) // 确认父组件事件是否触发
}
// -----------------------------------------新建航线---选择无人机机型-----------------------------------------------------------------------------------------------------------------------
const droneOption = [
  {
    value: 89,
    label: 'M350 RTK'
  },
  {
    value: 60,
    label: 'M300 RTK'
  },
  {
    value: 67,
    label: 'M30/M30T',
    children: [
      {
        value: 0,
        label: 'Matrice 30(M30)'
      },
      {
        value: 1,
        label: 'Matrice 30T(M30T)'
      }
    ]
  },
  {
    value: 77,
    label: 'M3E/M3T/M3M',
    children: [
      {
        value: 0,
        label: 'DJI Mavic3行业版(M3E)'
      },
      {
        value: 1,
        label: 'DJI Mavic3红外版(M3T)'
      },
      {
        value: 2,
        label: 'DJI Mavic3多光谱版(M3M)'
      }
    ]
  },
  {
    value: 91,
    label: 'M3D/M3TD',
    children: [
      {
        value: 0,
        label: 'DJI Matrice 3D(M3D)'
      },
      {
        value: 1,
        label: 'DJI Matrice 3TD (M3TD)'
      }
    ]
  }
]
const selectedDroneModel = ref(77)
// function updateDroneOption () {
//   const points = store.state.Points
//   points[0].missionConfig.droneInfo.droneEnumValue = selectedDroneModel.value
// }

const updateDroneOption = (value) => {
  console.log('选项23', value)
  const points = store.state.Points
  points[0].missionConfig.droneInfo.droneEnumValue = value[0]
  points[0].missionConfig.droneInfo.droneSubEnumValue = value[1]
}
const showSelectDialog = ref(false)
function selectDroneModel () {
  showSelectDialog.value = true
}
// ==========================================================================跳转到航点界面==========================================================================================================
/**
 * @description: 携带参数跳转到航点界面
 * @param [string] waylineId 航线id
 * */
function openWaylinePoints (waylineId: any) {
  // console.log('ssss', waylineId)
  // store.commit('SET_WAYLINE_INFO', waylineId)
  const obj = {
    id: waylineId.id,
    name: waylineId.name
  }
  localStorage.setItem('waylineId', JSON.stringify(obj))
  router.push({ path: '/waylinePoints' })
  // console.log('ssdfddd', store.state.waylineData.id)
  // const points1 = computed(() => store.state.Points)
}
// -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

// 控制航线是否可视化
const showMap = ref(false)
function closeDrag () {
  showMap.value = false
}
const wayLineid = ref(null) // 节点
function openDrag (waylineId: string, Waylinetype: Number) {
  if (Waylinetype !== 0) {
    ElMessage({
      message: '暂不支持面状航线!.',
      type: 'warning',
    })
    return
  }
  showMap.value = true
  gethWaylineInfo(workspaceId, waylineId).then(res => {
    if (res.code !== 0) {
      return
    }
    wayLineid.value.WaylineInfo(res.data)
  })
  // wayLineid.value.WaylineInfo(id) // 通过 ref.value 访问子组件的方法
}
// ----------------------------------------------------------------------------------------------------获取航线数据，改成相应的数据格式，添加航点-----------------------------------------------------
let mypoints = null
// 编辑航线信息
function editDrag (waylineId: string, name: string, Waylinetype: Number) {
  if (Waylinetype !== 0) {
    ElMessage({
      message: '暂不支持面状航线!.',
      type: 'warning',
    })
    return
  }
  editWaylineInfo(workspaceId, waylineId).then(res => {
    if (res.code !== 0) {
      return
    }
    // 重置仓库数据
    const newPoints = [
      {
        name: name,
        id: '',
        is_distributed: true,
        elements: [],
        is_check: false,
        is_select: false,
        type: 1,
        missionConfig: res.data.missionConfig,
        folder: res.data.folder,
        editing: 1
      },
    ]
    store.commit('SET_POINTS', newPoints)
    // const points1 = computed(() => store.state.Points)
    // 存放数据到仓库
    let pinNum = 0
    res.data.folder.placeMarks.forEach(item => {
      mypoints = item
      const coordinatesString = item.point.coordinates
      const [longitude, latitude] = coordinatesString.split(',').map(Number)
      // 转换为对象
      const obj = {
        title: '航点' + pinNum,
        coordinates: {
          lng: longitude,
          lat: latitude
        }
      }
      pinNum++
      postPinPositionResource(obj)
    })
  })
  router.push({ path: '/wayline/createWayline' })
  // wayLineid.value.WaylineInfo(id) // 通过 ref.value 访问子组件的方法
}

// 坐标转object
function getPinPositionResource (obj: any) {
  const position = obj.coordinates
  const resource = generatePointContent(position)
  const name = obj.title
  const id = uuidv4()
  return {
    id,
    name,
    resource
  }
}
function postPinPositionResource (obj) {
  const req = getPinPositionResource(obj)
  setLayers(req)
}

function setLayers (resource: PostElementsBody) {
  // 添加航点数据
  // const points1 = store.state.Points
  const points1 = store.state.Points
  const exists1 = points1[0].elements.some(item => item.id === resource.id)
  if (!exists1) {
    // 添加航点数据
    const Placemark = mypoints
    resource.Placemark = Placemark
    points1[0].elements.push(resource)
    store.commit('SET_POINTS', points1)
  }
}
// ----------------------------------------------------------------------------------------------------结束-----------------------------------------------------
function getWaylines () {
  if (!canRefresh.value) {
    return
  }
  canRefresh.value = false
  getWaylineFiles(workspaceId, {
    page: body.page,
    page_size: body.page_size,
    order_by: 'update_time desc'
  }).then(res => {
    if (res.code !== 0) {
      return
    }
    // waylinesData.data = [...waylinesData.data, ...res.data.list]
    waylinesData.data = res.data.list
    // body.total = res.data.pagination.total
    // body.page = res.data.pagination.page
    paginationProp.total = res.data.pagination.total
    paginationProp.current = res.data.pagination.page
  }).finally(() => {
    canRefresh.value = true
  })
}

function showWaylineTip (waylineId: string) {
  deleteWaylineId.value = waylineId
  deleteTip.value = true
}

// 分页事件
function handleSizeChange (val: number) {
  paginationProp.pageSize = val
  // refreshData(paginationProp)
  paginationProp.current = 1 // 重置为第一页
  refreshData(paginationProp)
}
function handleCurrentChange (val: number) {
  paginationProp.current = val
  refreshData(paginationProp)
}
type Pagination = TableState['pagination']
function refreshData (page: Pagination) {
  body.page = page?.current!
  body.page_size = page?.pageSize!
  getWaylines()
}

// 搜索航线
function SearchWayline () {
  if (!canRefresh.value) {
    return
  }
  canRefresh.value = false
  searchWaylineFiles(workspaceId, {
    search_value: searchValue.value,
    order_by: 'update_time desc'
  }).then(res => {
    if (res.code !== 0) {
      return
    }
    waylinesData.data = []
    waylinesData.data = [...waylinesData.data, ...res.data.list]
    body.total = res.data.pagination.total
    body.page = res.data.pagination.page
  }).finally(() => {
    canRefresh.value = true
  })
}

function deleteWayline (id: string) {
  deleteWaylineId.value = id
  deleteWaylineFile(workspaceId, deleteWaylineId.value).then(res => {
    if (res.code === 0) {
      message.success('Wayline file deleted')
    }
    deleteWaylineId.value = ''
    deleteTip.value = false
    body.total = 0
    body.page = 1
    waylinesData.data = []
    getWaylines()
  })
}

function downloadWayline (waylineId: string, fileName: string) {
  loading.value = true
  downloadWaylineFile(workspaceId, waylineId).then(res => {
    if (!res) {
      return
    }
    const data = new Blob([res], { type: 'application/zip' })
    downloadFile(data, fileName + '.kmz')
  }).finally(() => {
    loading.value = false
  })
}

function selectRoute (wayline: WaylineFile) {
  // console.log('ssss')
  store.commit('SET_SELECT_WAYLINE_INFO', wayline)
}

function onScroll (e: any) {
  const element = e.srcElement
  if (element.scrollTop + element.clientHeight >= element.scrollHeight - 5 && Math.ceil(body.total / body.page_size) > body.page && canRefresh.value) {
    body.page++
    getWaylines()
  }
}

interface FileItem {
  uid: string;
  name?: string;
  status?: string;
  response?: string;
  url?: string;
}

interface FileInfo {
  file: FileItem;
  fileList: FileItem[];
}
const fileList = ref<FileItem[]>([])

function beforeUpload (file: FileItem) {
  fileList.value = [file]
  loading.value = true
  return true
}
// const uploadFile = async () => {
//   fileList.value.forEach(async (file: FileItem) => {
//     const fileData = new FormData()
//     fileData.append('file', file, file.name)
//     await importKmzFile(workspaceId, fileData).then((res) => {
//       if (res.code === 0) {
//         message.success(`${file.name} file uploaded successfully`)
//         canRefresh.value = true
//         body.total = 0
//         body.page = 1
//         waylinesData.data = []
//         getWaylines()
//       }
//     }).finally(() => {
//       loading.value = false
//       fileList.value = []
//     })
//   })
// }
const uploadFile = async () => {
  fileList.value.forEach(async (file: FileItem) => {
    const fileData = new FormData()
    fileData.append('file', file, file.name)
    await importSubKmzFile(workspaceId, fileData).then((res) => {
      if (res.code === 0) {
        // message.success(`${file.name} 文件上传成功`)
        uploadWaylineId.value = res.data.wayline_id
        uploadWaylineName.value = res.data.name
        fileList.value = []
        canRefresh.value = true
      }
    }).finally(() => {
      loading.value = false
      fileList.value = []
    })
  })
}

</script>

<style lang="scss" scoped>
.wayline-panel {
  background: #3c3c3c;
  margin-left: auto;
  margin-right: auto;
  margin-top: 10px;
  height: 90px;
  width: 95%;
  font-size: 13px;
  border-radius: 2px;
  cursor: pointer;

  .title {
    display: flex;
    flex-direction: row;
    align-items: center;
    height: 30px;
    font-weight: bold;
    margin: 0px 10px 0 10px;
  }
}

.uranus-scrollbar {
  overflow: auto;
  scrollbar-width: thin;
  scrollbar-color: #c5c8cc transparent;
}

::v-deep .home-ant-input.ant-input-affix-wrapper .ant-input {
  background-color: black;
  color: #c5c8cc;
}

.custom-select {
  // margin-top: 10px;
  width: 300px;

  :deep(.el-select--large .el-select__wrapper) {
    min-height: 30px;
  }

  :deep(.custom-select .el-select-dropdown) {
    background-color: transparent !important;
    /* 透明背景 */
    border: 1px solid blue !important;
    /* 边框颜色 */
  }

  :deep(.custom-select .el-select-dropdown__item) {
    background-color: #05204B !important;
    /* 下拉项背景色 */
    color: #fff !important;
    /* 字体颜色 */
  }

  :deep(.el-select.is-disabled .el-select__dropdown) {
    background: #05204B;
    color: white;
  }

  :deep(.el-select__placeholder) {
    color: white;
  }

  :deep(.el-cascader-panel) {
    // box-sizing: border-box;
    // list-style: none;
    // margin: 0;
    // min-height: 100%;
    // padding: 6px 0;
    // position: relative;
    background: #05204B;
    color: white;
  }

  :deep(el-cascader__dropdown.el-popper) {
    background: #05204b !important;
    ;
    color: white;
  }
}

::v-deep .el-dialog__title {
  font-size: 16px;
  /* 修改为你想要的大小 */
  // font-weight: bold;
  color: #F1F6FF;
}

::v-deep .el-dialog {
  background-color: #0B2757;
  // background: rgba(59, 116, 255, 0.15);
  width: 25%;
  -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
}

.okbtn {
  background-color: rgba(7, 75, 208, 1);
  // height: 40px;
  border: 1px solid rgba(0, 64, 147, 1);
}

.okbtn:hover {
  border: 1px solid rgba(0, 112, 209, 1);
}

.nobtn {
  background-color: rgba(255, 255, 255, 0.05);
  // height: 40px;
  border: 1px solid rgba(206, 227, 255, 0.42);
}

::v-deep .el-scrollbar__view.el-cascader-menu__list {
  background-color: #0B2756;
  // border: 1px solid #00399A;
  color: white;
  border: 1px solid #154480 !important;
  box-shadow: none;

}

::v-deep .el-popper.is-light {
  border: 1px solid #00399A;
}

.container {
  // height: 100%;
  width: 100vw;
  // padding: 10px;
  display: flex;
  flex-direction: column;
  /* 使子元素垂直排列 */
}

.ellipsis {
  white-space: nowrap;
  /* 防止换行 */
  overflow: hidden;
  /* 隐藏超出部分 */
  text-overflow: ellipsis;
  /* 显示省略号 */
}

.main-box {
  display: flex;
  /* 使用 flexbox 布局 */
  height: calc(100vh - 80px);
  /* 设置容器高度为视口高度 */
}

.box-right {
  // background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  flex: 1;
  /* 右侧占据剩余空间 */
  // width: calc(80% - 10px);
  // margin-left: 10px;
  // background-color: #2196F3; /* 蓝色背景 */
  // padding: 20px;
  /* 内边距 */
  color: white;
  /* 字体颜色 */
  // border-radius: 15px;
  // border: none;
  height: 100%;
}

// .box-right:hover {
//   box-shadow: inset 0px 0px 20px 3px rgba(34, 135, 255, 0.7);
// }
// 头部  标题 面包屑
.header {
  width: 100%;
  height: 60px;
  background: #05204B;
  padding: 16px;
  font-size: 20px;
  font-weight: bold;
  text-align: start;
  color: aliceblue;
}

// 下拉框
.select-operation {
  :deep(.el-select__placeholder) {
    color: rgba(255, 255, 255, 1);
    font-size: 14px;
    font-family: Google Sans-Medium;
    font-weight: 500;
  }

  /**修改下拉图标颜色 */
  :deep(.el-select__caret) {
    color: #ffffff;
  }

  :deep(.el-select-dropdown) {
    background: #012b78;
    box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    border: 1px solid #719fff;
  }

  :deep(.el-select-dropdown__item) {
    font-size: 14px;
    font-weight: 500;
    color: #ffffff;
  }

  :deep(.el-select-dropdown__item.is-hovering) {

    background-color: skyblue;
  }
}

// 操作部分
.operation {
  display: flex;
  // justify-items: center; /* 这里可能是错误的，flexbox 中应该使用 justify-content */
  align-items: center;
  /* 这个会确保 label 在垂直方向居中 */
  background-color: rgba(1, 36, 98, 1);
  border-radius: 4px;
  // width: 100%;
  height: 60px;
  margin: 31px 12px 0 12px;

  .label {
    height: 60px;
    display: flex;
    /* 这个可以保留，确保子元素居中 */
    align-items: center;
    /* 垂直居中 */
    justify-content: center;
    /* 水平居中 */
    color: rgba(255, 255, 255, 1);
    font-size: 14px;
    font-family: Google Sans-Medium;
    font-weight: 500;
    margin: 0 10px 0 30px;
  }

  .new_btn {
    background-image: linear-gradient(180deg,
        rgba(70, 145, 217, 1) 0,
        rgba(21, 81, 181, 1) 100%);
    border-radius: 4px;
    width: 108px;
    height: 37px;

    // margin: 12px 0 0 30px;
    .thumbnail_1 {
      width: 12px;
      height: 12px;
      margin: 5px 0 0 12px;
    }

    .btn_text {
      width: 70px;
      height: 18px;
      overflow-wrap: break-word;
      color: rgba(255, 255, 255, 1);
      font-size: 14px;
      font-family: Google Sans-Medium;
      font-weight: 500;
      text-align: left;
      white-space: nowrap;
      line-height: 14px;
      margin: 9px 20px 0 8px;
    }

  }

  .new_btn1 {
    background-image: linear-gradient(180deg,
        rgba(248, 212, 94, 1) 0,
        rgba(227, 157, 6, 1) 100%);
    border-radius: 4px;
    width: 70px;
    height: 37px;

    // margin: 12px 0 0 30px;
    .thumbnail_1 {
      width: 12px;
      height: 12px;
      margin: 5px 0 0 12px;
    }

    .btn_text {
      width: 30px;
      height: 18px;
      overflow-wrap: break-word;
      color: rgba(255, 255, 255, 1);
      font-size: 14px;
      font-family: Google Sans-Medium;
      font-weight: 500;
      text-align: left;
      white-space: nowrap;
      line-height: 14px;
      margin: 9px 20px 0 8px;
    }

  }

}

.content {
  margin: 15px 12px 0 12px;
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}

// 操作下面的几个按钮的样式-------------------------------------------

.download,
.preview,
.temeasure,
.waylipot,
.wayliedit {
  background-color: rgba(51, 122, 255, 0.12);
  border-radius: 4px;
  height: 28px;
  border: 1px solid rgba(0, 64, 147, 1);
  width: 40px;
  margin-left: 7px;
}

.delete {
  background-color: rgba(255, 92, 51, 0.19);
  border-radius: 4px;
  height: 28px;
  color: rgba(255, 215, 215, 1);
  border: 1px solid rgba(255, 132, 132, 1);
  width: 40px;
  margin-left: 7px;
}

.action-buttons .el-button {
  color: white;
  /* 使文字颜色为白色 */
}

.action-buttons .delete {
  color: rgba(255, 215, 215, 1);
  /* 使文字颜色为白色 */
}

//----------------------------------------------------------------------
.btn {
  background-image: linear-gradient(180deg,
      rgba(70, 145, 217, 1) 0,
      rgba(21, 81, 181, 1) 100%);
  border-radius: 4px;
  height: 36px;
  color: white;
  width: 93px;
  border: none;

}

.btn:hover {
  border: 2px solid #1299C3;
}

:deep(.btn1) {
  border: 2px solid #1299C3;
  background: linear-gradient(to top, #11B4FB, #023956);
  color: rgba(255, 255, 255, 0.762);
}

::v-deep .home-ant-input.ant-input-affix-wrapper .ant-input {
  background-color: black;
  color: #c5c8cc;
}

.live {
  position: absolute;
  z-index: 1;
  left: 0;
  top: 400px;
  margin-left: 345px;

  text-align: center;
  width: 1000px;
  height: 420px;
  background: #232323;
}

.live1 {
  padding: 10px 0 0 0;
  position: absolute;
  z-index: 1;
  left: 0;
  top: 200px;
  margin-left: 345px;
  width: 940px;
  height: 690px;
  // background: #232323;
  // background: rgba(59, 116, 255, 0.2);
  background-color: #205CA1;

  // background-color: #294A72;
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  .content {
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #06346A;
    padding: 20px;

    .content-left {
      width: 300px;
      // background-color:#1d4292;
      height: 600px;
      border-right: 1px solid #023956;
    }

    .content-right {
      width: 600px;
      background-color: #1d4292;
      height: 600px;
      border: 3px dashed #3667A7;
    }

  }

}

.pagination-container {
  position: absolute;
  bottom: 40px;
  /* 距离底部的距离，可调整 */
  left: 50%;
  /* 距离右边的距离，可调整 */
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  align-items: center;
}

// 输入框
:deep(.el-input) {
  --el-input-border-color: #1d4292;
  --el-input-background-color: #1d4292;
  --el-input-text-color: white;
}

:deep(.el-input__wrapper) {
  background-color: #0B2756;
}

:deep(.el-select__wrapper) {
  background-color: #0B2756;
  box-shadow: 0 0 0 1px #163474 inset;
  color: aliceblue;
  width: 130px;
}

::v-deep .el-cascader-node:hover {
  background-color: #2264a7 !important;
}

::v-deep .el-cascader-node.in-active-path {
  background-color: #2264a7 !important;
}

::v-deep .el-table tr {
  background-color: #0B2756 !important;
  /* opacity: 0.6; */
  color: #F1F6FF;
  font-weight: bold;
}

::v-deep .el-select__placeholder.is-transparent {
  color: white;
}

::v-deep .el-scrollbar__view.el-select-dropdown__list {
  background-color: #0B2756;
  box-shadow: 0 0 0 1px #163474 inset;
  color: white !important;
}

::v-deep .el-select-dropdown__item.is-hovering {
  background-color: #2264a7 !important;
}

::v-deep .el-select-dropdown__item {
  color: white;
}

::v-deep .el-select__placeholder {
  color: white;
}

//============================================================================================================
// 表格 无数据内容背景设置
:deep(.el-table__empty-block) {
  background-color: #2264a7;
}

// 表格最后一条白线
:deep .el-table__inner-wrapper::before {
  height: 0;
}

/* // 修改高亮当前行颜色 */
::v-deep .el-table tbody tr:hover>td {
  background: rgba(0, 114, 245, 0.6) !important;
}

::v-deep .el-table td {
  border: 2px solid #01123288;
  /* 设置列的边框颜色和粗细 */
  font-size: 16px;
  font-weight: 500;
}

// 表格样式
::v-deep .el-table {
  .cell {
    text-align: center;
  }
}

// // 表头大小
::v-deep .el-table th {
  height: 50px;
  font-size: 16px !important;
  /* 如果你需要修改表头字体大小，设置一个不同的大小 */
  color: rgba(255, 255, 255, 1);
  background-color: #00399A;
  border-left: 2px #01123288 solid;
  border-bottom: 2px #01123288 solid !important;
}

// :header-cell-style="{ height: '43px', color: 'rgba(255, 255, 255, 1)', fontSize: '16px ', fontWeight: 'bold', backgroundColor: '#00399A', borderLeft: '2px #01123288 solid', borderBottom: '1px #154480 solid' }">

/* // 斑马线颜色 */

::v-deep .el-table--striped .el-table__body tr.el-table__row--striped td {
  background: rgba(0, 45, 120, 1);
}

//分页数据
/* 修改前后箭头未点击时的背景颜色 */
:deep .el-pagination .btn-prev,
:deep .el-pagination .btn-next {
  background-color: #062254 !important;
  color: #fff;
}

/* 修改未点击时的数字方块背景颜色 */
:deep .el-pagination .el-pager li:not(.active):not(.disabled):hover {
  background-color: #2264a7 !important;
}

/* 未点击时的数字方块背景颜色 */
:deep .el-pagination .el-pager li:not(.active):not(.disabled) {
  background-color: #062254 !important;
  color: #fff;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #124AAD !important; //修改默认的背景色
  color: #fff;
}

::v-deep el-pager {
  background-color: #0B2756;
}

/* 移除表格行的底部边框 */
::v-deep .el-table__body-wrapper .el-table__row {
  border-bottom: none !important;
  /* 或者： */
  box-shadow: none !important;
}

/* 如果仍然看到白色线条，可能是阴影效果的原因 */
::v-deep .el-table__body-wrapper .el-table__row td {
  border-bottom: none !important;
}
</style>
