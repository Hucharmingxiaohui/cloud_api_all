<template>
  <div class="main-box">
    <div class="box-left" v-show="viewMode === '2d'">
      <div class="box_title">
        <div class="thumbnail_1"></div>
        <div class="box_text">新建光伏计划</div>
      </div>

      <div class="create-plan-wrapper">
        <div class="content">
          <el-form
            :model="planBody"
            label-width="120px"
            ref="valueRef"
            :rules="rules"
            label-position="left"
          >
            <!-- 第一步: 基础信息 -->
            <div v-if="currentStep === 1">
              <el-form-item label="计划名称：" required prop="name">
                <el-input v-model="planBody.name" maxlength="50"></el-input>
              </el-form-item>
              <el-form-item label="正射图：" required prop="orthophoto_id">
                <el-select
                  v-model="selectedOrthophotoId"
                  placeholder="请选择正射图"
                  @change="handleOrthophotoChange"
                >
                  <el-option
                    v-for="item in orthophotoTable"
                    :label="item.name"
                    :value="item.id"
                    :key="item.id"
                  ></el-option>
                </el-select>
              </el-form-item>
              <el-form-item
                label="光伏板区域："
                required
                prop="solar_panel_id"
              >
                <el-select
                  v-model="selectedSolarIds"
                  multiple
                  collapse-tags
                  :disabled="!selectedOrthophotoId"
                  placeholder="请选择光伏板区域"
                  @change="handleSolarPanelChange"
                >
                  <el-option
                    v-for="item in filteredSolarTable"
                    :label="item.solar_panel_area_name"
                    :value="item.id"
                    :key="item.id"
                  ></el-option>
                </el-select>
              </el-form-item>
              <div
                class="wayline-panel"
                style="padding-top: 5px; background-color: #081B39; margin-bottom: 15px;"
                v-if="planBody.file_id"
              >
                <div class="title">
                  <el-tooltip :content="wayline.name">
                    <div class="wayline-name">{{ wayline.name }}</div>
                  </el-tooltip>
                </div>
                <div
                  class="ml10 mt5"
                  style="color: rgba(65, 176, 255, 1); font-weight: 500; font-size: 14px; text-align: left;"
                >
                  <span><el-icon><Promotion /></el-icon></span>
                  <span
                    class="ml5"
                    >{{ DEVICE_NAME[wayline.drone_model_key] }}</span
                  >
                  <span class="ml10"
                    ><el-icon><Camera /></el-icon></span>
                  <span
                    class="ml5"
                    v-for="payload in wayline.payload_model_keys"
                    :key="payload.id"
                  >
                    {{ DEVICE_NAME[payload] }}
                  </span>
                </div>
                <div
                  class="mt5 ml10"
                  style="color: rgba(65, 176, 255, 1); font-weight: 500; font-size: 14px;text-align: left;"
                >
                  <span class="mr10"
                    >更新于{{ new Date(wayline.update_time).toLocaleString() }}</span
                  >
                </div>
              </div>

              <el-form-item label="执行设备：" required prop="dock_sn">
                <el-button
                  type="primary"
                   style="background-color: rgba(7, 75, 208, 1); width: 100px; border: 1px solid rgba(0, 64, 147, 1)"
                  @click="selectDevice"
                >
                  选择设备
                </el-button>
              </el-form-item>
              <div
                class="panel"
                style="padding-top: 5px; background-color: #081B39; margin-bottom: 15px;"
              >
                <div class="title">
                  <el-tooltip :content="dock.nickname">
                    <div class="wayline-name">{{ dock.nickname }}</div>
                  </el-tooltip>
                </div>
                <div
                  class="ml10 mt5"
                  style="color: rgba(65, 176, 255, 1); font-weight: 500; font-size: 14px;text-align: left;"
                >
                  <span><el-icon><Promotion /></el-icon></span>
                  <span
                    class="ml5"
                    >{{ dock.children?.nickname ?? 'No drone' }}</span
                  >
                </div>
              </div>
              <el-form-item
                label="时间方案："
                required
                prop="task_type"
              >
                <el-radio-group v-model="planBody.task_type" size="large">
                  <el-radio
                    v-for="type in TaskTypeOptions"
                    :value="type.value"
                    :key="type.value"
                    class="radio-custom"
                    >{{ type.label }}</el-radio
                  >
                </el-radio-group>
              </el-form-item>
              <el-form-item
                label="开始时间："
                required
                prop="begin_time"
                v-if="planBody.task_type === TaskType.Timed || planBody.task_type === TaskType.Condition"
              >
                <el-date-picker
                  style="width:250px"
                  v-model="planBody.begin_time"
                  type="datetime"
                  placeholder="选择日期和时间"
                  value-format="x"
                  format="YYYY-MM-DD HH:mm:ss"
                />
              </el-form-item>
              <el-form-item label="返航高度(m)：" prop="rth_altitude" required>
                <el-input v-model="planBody.rth_altitude"></el-input>
              </el-form-item>

              <el-form-item
                label="航线规划："
                required
                prop="type"
              >
                <el-radio-group v-model="planBody.type" size="large">
                  <el-radio value="uniform">自定义</el-radio>
                  <el-radio value="auto">自适应</el-radio>
                </el-radio-group>
              </el-form-item>

              <!-- 第一步底部按钮 -->

               <div class="footer">
                  <el-button
                    class="mr10"
                    style="background-color: rgba(255, 255, 255, 0.05); width: 100px; border: 1px solid rgba(206, 227, 255, 0.42);"
                    @click="closePlan"
                    >取消
                  </el-button>
                  <el-button
                    class="mr10"
                    style="background-color: rgba(255, 255, 255, 0.05); width: 100px; border: 1px solid rgba(206, 227, 255, 0.42);"
                    @click="handleNextStep"
                    >下一步
                  </el-button>
                </div>
            </div>

            <!-- 第二步: 光伏参数配置 -->
            <div v-if="currentStep === 2">
              <!-- 红外/可见光选择 (uniform & auto 共有) -->
              <el-form-item label="相机类型：" required prop="image_format_list">
                <el-checkbox-group v-model="planBody.image_format_list" size="large">
                  <el-checkbox value="visable">可见光</el-checkbox>
                  <el-checkbox value="ir">红外</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <div
                v-for="config in areaConfigs"
                :key="config.solar_panel_id"
                class="area-config-card"
              >
                <div class="area-config-title">{{ config.solar_panel_name }}</div>
                <el-form-item label="航线高度：" required>
                  <el-input v-model="config.flight_altitude" type="number" placeholder="请输入航线高度(米)"></el-input>
                </el-form-item>
                <el-form-item label="光伏板朝向：">
                  <el-input v-model="config.panel_heading" type="number" placeholder="请输入光伏板朝向"></el-input>
                </el-form-item>

                <!-- uniform 模式下额外字段 -->
                <template v-if="planBody.type === 'uniform'">
                  <el-form-item label="横向航线数：" required>
                    <el-input v-model="config.horizontal_lines" type="number" placeholder="请输入横向航线数"></el-input>
                  </el-form-item>
                  <el-form-item label="区域边距：" required>
                    <el-input v-model="config.margin" type="number" placeholder="请输入区域边距"></el-input>
                  </el-form-item>
                  <el-form-item label="航线内点数：" required>
                    <el-input v-model="config.points_per_line" type="number" placeholder="请输入航线内点数"></el-input>
                  </el-form-item>
                </template>

                <!-- auto 模式下字段 -->
                <template v-if="planBody.type === 'auto'">
                  <el-form-item label="光伏板倾角：">
                    <el-input v-model="config.panel_tilt" type="number" placeholder="请输入光伏板倾角(度)"></el-input>
                  </el-form-item>
                </template>
              </div>

              <!-- 第二步底部按钮 -->
              <div class="footer footer-actions">
                  <el-button
                    style="background-color: rgba(255, 255, 255, 0.05); width: 100px; border: 1px solid rgba(206, 227, 255, 0.42);"
                    @click="handlePrevStep"
                    >上一步
                  </el-button>
                  <el-button
                    type="primary"
                    style="background-color: rgba(7, 75, 208, 1); width: 100px; border: 1px solid rgba(0, 64, 147, 1)"
                    @click="previewWayline"
                    >航线预览
                  </el-button>
                  <el-button
                    type="primary"
                    style="background-color: rgba(7, 75, 208, 1); width: 100px; border: 1px solid rgba(0, 64, 147, 1)"
                    @click="onSubmit"
                    >确认
                  </el-button>
              </div>
            </div>
          </el-form>
        </div>
      </div>
    </div>
    <div class="box-right" :class="{ 'full-width': viewMode === '3d' }">
      <div class="view-toggle">
        <el-button-group>
          <el-button :type="viewMode === '2d' ? 'primary' : ''" @click="viewMode = '2d'" size="small">二维视图</el-button>
          <el-button :type="viewMode === '3d' ? 'primary' : ''" @click="viewMode = '3d'" size="small">三维视图</el-button>
        </el-button-group>
        <el-button v-if="viewMode === '3d'" type="warning" @click="viewMode = '2d'" size="small" style="margin-left: 10px;">返回二维</el-button>
      </div>
      <div class="view-content">
        <loadSolarPanel v-if="viewMode === '2d'" :imagePath="selectedImagePath" :detectAreas="selectedDetectAreas" :waylineInfo="waylineInfo"/>
        <keep-alive>
          <Solar3DRouteEditor v-if="viewMode === '3d'" container-id="solar3dPreviewContainer" :embedded="true" :plan-id="solarCreatePlanId" />
        </keep-alive>
      </div>
    </div>
  </div>

  <el-drawer
    v-model="drawerVisible"
    title="选择设备"
    size="300px"
    :with-header="false"
    :modal="false"
    style="background: #282828; color: white;"
  >
    <div style="position: relative; height: 100%;">
      <div style="position: absolute; top: 15px; right: 10px; z-index: 10;">
        <el-button link style="color: white;" @click="closePanel">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <SelectDock />
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { computed, onMounted, onUnmounted, reactive, ref, toRaw, UnwrapRef, nextTick } from 'vue'
import { Promotion, Camera, Close } from '@element-plus/icons-vue'
import { ELocalStorageKey, ERouterName } from '/@/types'
import { useMyStore } from '/@/store'
import { WaylineType, WaylineFile } from '/@/types/wayline'
import { Device, DEVICE_NAME } from '/@/types/device'
import { createPlan, CreatePlan, createFlyPlan } from '/@/api/wayline'
import { saveSolar3DPreviewPayload } from '/@/api/solar3d-route'
import { getRoot } from '/@/root'
import { TaskType, OutOfControlActionOptions, OutOfControlAction, TaskTypeOptions } from '/@/types/task'
import moment, { Moment } from 'moment'
import { ElTable, ElTableColumn, ElFormItem, ElForm, ElInput, ElButton, ElSelect, ElOption, ElPagination, ElContainer, ElHeader, ElMain, ElFooter, ElDialog, ElMessage, ElMessageBox, ElText, ElLink, ElTag, ElTooltip } from 'element-plus'
import loadSolarPanel from '/@/components/task/solarflyPlan/loadSolarPanel.vue'
import Solar3DRouteEditor from '/@/components/cesium/Solar3DRouteEditor.vue'
import SelectDock from '/@/pages/page-web/projects/dock.vue'
import { useRouter, useRoute } from 'vue-router'
import { getAllWindTurbineApi, getAllInserestPointApi, getAllSolarPanelApi, getSolarPanelImgByIdApi, getOrthophotoListApi, getSolarPanelByIdApi } from '/@/api/turbine/turbineMgt'
const router = useRouter()
const route = useRoute()
const store = useMyStore()
const selectType = ref('')
const viewMode = ref<'2d' | '3d'>('2d')

function createSolarPlanId () {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID()
  }
  return `solar-plan-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

const solarCreatePlanId = createSolarPlanId()
console.log('新建光伏计划周期 planId:', solarCreatePlanId)

const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!

const wayline = computed<WaylineFile>(() => {
  return store.state.waylineInfo
})

const dock = computed<Device>(() => {
  return store.state.dockInfo
})
const orthophotoTable = ref([]) // 正射图列表
const solarTable = ref([]) // 光伏板区域列表
const selectedOrthophotoId = ref('')
const selectedImagePath = ref('') // 选中光伏板区域对应的正射图path
const selectedDetectAreas = ref<any[]>([]) // 选中光伏板区域的检测区域（支持多选）
const selectedSolarIds = ref<string[]>([]) // 选中的光伏板区域ID数组
const waylineInfo = ref()
const filteredSolarTable = computed(() => {
  if (!selectedOrthophotoId.value) {
    return []
  }
  return solarTable.value.filter((item: any) => item.orthophoto_id === selectedOrthophotoId.value)
})

interface AreaConfig {
  solar_panel_id: string
  solar_panel_name: string
  flight_altitude: string
  panel_heading: string
  margin: string
  horizontal_lines: string
  points_per_line: string
  panel_tilt: string
}

const areaConfigs = ref<AreaConfig[]>([])

const planBody = reactive({
  plan_source: '系统创建',
  name: '',
  orthophoto_id: '',
  file_id: computed(() => store?.state?.waylineInfo.id),
  dock_sn: computed(() => store?.state?.dockInfo.device_sn),
  workspace_id: localStorage.getItem(ELocalStorageKey.WorkspaceId)!,
  task_type: TaskType.Immediate,
  begin_time: '',
  end_time: '',
  status: 1,
  fan_id: '',
  poi_id: '',
  solar_panel_id: '',
  poi_orbit_num: 1,
  username: 'pilot',
  plan_type: '4',
  rth_altitude: '',
  out_of_control: 0, // 默认返回
  enable_status: 0,
  plan_priority: 1,
  type: 'auto',
  image_format_list: ['visable'], // 相机类型多选
  image_format: computed(() => planBody.image_format_list.join(',')), // 提交时转为逗号分隔字符串
  index: 0 // index为0时不会向数据库存航线且会返回wayline（航线的具体信息），为1时不返回wayline，会存入数据库
})

const drawerVisible = ref(false)
const valueRef = ref()
const currentStep = ref(1) // 当前步骤: 1=基础信息, 2=光伏参数
const rules = {
  rth_altitude: [
    { required: true, message: '请输入返航高度', trigger: 'blur' },
    {
      pattern: /^[1-9]\d*$/,
      message: '返航高度必须是正整数',
      trigger: 'submit'
    },
    {
      validator: (rule, value, callback) => {
        const numValue = Number(value)
        if (numValue < 30) {
          callback(new Error('返航高度不能低于30米'))
        } else if (numValue > 1500) {
          callback(new Error('返航高度不能超过1500米'))
        } else {
          callback()
        }
      },
      trigger: 'submit'
    }
  ],
  poi_orbit_num: [
    { required: true, message: '请输入环绕点数量', trigger: 'blur' },
    {
      pattern: /^[1-9]\d*$/,
      message: '环绕点数量必须是正整数',
      trigger: 'blur'
    },
    {
      validator: (rule, value, callback) => {
        const numValue = Number(value)
        if (numValue < 1) {
          callback(new Error('环绕点最低为1'))
        } else if (numValue > 20) {
          callback(new Error('返环绕点最高为20'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  poi_id: [{ required: true, message: '请选择兴趣点', trigger: 'change' }],
  fan_id: [{ required: true, message: '请选择风机', trigger: 'blur' }],
  solar_panel_id: [{ required: true, message: '请选择光伏板区域', trigger: 'blur' }],
  orthophoto_id: [{ required: true, message: '请选择正射图', trigger: 'change' }],
  name: [
    { required: true, message: '请输入计划名称', trigger: 'blur' },
    { min: 1, max: 50, message: '计划名称长度在1-50个字符', trigger: 'blur' }
  ],

  file_id: [
    { required: true, message: '请选择航线', trigger: ['blur', 'change'] }
  ],

  dock_sn: [
    { required: true, message: '请选择设备', trigger: ['blur', 'change'] }
  ],

  task_type: [
    { required: true, message: '请选择时间方案', trigger: 'change' }
  ],

  begin_time: [
    {
      validator: (rule, value, callback) => {
        if ((planBody.task_type === TaskType.Timed || planBody.task_type === TaskType.Condition) && !value) {
          callback(new Error('请选择开始时间'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],

  out_of_control: [
    { required: true, message: '请选择失联动作', trigger: 'change' }
  ],
  // 第二步表单校验规则
  image_format_list: [
    { required: true, message: '请选择相机类型', trigger: 'change' }
  ]
}

onMounted(async () => {
  await getOrthophoto()
  getSolarPanel()
})

async function getOrthophoto () {
  try {
    const res = await getOrthophotoListApi({
      pageSize: 10000,
      pageNo: 1,
      name: '',
      id: ''
    })
    if (res.code !== 0) {
      return
    }
    orthophotoTable.value = res.data.list || []
  } catch (error) {
    console.error('加载正射图列表失败:', error)
  }
}

/**
 * @description: 查询光伏板ID列表
 * @param {string}
 * */
function getSolarPanel () {
  try {
    getAllSolarPanelApi({
      pageSize: 10000,
      pageNo: 1,
      solar_panel_area_name: '',
      id: '',
      orthophoto_id: ''
    }).then(res => {
      if (res.code !== 0) {
        return
      }
      solarTable.value = res.data.list || []
    })
  } catch (error) {
  }
}

// 加载正射图列表
async function getOrthophotoPath (id:any) {
  try {
    const res = await getSolarPanelImgByIdApi(id)
    if (res.code === 0) {
      return res.data.path
    }
    return ''
  } catch (error) {
    console.error('加载正射图列表失败:', error)
  }
}

function createAreaConfig (item: any): AreaConfig {
  return {
    solar_panel_id: item.id,
    solar_panel_name: item.solar_panel_area_name || item.name || item.id,
    flight_altitude: '',
    panel_heading: '',
    margin: '',
    horizontal_lines: '',
    points_per_line: '',
    panel_tilt: item.tilt_angle !== undefined && item.tilt_angle !== null ? String(item.tilt_angle) : ''
  }
}

function syncAreaConfigs (items: any[]) {
  areaConfigs.value = items.map((item: any) => {
    const existing = areaConfigs.value.find(config => config.solar_panel_id === item.id)
    return existing || createAreaConfig(item)
  })
}

function isEmptyValue (value: string) {
  return value === null || value === undefined || value === ''
}

function isPositiveInteger (value: string) {
  return /^[1-9]\d*$/.test(String(value))
}

function isNumberValue (value: string) {
  return /^[0-9]+(\.[0-9]+)?$/.test(String(value))
}

function validateAreaConfigs () {
  if (areaConfigs.value.length === 0) {
    ElMessage.warning('请选择光伏板区域')
    return false
  }

  for (const config of areaConfigs.value) {
    if (isEmptyValue(config.flight_altitude) || !isPositiveInteger(config.flight_altitude)) {
      ElMessage.warning(`请填写有效的航线高度：${config.solar_panel_name}`)
      return false
    }
    if (planBody.type === 'uniform') {
      if (isEmptyValue(config.panel_heading) || !isNumberValue(config.panel_heading)) {
        ElMessage.warning(`请填写有效的光伏板朝向：${config.solar_panel_name}`)
        return false
      }
      if (isEmptyValue(config.horizontal_lines) || !isPositiveInteger(config.horizontal_lines)) {
        ElMessage.warning(`请填写有效的横向航线数：${config.solar_panel_name}`)
        return false
      }
      if (isEmptyValue(config.margin) || !isNumberValue(config.margin)) {
        ElMessage.warning(`请填写有效的区域边距：${config.solar_panel_name}`)
        return false
      }
      if (isEmptyValue(config.points_per_line) || !isPositiveInteger(config.points_per_line)) {
        ElMessage.warning(`请填写有效的航线内点数：${config.solar_panel_name}`)
        return false
      }
    }
  }

  return true
}

function getAreaConfigPayload () {
  return areaConfigs.value.map(config => {
    const baseConfig: any = {
      solar_panel_id: config.solar_panel_id,
      flight_altitude: Number(config.flight_altitude),
      panel_heading: Number(config.panel_heading)
    }

    if (planBody.type === 'uniform') {
      return {
        ...baseConfig,
        margin: Number(config.margin),
        horizontal_lines: Number(config.horizontal_lines),
        points_per_line: Number(config.points_per_line)
      }
    }

    return {
      ...baseConfig,
      panel_tilt: Number(config.panel_tilt)
    }
  })
}

function getSubmitPlanBody () {
  return {
    ...toRaw(planBody),
    file_id: planBody.file_id,
    dock_sn: planBody.dock_sn,
    image_format: planBody.image_format,
    plan_id: solarCreatePlanId,
    planId: solarCreatePlanId,
    orthophoto_id: planBody.orthophoto_id,
    area_configs: getAreaConfigPayload()
  }
}

// 光伏板区域选择变化（支持多选）
async function handleSolarPanelChange (ids: string[]) {
  selectedImagePath.value = ''
  selectedDetectAreas.value = []

  if (!ids || ids.length === 0) {
    planBody.solar_panel_id = ''
    areaConfigs.value = []
    return
  }

  selectedSolarIds.value = ids
  planBody.solar_panel_id = ids.join(',')
  const items = solarTable.value.filter((item: any) => ids.includes(item.id) && item.orthophoto_id === selectedOrthophotoId.value)
  syncAreaConfigs(items)
  selectedDetectAreas.value = items
  if (items.length > 0) {
    selectedImagePath.value = await getOrthophotoPath(selectedOrthophotoId.value)
  }
}

async function handleOrthophotoChange (id: string) {
  planBody.orthophoto_id = id
  planBody.solar_panel_id = ''
  selectedSolarIds.value = []
  selectedDetectAreas.value = []
  areaConfigs.value = []
  waylineInfo.value = []
  selectedImagePath.value = ''
  if (!id) {
    return
  }
  selectedImagePath.value = await getOrthophotoPath(id)
}

// 返回
function closePlan () {
  router.push({ path: '/taskManage/solarPlanMgt' })
}

function closePanel () {
  drawerVisible.value = false
  selectType.value = ''
}

function selectDevice () {
  drawerVisible.value = true
  selectType.value = '2'
}

/**
 * @description: 下一步 - 校验第一步表单并切换到第二步
 */
async function handleNextStep () {
  try {
    // 只校验第一步的字段
    const valid = await valueRef.value.validateField([
      'name', 'orthophoto_id', 'solar_panel_id', 'dock_sn', 'task_type', 'begin_time', 'rth_altitude', 'type'
    ])
    if (valid) {
      currentStep.value = 2
    }
  } catch (error) {
    ElMessage.warning('请填写必填项!')
  }
}

/**
 * @description: 上一步 - 返回第一步
 */
function handlePrevStep () {
  currentStep.value = 1
}

/**
 * 预览航线
 */
async function previewWayline () {
  try {
    const valid = await valueRef.value.validateField(['image_format_list'])
    if (valid && validateAreaConfigs()) {
      planBody.index = 0
      const res = await createFlyPlan(getSubmitPlanBody())
      if (res.code !== 0) {
        ElMessage.error('航线预览异常!')
        return
      }
      waylineInfo.value = res.data.wayline.route_2d.area.points
      const threeDPayload = res.data.wayline.three_d_payload
      if (threeDPayload) {
        threeDPayload.planId = solarCreatePlanId
        threeDPayload.route_draft_id = solarCreatePlanId
        saveSolar3DPreviewPayload(threeDPayload)
        window.postMessage({
          type: 'SOLAR_3D_ROUTE_PREVIEW',
          payload: threeDPayload
        }, '*')
      }
    }
  } catch (error) {
    ElMessage.warning('请填写必填项!')
  }
}

/**
 * 创建航线
 */
async function onSubmit () {
  try {
    const valid = await valueRef.value.validateField(['image_format_list'])
    if (valid && validateAreaConfigs()) {
      // 1.创建飞行计划
      planBody.index = 1
      const res = await createFlyPlan(getSubmitPlanBody())
      if (res.code !== 0) {
        ElMessage.warning('请填写必填项!')
        return
      }
      ElMessage.success('创建成功!')
      // 返回飞行计划管理页面
      closePlan()
    }
  } catch (error) {
    ElMessage.warning('请填写必填项!')
  }
}

</script>

<style lang="scss" scoped>
.main-box {
  display: flex;
  /* 使用 flexbox 布局 */
  height: 100vh;
  /* 设置容器高度为视口高度 */
}

//表单样式
:deep(.el-input) {
  --el-input-border-color: #1d4292;
}
:deep(.el-input__wrapper){
  background-color: #041b75;
}
:deep(.el-input__inner){
  color: white;
}
:deep(.el-select__placeholder){
  color: rgb(198, 196, 196);
}
:deep(.el-select__wrapper){
  background-color: #041b75;
  box-shadow: none;
  border: 1px solid #1d4292;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-form-item__label) {
  color: #fff;
  font-size: 14px;
}

.box-left {
  background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  width: 440px;
  /* 左侧占据 20% 的宽度 */
  // background-color: #4CAF50; /* 绿色背景 */
  padding: 10px;
  /* 内边距 */
  color: white;
  /* 字体颜色 */
  // transition: 0.5s;
  border: none;
  height:calc(100vh - 110px);
  .box_title{
    width: 400px;
    height: 37px;;
    background: url('/@/assets/v4/plan_title.png') 100% no-repeat;
    background-size: 100% 100%;

    display: flex;          /* 启用 flexbox */
    justify-content: left;  /* 水平左对齐 */
    align-items: center;    /* 垂直居中对齐 */
    padding-left: 10px;     /* 根据需要添加左内边距 */
    margin-bottom: 10px;
    .thumbnail_1{
      width: 22px;
      height: 22px;
      margin-right: 15px;
      background: url('/@/assets/v4/plan_icon1.png') 100% no-repeat;
      background-size: 100% 100%;
    }
    .box_text{
      text-shadow: 0px 0px 4px rgba(201, 252, 255, 0.41);
      background-image: linear-gradient(
        180deg,
        rgba(255, 255, 255, 1) 0,
        rgba(144, 201, 255, 1) 100%
      );
      font-size: 20px;
      font-family: Google Sans-Medium;
      font-weight: 500;
      text-align: justified;
      white-space: nowrap;
      line-height: 30px;
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;

    }
  }
}
.box-right {
  background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  flex: 1;
  /* 右侧占据剩余空间 */
  width: calc(80% - 10px);
  margin-left: 10px;
  // background-color: #2196F3; /* 蓝色背景 */
  padding: 20px;
  /* 内边距 */
  color: white;
  /* 字体颜色 */
  // border-radius: 15px;
  border: none;
  height:calc(100vh - 110px);
  position: relative;
  display: flex;
  flex-direction: column;
}
.view-toggle {
  text-align: right;
  margin-bottom: 10px;
  flex-shrink: 0;
}
.view-content {
  flex: 1;
  overflow: hidden;
}
.box-right.full-width {
  width: 100%;
  margin-left: 0;
}
.create-plan-wrapper {
  background-color: #06265a;
  color: #fff;
  display: flex;
  flex-direction: column;
  width: 400px;
  overflow-y: auto;
  height: calc(100vh - 210px);
  padding: 20px;
  border: 2px solid rgba(0, 79, 169, 0.4);
  // box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);

  ::-webkit-scrollbar {
    display: none;
  }

  .content {
    height: calc(100% - 100px);

    form {
      margin: 0;
    }
  }

  .footer {
    margin-bottom: 40px;
    display: flex;
    justify-content: center;
    gap: 12px;
    padding:10px 0;

    button {
      width: 45%;
      color: #fff ;
      border: 0;
      margin-left: 0;
    }
  }

  .footer-actions {
    width: 100%;
  }
}

.area-config-card {
  padding: 14px 12px 2px;
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(206, 227, 255, 0.18);
  border-radius: 4px;
}

.area-config-title {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  line-height: 20px;
  margin-bottom: 12px;
  text-align: left;
}

.wayline-name{
  background-color: #06142B; text-shadow: 0px 0px 4px rgba(52, 191, 255, 0.5);
  background-image: linear-gradient(
    180deg,
    rgba(255, 255, 255, 1) 0,
    rgba(190, 235, 255, 1) 51.999998%,
    rgba(130, 217, 255, 1) 100%
  );
  font-size: 16px;
  width: 100%;
  padding:0 20px;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  font-family: Google Sans-Medium;
  font-weight: 550;
  text-align: center;
  white-space: nowrap;
  line-height: 20px;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
/* .block_2 已替换为 el-button */
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
    color: white;
    flex-direction: row;
    align-items: center;
    height: 30px;
    font-weight: bold;
    margin: 0px 10px 0 10px;
  }
}
.radio-custom{
  :deep(.el-radio-button__inner) {
    box-shadow: inset 0px 0px 15px 1px rgba(154, 206, 255, 0.5) ;
    background-color: rgba(24, 118, 224, 0.1) ;
    // border-radius: 4px;
    height: 36px;
    margin-left: 7px;
    width: 100px;
    border: none !important;
  }

  :deep(el-radio-button__original-radio:checked+.el-radio-button__inner) {
    background-image: linear-gradient(
      180deg,
      rgba(70, 145, 217, 1) 0,
      rgba(21, 81, 181, 1) 100%
    ) !important;
    background-color: transparent !important;
    height: 36px;
    margin-left: 7px;
    width: 100px;
    border: none !important;
    // border-radius: 4px;
  }
  :deep(.el-radio-button:first-child .el-radio-button__inner){
    box-shadow: inset 0px 0px 15px 1px rgba(154, 206, 255, 0.5) !important;
    background-color: rgba(24, 118, 224, 0.1) !important;
  }

}

.radio-custom{
  ::v-deep(.el-radio-button__inner) {
    box-shadow: inset 0px 0px 15px 1px rgba(154, 206, 255, 0.5) ;
    background-color: rgba(24, 118, 224, 0.1) ;
    // border-radius: 4px;
    height: 36px;
    margin-left: 7px;
    width: 110px;
    border: none !important;
  }

  ::v-deep(el-radio-button__original-radio:checked+.el-radio-button__inner) {
    background-image: linear-gradient(
      180deg,
      rgba(70, 145, 217, 1) 0,
      rgba(21, 81, 181, 1) 100%
    ) !important;
    background-color: transparent !important;
    height: 36px;
    margin-left: 7px;
    width: 110px;
    border: none !important;
    // border-radius: 4px;
  }
  ::v-deep(.el-radio-button:first-child .el-radio-button__inner){
    box-shadow: inset 0px 0px 15px 1px rgba(154, 206, 255, 0.5) !important;
    background-color: rgba(24, 118, 224, 0.1) !important;
  }

}
.radio-custom1{
  ::v-deep(.el-radio-button__inner) {
    box-shadow: inset 0px 0px 15px 1px rgba(154, 206, 255, 0.5) ;
    background-color: rgba(24, 118, 224, 0.1) ;
    // border-radius: 4px;
    height: 36px;
    margin-left: 7px;
    width: 115px;
    border: none !important;
  }

  :deep(el-radio-button__original-radio:checked+.el-radio-button__inner) {
    background-image: linear-gradient(
      180deg,
      rgba(70, 145, 217, 1) 0,
      rgba(21, 81, 181, 1) 100%
    ) !important;
    background-color: transparent !important;
    height: 36px;
    margin-left: 7px;
    width: 115px;
    border: none !important;
    // border-radius: 4px;
  }
  ::v-deep(.el-radio-button:first-child .el-radio-button__inner){
    box-shadow: inset 0px 0px 15px 1px rgba(154, 206, 255, 0.5) !important;
    background-color: rgba(24, 118, 224, 0.1) !important;
  }

}

// 下拉框

// 下拉框
.select-operation {
  :deep(.el-select__placeholder) {
    font-size: 14px;
    font-weight: 500;
    color: #A8ABB2;
  }

  :deep(.el-select__wrapper) {

    // background: rgba(59, 116, 255, 0.15);
    background-color: #0B2756;
    // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    // border: 1px solid #719fff;
    // border-radius: 4px;
    width: 360px;
    height: 30px;
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
.radio-custom{

}

.panel {
  background: #3c3c3c;
  margin-left: auto;
  margin-right: auto;
  margin-top: 10px;
  height: 70px;
  width: 95%;
  font-size: 13px;
  border-radius: 2px;
  cursor: pointer;
  .title {
    display: flex;
    color: white;
    flex-direction: row;
    align-items: center;
    height: 30px;
    font-weight: bold;
    margin: 0px 10px 0 10px;
  }
}
</style>
