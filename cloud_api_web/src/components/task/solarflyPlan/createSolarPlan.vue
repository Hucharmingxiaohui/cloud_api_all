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
              <div class="auto-task-row">
                <el-button
                  class="auto-task-button"
                  :disabled="!selectedOrthophotoId"
                  @click="openAutoDialog"
                >
                  自动生成任务
                </el-button>
                <span class="auto-task-tip">按巡检面积推荐单机/多机协同，并就近分配在线机巢</span>
              </div>

              <el-form-item label="任务分组：" required>
                <div class="task-groups">
                  <div
                    v-for="(group, groupIndex) in taskGroups"
                    :key="group.groupId"
                    class="task-group-card"
                  >
                    <div class="task-group-header">
                      <span class="task-group-title">分组 {{ groupIndex + 1 }}</span>
                      <el-button
                        v-if="taskGroups.length > 1"
                        link
                        type="danger"
                        @click="removeTaskGroup(group)"
                        >删除
                      </el-button>
                    </div>
                    <div class="task-group-row">
                      <el-button
                        type="primary"
                        style="background-color: rgba(7, 75, 208, 1); border: 1px solid rgba(0, 64, 147, 1)"
                        @click="selectDevice(group)"
                      >
                        {{ group.dock ? '更换设备' : '选择设备' }}
                      </el-button>
                      <span class="task-group-device-name">
                        {{ group.dock ? `${group.dock.nickname}（${group.dock.children?.nickname ?? 'No drone'}）` : '未选择设备' }}
                      </span>
                    </div>
                    <el-select
                      :model-value="group.solarIds"
                      multiple
                      collapse-tags
                      :disabled="!selectedOrthophotoId"
                      placeholder="请选择光伏板区域"
                      @change="(ids) => handleGroupAreasChange(group, ids)"
                    >
                      <el-option
                        v-for="item in groupAreaOptions(group)"
                        :label="item.solar_panel_area_name"
                        :value="item.id"
                        :key="item.id"
                      ></el-option>
                    </el-select>
                  </div>
                  <el-button class="add-group-button" @click="addTaskGroup">+ 添加分组</el-button>
                </div>
              </el-form-item>
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
                  v-for="group in taskGroups"
                  :key="group.groupId"
                  class="device-group-section"
                >
                  <div class="device-group-header">
                    <span class="device-group-name">{{ group.dock?.nickname ?? '未选择设备' }}</span>
                    <el-button
                      size="small"
                      type="primary"
                      style="background-color: rgba(7, 75, 208, 1); border: 1px solid rgba(0, 64, 147, 1)"
                      :disabled="group.submitted"
                      @click="previewWayline(group)"
                    >
                      {{ group.submitted ? '已创建' : '预览航线' }}
                    </el-button>
                  </div>
                  <div
                    v-for="config in groupAreaConfigs(group)"
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

  <el-dialog
    v-model="autoDialogVisible"
    title="自动生成任务"
    width="480px"
    append-to-body
  >
    <div class="auto-task-body">
      <el-select
        v-model="autoAreaIds"
        multiple
        collapse-tags
        placeholder="请选择巡检区域"
        style="width: 100%"
      >
        <el-option
          v-for="item in filteredSolarTable"
          :label="item.solar_panel_area_name"
          :value="item.id"
          :key="item.id"
        ></el-option>
      </el-select>
      <el-button
        type="primary"
        style="width: 100%; margin-top: 12px; background-color: rgba(7, 75, 208, 1); border: 1px solid rgba(0, 64, 147, 1)"
        @click="generateAutoTask"
      >
        开始生成
      </el-button>

      <template v-if="autoResult">
        <div class="auto-result-summary">
          <div class="auto-result-item">
            <span>执行方式：</span>
            <strong>{{ autoResult.mode === 'single' ? '单机执行' : '多机协同' }}</strong>
          </div>
          <div class="auto-result-item">
            <span>推荐无人机数量：</span>
            <strong>{{ autoResult.droneCount }} 架</strong>
          </div>
          <div class="auto-result-item">
            <span>巡检总面积：</span>
            <strong>{{ autoResult.totalAreaMu.toFixed(1) }} 亩（{{ Math.round(autoResult.totalAreaM2) }} ㎡）</strong>
          </div>
          <div class="auto-result-item">
            <span>预计巡检时间：</span>
            <strong>约 {{ autoResult.estimatedMinutes }} 分钟</strong>
          </div>
        </div>
        <div
          v-for="assignment in autoResult.assignments"
          :key="assignment.sn"
          class="auto-result-card"
        >
          <div class="auto-result-dock">{{ assignment.nickname }}</div>
          <div class="auto-result-areas">
            {{ assignment.areaNames.length ? assignment.areaNames.join('、') : '（无分配区域）' }}
          </div>
          <div class="auto-result-distance">
            负责区域距机巢最远约 {{ Number.isFinite(assignment.distanceM) ? Math.round(assignment.distanceM) + ' 米' : '--' }}
          </div>
        </div>
        <div
          v-for="(warning, index) in autoWarnings"
          :key="index"
          class="auto-result-warning"
        >
          {{ warning }}
        </div>
      </template>

      <div class="auto-dialog-footer">
        <el-button @click="autoDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!autoResult" @click="applyAutoTask">应用到任务分组</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
import { computed, onMounted, onUnmounted, reactive, ref, toRaw, UnwrapRef, nextTick, watch } from 'vue'
import { Promotion, Camera, Close } from '@element-plus/icons-vue'
import { EDeviceTypeName, ELocalStorageKey, ERouterName } from '/@/types'
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
import { getBindingDevices } from '/@/api/manage'
import { AutoTaskRecommendation, computeAreaGeometry, DockPosition, recommendAutoTask } from './solarAutoTask'
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

const orthophotoTable = ref([]) // 正射图列表
const solarTable = ref([]) // 光伏板区域列表
const selectedOrthophotoId = ref('')
const selectedImagePath = ref('') // 选中光伏板区域对应的正射图path
const waylineInfo = ref() // 当前预览分组的航线（2D）
const filteredSolarTable = computed(() => {
  if (!selectedOrthophotoId.value) {
    return []
  }
  return solarTable.value.filter((item: any) => item.orthophoto_id === selectedOrthophotoId.value)
})

// 任务分组：每个分组绑定一个机场设备和该设备负责的光伏板区域
interface DeviceTaskGroup {
  groupId: string
  planId: string
  dock: Device | null
  solarIds: string[]
  submitted: boolean
}

function createTaskGroup (): DeviceTaskGroup {
  return {
    groupId: createSolarPlanId(),
    planId: createSolarPlanId(),
    dock: null,
    solarIds: [],
    submitted: false
  }
}

const taskGroups = ref<DeviceTaskGroup[]>([createTaskGroup()])
const activeDeviceGroupId = ref('') // 正在通过抽屉选择设备的分组
// 所有分组已选区域的合集，用于 2D 叠加显示与参数配置同步
const selectedDetectAreas = computed(() => {
  const ids = taskGroups.value.flatMap(group => group.solarIds)
  return solarTable.value.filter((item: any) => ids.includes(item.id) && item.orthophoto_id === selectedOrthophotoId.value)
})

// 抽屉打开期间，把用户在 SelectDock 中点击的设备赋给对应分组
watch(() => store.state.dockInfo, (info) => {
  if (!activeDeviceGroupId.value || !info?.device_sn) return
  const group = taskGroups.value.find(item => item.groupId === activeDeviceGroupId.value)
  if (group) {
    group.dock = { ...toRaw(info) }
  }
})

// ===================== 自动生成任务 =====================
const boundDocks = ref<any[]>([]) // 绑定的机巢清单（自动生成任务用）
const autoDialogVisible = ref(false)
const autoAreaIds = ref<string[]>([])
const autoResult = ref<AutoTaskRecommendation | null>(null)
const autoWarnings = ref<string[]>([])

// 有实时坐标的在线机巢（离线机巢无坐标，不参与就近分配）
const onlineDockPositions = computed<DockPosition[]>(() => {
  return boundDocks.value
    .map((dock: any) => {
      const osd = store.state.deviceState.dockInfo[dock.device_sn]
      const lng = Number(osd?.basic_osd?.longitude)
      const lat = Number(osd?.basic_osd?.latitude)
      if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null
      return { sn: dock.device_sn, nickname: dock.nickname, lng, lat }
    })
    .filter((dock): dock is DockPosition => dock !== null)
})

async function loadBoundDocks () {
  try {
    const res = await getBindingDevices(workspaceId, { page: 1, page_size: 100, total: -1 }, EDeviceTypeName.Dock)
    if (res.code === 0) {
      boundDocks.value = res.data?.list ?? []
    }
  } catch (error) {
    console.error('加载绑定机巢失败:', error)
  }
}

function openAutoDialog () {
  autoAreaIds.value = []
  autoResult.value = null
  autoWarnings.value = []
  autoDialogVisible.value = true
}

function generateAutoTask () {
  autoWarnings.value = []
  if (!autoAreaIds.value.length) {
    ElMessage.warning('请选择巡检区域')
    return
  }
  const geometries = []
  let missingGeoCount = 0
  solarTable.value
    .filter((item: any) => autoAreaIds.value.includes(item.id))
    .forEach((item: any) => {
      const geometry = computeAreaGeometry(item)
      if (geometry) {
        geometries.push(geometry)
      } else {
        missingGeoCount += 1
      }
    })
  if (!geometries.length) {
    ElMessage.warning('所选区域缺少经纬度数据，无法生成')
    return
  }
  if (missingGeoCount > 0) {
    autoWarnings.value.push(`${missingGeoCount} 个区域缺少经纬度数据，未参与分配`)
  }
  if (!onlineDockPositions.value.length) {
    autoWarnings.value.push('当前没有在线机巢，无法自动分配')
    autoResult.value = null
    return
  }
  const result = recommendAutoTask(geometries, onlineDockPositions.value)
  if (!result) {
    ElMessage.warning('自动生成失败，请稍后重试')
    return
  }
  if (result.degraded) {
    autoWarnings.value.push(`在线机巢数量不足，推荐架数已降级为 ${result.droneCount}`)
  }
  autoResult.value = result
}

function applyAutoTask () {
  if (!autoResult.value) return
  const hasExisting = taskGroups.value.some(group => group.dock || group.solarIds.length > 0)
  if (hasExisting) {
    ElMessageBox.confirm('将覆盖当前已配置的任务分组，是否继续？', '自动生成任务', { type: 'warning' })
      .then(() => {
        applyAutoGroups()
      })
      .catch(() => {})
    return
  }
  applyAutoGroups()
}

function applyAutoGroups () {
  if (!autoResult.value) return
  taskGroups.value = autoResult.value.assignments.map(assignment => ({
    groupId: createSolarPlanId(),
    planId: createSolarPlanId(),
    dock: boundDocks.value.find((item: any) => item.device_sn === assignment.sn) ?? null,
    solarIds: [...assignment.areaIds],
    submitted: false
  }))
  syncAreaConfigs(selectedDetectAreas.value)
  autoDialogVisible.value = false
  ElMessage.success('已按推荐结果填充任务分组，可在下方微调')
}
// ===================== 自动生成任务结束 =====================

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
  workspace_id: localStorage.getItem(ELocalStorageKey.WorkspaceId)!,
  task_type: TaskType.Immediate,
  begin_time: '',
  end_time: '',
  status: 1,
  fan_id: '',
  poi_id: '',
  poi_orbit_num: 1,
  username: 'pilot',
  plan_type: '4',
  rth_altitude: '',
  out_of_control: 0, // 默认返回
  enable_status: 0,
  plan_priority: 1,
  type: 'auto',
  image_format_list: ['visable'], // 相机类型多选
  image_format: computed(() => planBody.image_format_list.join(',')) // 提交时转为逗号分隔字符串
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
  orthophoto_id: [{ required: true, message: '请选择正射图', trigger: 'change' }],
  name: [
    { required: true, message: '请输入计划名称', trigger: 'blur' },
    { min: 1, max: 50, message: '计划名称长度在1-50个字符', trigger: 'blur' }
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
  loadBoundDocks()
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

// 校验任务分组：每组必须有设备、有区域，且设备不重复
function validateTaskGroups () {
  if (!taskGroups.value.length) {
    ElMessage.warning('请至少保留一个任务分组')
    return false
  }
  const usedDeviceSns = new Set<string>()
  for (const group of taskGroups.value) {
    if (!group.dock) {
      ElMessage.warning('请为每个分组选择执行设备')
      return false
    }
    if (usedDeviceSns.has(group.dock.device_sn)) {
      ElMessage.warning(`执行设备重复：${group.dock.nickname}`)
      return false
    }
    usedDeviceSns.add(group.dock.device_sn)
    if (!group.solarIds.length) {
      ElMessage.warning(`请选择光伏板区域：${group.dock.nickname}`)
      return false
    }
  }
  return true
}

function validateGroupAreaConfigs (group: DeviceTaskGroup) {
  const configs = getGroupAreaConfigs(group)
  if (!configs.length) {
    ElMessage.warning(`请选择光伏板区域：${group.dock?.nickname ?? '未选择设备'}`)
    return false
  }

  for (const config of configs) {
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

function mapAreaConfig (config: AreaConfig) {
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
}

function getGroupAreaConfigs (group: DeviceTaskGroup): AreaConfig[] {
  return areaConfigs.value.filter(config => group.solarIds.includes(config.solar_panel_id))
}

function getGroupAreaConfigPayload (group: DeviceTaskGroup) {
  return getGroupAreaConfigs(group).map(mapAreaConfig)
}

// 按分组构造提交体：设备、区域、planId 均为该分组独立
function getGroupPlanBody (group: DeviceTaskGroup, index: number) {
  return {
    ...toRaw(planBody),
    name: `${planBody.name}-${group.dock.nickname}`,
    dock_sn: group.dock.device_sn,
    image_format: planBody.image_format,
    plan_id: group.planId,
    planId: group.planId,
    orthophoto_id: planBody.orthophoto_id,
    solar_panel_id: group.solarIds.join(','),
    area_configs: getGroupAreaConfigPayload(group),
    index
  }
}

// 第二步模板：某分组下的区域参数卡片
function groupAreaConfigs (group: DeviceTaskGroup) {
  return getGroupAreaConfigs(group)
}

// 分组区域下拉选项：排除其他分组已选区域（组间互斥），保留本组已选项
function groupAreaOptions (group: DeviceTaskGroup) {
  const otherGroupIds = taskGroups.value
    .filter(item => item.groupId !== group.groupId)
    .flatMap(item => item.solarIds)
  return filteredSolarTable.value.filter((item: any) => !otherGroupIds.includes(item.id))
}

function addTaskGroup () {
  taskGroups.value.push(createTaskGroup())
}

function removeTaskGroup (group: DeviceTaskGroup) {
  taskGroups.value = taskGroups.value.filter(item => item.groupId !== group.groupId)
  if (activeDeviceGroupId.value === group.groupId) {
    activeDeviceGroupId.value = ''
  }
}

// 光伏板区域选择变化（按分组维护，区域在分组间互斥）
function handleGroupAreasChange (group: DeviceTaskGroup, ids: string[]) {
  group.solarIds = ids || []
  syncAreaConfigs(selectedDetectAreas.value)
}

async function handleOrthophotoChange (id: string) {
  planBody.orthophoto_id = id
  taskGroups.value.forEach(group => {
    group.solarIds = []
  })
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
  activeDeviceGroupId.value = ''
}

function selectDevice (group: DeviceTaskGroup) {
  activeDeviceGroupId.value = group.groupId
  drawerVisible.value = true
  selectType.value = '2'
}

/**
 * @description: 下一步 - 校验第一步表单并切换到第二步
 */
async function handleNextStep () {
  try {
    // 只校验第一步的表单字段，任务分组单独校验
    const valid = await valueRef.value.validateField([
      'name', 'orthophoto_id', 'task_type', 'begin_time', 'rth_altitude', 'type'
    ])
    if (valid && validateTaskGroups()) {
      syncAreaConfigs(selectedDetectAreas.value)
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
 * 预览航线（按分组，只预览当前组的航线）
 */
async function previewWayline (group: DeviceTaskGroup) {
  try {
    if (!group.dock) {
      ElMessage.warning('请先选择执行设备')
      return
    }
    const valid = await valueRef.value.validateField(['image_format_list'])
    if (valid && validateGroupAreaConfigs(group)) {
      const res = await createFlyPlan(getGroupPlanBody(group, 0))
      if (res.code !== 0) {
        ElMessage.error('航线预览异常!')
        return
      }
      waylineInfo.value = res.data.wayline.route_2d.area.points
      const threeDPayload = res.data.wayline.three_d_payload
      if (threeDPayload) {
        threeDPayload.planId = group.planId
        threeDPayload.route_draft_id = group.planId
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
 * 创建航线：按分组顺序逐条创建，失败不中断，结束后汇总结果
 */
async function onSubmit () {
  try {
    const valid = await valueRef.value.validateField(['image_format_list'])
    if (!valid || !validateTaskGroups()) {
      return
    }
    const pendingGroups = taskGroups.value.filter(group => !group.submitted)
    for (const group of pendingGroups) {
      if (!validateGroupAreaConfigs(group)) {
        return
      }
    }
    const failedGroups: DeviceTaskGroup[] = []
    for (const group of pendingGroups) {
      try {
        const res = await createFlyPlan(getGroupPlanBody(group, 1))
        if (res.code === 0) {
          group.submitted = true
        } else {
          failedGroups.push(group)
        }
      } catch (error) {
        failedGroups.push(group)
      }
    }
    const successCount = pendingGroups.length - failedGroups.length
    if (!failedGroups.length) {
      ElMessage.success(`已创建 ${successCount} 条计划!`)
      // 返回飞行计划管理页面
      closePlan()
    } else {
      const failedNames = failedGroups.map(group => group.dock?.nickname ?? '未选择设备').join('、')
      if (successCount > 0) {
        ElMessage.warning(`成功创建 ${successCount} 条计划，失败：${failedNames}。可再次点击确认重试失败的分组`)
      } else {
        ElMessage.error(`创建失败：${failedNames}`)
      }
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

.task-groups {
  width: 100%;
}

.task-group-card {
  padding: 10px 12px;
  margin-bottom: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(206, 227, 255, 0.18);
  border-radius: 4px;
}

.task-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.task-group-title {
  font-weight: 600;
  color: #d8efff;
}

.task-group-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.task-group-device-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: rgba(65, 176, 255, 1);
  font-size: 13px;
  text-align: left;
}

.add-group-button {
  width: 100%;
  border-style: dashed;
}

.device-group-section {
  margin-bottom: 16px;
}

.device-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.device-group-name {
  font-weight: 600;
  color: #55e4ff;
}

.auto-task-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.auto-task-button {
  background-color: rgba(7, 75, 208, 1);
  border: 1px solid rgba(0, 64, 147, 1);
  color: #fff;
}

.auto-task-tip {
  color: #8fcfff;
  font-size: 12px;
}

.auto-task-body {
  color: #fff;
}

.auto-result-summary {
  margin-top: 14px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(206, 227, 255, 0.18);
  border-radius: 4px;
}

.auto-result-item {
  line-height: 26px;
  font-size: 13px;

  span {
    color: #a0aec0;
  }

  strong {
    color: #55e4ff;
  }
}

.auto-result-card {
  padding: 10px 12px;
  margin-top: 10px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(206, 227, 255, 0.18);
  border-radius: 4px;
}

.auto-result-dock {
  font-weight: 600;
  color: #d8efff;
  margin-bottom: 4px;
}

.auto-result-areas {
  font-size: 13px;
  line-height: 20px;
}

.auto-result-distance {
  margin-top: 4px;
  font-size: 12px;
  color: #8fcfff;
}

.auto-result-warning {
  margin-top: 8px;
  font-size: 12px;
  color: #ffd66e;
}

.auto-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
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
