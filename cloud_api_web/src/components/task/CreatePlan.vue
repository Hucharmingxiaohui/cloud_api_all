<template>
  <div class="main-box">
    <div class="box-left">
      <div class="box_title">
        <div class="thumbnail_1"></div>
        <div class="box_text">新建计划</div>
      </div>

      <div class="create-plan-wrapper">
        <div class="content">
          <a-form ref="valueRef" layout="horizontal"  :rules="rules" :model="planBody" labelAlign="left">
            <a-form-item label="任务名称" name="name" class="custom-label">
              <a-input style="background: #031846; width: 360px; color: #fff;margin-left: 7px"  placeholder="请输入任务名称" v-model:value="planBody.name"/>
            </a-form-item>
            <a-form-item label="优先级" name="priority" :labelCol="{span: 23}"  class="custom-label1">
              <el-radio-group v-model="planBody.plan_priority" size="large" >
                <el-radio-button style="width: 0" />
                <el-radio-button label="1级" value=1 class="radio-custom" />
                <el-radio-button label="2级" value=2 class="radio-custom"  />
                <el-radio-button label="3级" value=3 class="radio-custom"  />
                <el-radio-button label="4级" value=4 class="radio-custom" />
              </el-radio-group>
            </a-form-item>
            <a-form-item label="专业" name="major" :labelCol="{span: 23}" class="custom-label">
              <el-radio-group v-model="planBody.major" size="large">
                <el-radio-button style="width: 0" />
                <el-radio-button label="输电" value="输电" class="radio-custom" />
                <el-radio-button label="配电" value="配电" class="radio-custom" />
                <el-radio-button label="变电" value="变电"  class="radio-custom" />
              </el-radio-group>
            </a-form-item>
            <a-form-item label="场站/线路" name="sub" :labelCol="{span: 23}" class="custom-label1">
              <el-select v-model="planBody.sub_code" placeholder="变电站名称" class="select-operation" :teleported='false' style="margin-left: 7px">
                <el-option v-for="item in subOption" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </a-form-item>
            <!-- 航线 -->
            <a-form-item label="执行航线" :wrapperCol="{offset: 7}" name="file_id" class="custom-label">
              <!-- <span @click="selectDevice">选择设备<ttt></ttt></span> -->
              <router-link
                :to="{name: 'select-plan'}"
                @click="selectRoute"
                style="margin-left: 90px"
                class="block_2"
              >
              选择航线
              </router-link>
            </a-form-item>
            <a-form-item v-if="planBody.file_id" style="margin-top: -15px;" class="custom-label">
              <div class="wayline-panel" style="padding-top: 5px; background-color: #081B39;">
                <div class="title">
                  <a-tooltip :title="wayline.name">
                    <div class="wayline-name">{{ wayline.name }}</div>
                  </a-tooltip>
                  <!-- <div class="ml10"><UserOutlined /></div>
                  <a-tooltip :title="wayline.user_name">
                    <div class="ml5 pr10" style="width: 80px; white-space: nowrap; text-overflow: ellipsis; overflow: hidden;">{{ wayline.user_name }}</div>
                  </a-tooltip> -->
                </div>
                <div class="ml10 mt5" style="color: rgba(65, 176, 255, 1); font-weight: 500; font-size: 14px; text-align: left;">
                  <span><RocketOutlined /></span>
                  <span class="ml5">{{ DEVICE_NAME[wayline.drone_model_key] }}</span>
                  <span class="ml10"><CameraFilled style="border-top: 1px solid; padding-top: -3px;" /></span>
                  <span class="ml5" v-for="payload in wayline.payload_model_keys" :key="payload.id">
                    {{ DEVICE_NAME[payload] }}
                  </span>
                </div>
                <div class="mt5 ml10" style="color: rgba(65, 176, 255, 1); font-weight: 500; font-size: 14px;text-align: left;">
                  <span class="mr10">更新于{{ new Date(wayline.update_time).toLocaleString() }}</span>
                </div>
              </div>
            </a-form-item>
            <!-- 设备 -->
            <a-form-item label="执行设备" :wrapperCol="{offset: 10}" v-model:value="planBody.dock_sn" name="dock_sn" class="custom-label">
              <router-link
                :to="{name: 'select-plan'}"
                @click="selectDevice"
                style="margin-left: 40px"
                class="block_2"
              >选择设备</router-link>
            </a-form-item>
            <a-form-item v-if="planBody.dock_sn" style="margin-top: -15px;" class="custom-label">
              <div class="panel" style="padding-top: 5px; background-color: #081B39;">
                <div class="title">
                  <a-tooltip :title="dock.nickname">
                    <div class="wayline-name">{{ dock.nickname }}</div>
                  </a-tooltip>
                </div>
                <div class="ml10 mt5" style="color: rgba(65, 176, 255, 1); font-weight: 500; font-size: 14px;text-align: left;">
                  <span><RocketOutlined /></span>
                  <span class="ml5">{{ dock.children?.nickname ?? 'No drone' }}</span>
                </div>
              </div>
            </a-form-item>
            <!-- 任务类型 -->
            <a-form-item label="时间方案" :labelCol="{span: 23}" class="custom-label1" >
                <el-radio-group v-model="planBody.task_type" size="large">
                  <el-radio-button style="width: 0" />
                  <el-radio-button v-for="type in TaskTypeOptions" :value="type.value" :key="type.value" class="radio-custom1" >{{ type.label }}</el-radio-button>
                </el-radio-group>
            </a-form-item>
            <!-- execute date -->
            <a-form-item label="执行时间" v-if="planBody.task_type === TaskType.Timed || planBody.task_type === TaskType.Condition" name="select_execute_date" :labelCol="{span: 23}" class="custom-label">
              <a-range-picker
                v-model:value="planBody.select_execute_date"
                :disabledDate="(current: Moment) => current < moment().subtract(1, 'days')"
                format="YYYY-MM-DD"
                :placeholder="['开始时间', '结束时间']"
                style="width: 100%;"
              />
            </a-form-item>
            <!-- execute time -->
            <a-form-item label="时间" v-if="planBody.task_type === TaskType.Timed || planBody.task_type === TaskType.Condition"
              name="select_execute_time" ref="select_execute_time" :labelCol="{span: 23}" :autoLink="false">
              <div class="mb10 flex-row flex-align-center flex-justify-around" v-for="n in planBody.select_time_number" :key="n">
                <a-time-picker
                  v-model:value="planBody.select_time[n - 1][0]"
                  format="HH:mm:ss"
                  show-time
                  placeholder="开始时间"
                  :style="planBody.task_type === TaskType.Condition ? 'width: 40%' : 'width: 82%'"
                  @change="() => $refs.select_execute_time.onFieldChange()"
                />
                <template v-if="planBody.task_type === TaskType.Condition">
                  <div><span style="color: white;">-</span></div>
                  <a-time-picker
                    v-model:value="planBody.select_time[n - 1][1]"
                    format="HH:mm:ss"
                    show-time
                    placeholder="结束时间"
                    style="width: 40%;"
                  />
                </template>
                <div class="ml5" style="font-size:18px">
                  <PlusCircleOutlined class="mr5" style="color: #1890ff" @click="addTime"/>
                  <MinusCircleOutlined :style="planBody.select_time_number === 1 ? 'color: gray' : 'color: red;'" @click="removeTime"/>
                </div>
              </div>
            </a-form-item>
            <template v-if="planBody.task_type === TaskType.Condition">
              <!-- battery capacity -->
              <a-form-item label="电池电量达到时启动任务" :labelCol="{span: 23}" name="min_battery_capacity">
                <a-input-number class="width-100" v-model:value="planBody.min_battery_capacity" :min="50" :max="100"
                :formatter="(value: number) => `${value}%`" :parser="(value: string) => value.replace('%', '')">
                </a-input-number>
              </a-form-item>
              <!-- storage capacity -->
              <a-form-item label="当存储级别达到（MB）时启动任务" :labelCol="{span: 23}" name="storage_capacity">
                <a-input-number v-model:value="planBody.min_storage_capacity" class="width-100">
                </a-input-number>
              </a-form-item>
            </template>
            <!-- RTH Altitude Relative to Dock -->
            <a-form-item label="返航高度(m)" :labelCol="{span: 23}" name="rth_altitude" class="custom-label1">
              <a-input-number v-model:value="planBody.rth_altitude" :min="20" :max="1500" style="width: 360px; margin-left: 7px;" class="width-100" required>
              </a-input-number>
            </a-form-item>
            <!-- Lost Action -->
            <a-form-item label="失联动作" :labelCol="{span: 23}" name="out_of_control_action" class="custom-label1">
              <div style="white-space: nowrap;">
                <el-radio-group v-model="planBody.out_of_control_action" size="large">
                  <el-radio-button style="width: 0" />
                  <el-radio-button v-for="action in OutOfControlActionOptions" :value="action.value" :key="action.value" class="radio-custom1" >{{ action.label }}</el-radio-button>
                </el-radio-group>
              </div>
            </a-form-item>
            <a-form-item class="width-100" style="margin-bottom: 40px;">
              <div class="footer">
                <a-button class="mr10" style="background-color: rgba(255, 255, 255, 0.05); border: 1px solid rgba(206, 227, 255, 0.42); width: 175px;" @click="closePlan">取消
                </a-button>
                <a-button type="primary" style=" width: 175px; background-color: rgba(7, 75, 208, 1);border: 1px solid rgba(0, 64, 147, 1)" @click="onSubmit" :disabled="disabled">确认
                </a-button>
              </div>
            </a-form-item>
          </a-form>
        </div>
      </div>
    </div>
    <div class="box-right">
       <waylinePanel/>
    </div>

  </div>

  <div v-if="drawerVisible" style="position: absolute; left: 460px; width: 280px; height: 605px; float: right; top: 145px; z-index: 1000; color: white; background: #282828;">
    <div>
      <router-view :name="routeName"/>
    </div>
    <div style="position: absolute; top: 15px; right: 10px;">
      <a style="color: white;" @click="closePanel"><CloseOutlined /></a>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, onUnmounted, reactive, ref, toRaw, UnwrapRef, nextTick } from 'vue'
import { CloseOutlined, RocketOutlined, CameraFilled, UserOutlined, PlusCircleOutlined, MinusCircleOutlined } from '@ant-design/icons-vue'
import { ELocalStorageKey, ERouterName } from '/@/types'
import { useMyStore } from '/@/store'
import { WaylineType, WaylineFile } from '/@/types/wayline'
import { Device, DEVICE_NAME } from '/@/types/device'
import { createPlan, CreatePlan, createFlyPlan } from '/@/api/wayline'
import { getRoot } from '/@/root'
import { TaskType, OutOfControlActionOptions, OutOfControlAction, TaskTypeOptions } from '/@/types/task'
import moment, { Moment } from 'moment'
import { RuleObject } from 'ant-design-vue/es/form/interface'
import waylinePanel from '/@/components/g-map/showLineAtPlan.vue'
import { useRouter } from 'vue-router'
import { getAllSub } from '/@/api/points'
const router = useRouter()
const root = getRoot()
const store = useMyStore()

const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!

const wayline = computed<WaylineFile>(() => {
  return store.state.waylineInfo
})

const dock = computed<Device>(() => {
  return store.state.dockInfo
})

const disabled = ref(false)

const routeName = ref('')
const planBody = reactive({
  name: '',
  file_id: computed(() => store.state?.waylineInfo.id),
  dock_sn: computed(() => store.state?.dockInfo.device_sn), // 此处写一个假的设备sn
  plan_priority: 1,
  plan_source: '系统创建',
  major: '变电',
  sub_code: '',
  workspace_id: localStorage.getItem(ELocalStorageKey.WorkspaceId)!,
  begin_time: '',
  username: 'pilot',
  status: 1,
  enable_status: 0,
  // dock_sn: computed(() => store.state?.dockInfo.device_sn),
  task_type: TaskType.Immediate,
  select_execute_date: [moment(), moment()] as Moment[],
  select_time_number: 1,
  select_time: [[]] as Moment[][],
  rth_altitude: '',
  out_of_control_action: OutOfControlAction.ReturnToHome,
  min_battery_capacity: 90 as number,
  min_storage_capacity: undefined as number | undefined,
})

const drawerVisible = ref(false)
const valueRef = ref()
const rules = {
  name: [
    { required: true, message: '请输入任务名称' },
    { max: 20, message: '长度应在1到20之间' }
  ],
  plan_priority: [{ required: true }],
  major: [{ required: true }],
  file_id: [{ required: true, message: '请选择航线' }],
  dock_sn: [{ required: true, message: '请选择设备' }],
  select_execute_time: [{
    validator: async (rule: RuleObject, value: Moment[]) => {
      validEndTime()
      validStartTime()
      if (planBody.select_time.length < planBody.select_time_number) {
        throw new Error('请选择时间')
      }
      validOverlapped()
    }
  }],
  select_execute_date: [{ required: true, message: '请选择数据' }],
  rth_altitude: [
    {
      validator: async (rule: RuleObject, value: string) => {
      // 验证值是否为数字格式
        if (!/^[0-9]{1,}$/.test(value)) {
          throw new Error('RTH高度要求数字格式')
        }

        // 将字符串值转换为数字                                                      根据后端返回信息，新增验证
        const numericValue = Number(value)

        // 验证数字值是否在20到500之间
        if (numericValue < 20 || numericValue > 500) {
          throw new Error('RTH高度要求在20到500之间')
        }
      },
    }
  ],
  min_battery_capacity: [
    {
      validator: async (rule: RuleObject, value: any) => {
        if (TaskType.Condition === planBody.task_type && !value) {
          throw new Error('请输入电池容量')
        }
      },
    }
  ],
  out_of_control_action: [{ required: true, message: '请选择失联动作' }],
}

onMounted(() => {
  getSubInfo()
})

function validStartTime (): Error | void {
  for (let i = 0; i < planBody.select_time.length; i++) {
    if (!planBody.select_time[i][0]) {
      throw new Error('请选择开始时间')
    }
  }
}
function validEndTime (): Error | void {
  if (TaskType.Condition !== planBody.task_type) return
  for (let i = 0; i < planBody.select_time.length; i++) {
    if (!planBody.select_time[i][1]) {
      throw new Error('请选择着陆时间')
    }
    if (planBody.select_time[i][0] && planBody.select_time[i][1].isSameOrBefore(planBody.select_time[i][0])) {
      throw new Error('结束时间应晚于开始时间')
    }
  }
}
function validOverlapped (): Error | void {
  if (TaskType.Condition !== planBody.task_type) return
  const arr = planBody.select_time.slice()
  arr.sort((a, b) => a[0].unix() - b[0].unix())
  arr.forEach((v, i, arr) => {
    if (i > 0 && v[0] < arr[i - 1][1]) {
      throw new Error('时间段冲突.')
    }
  })
}

function onSubmit () {
  valueRef.value.validate().then(() => {
    disabled.value = true

    const createPlanBody = { ...planBody } as unknown as CreatePlan
    createPlanBody.task_days = []
    if (planBody.select_execute_date.length === 2) {
      createPlanBody.task_days = []
      for (let i = planBody.select_execute_date[0]; i.isSameOrBefore(planBody.select_execute_date[1]); i.add(1, 'days')) {
        createPlanBody.task_days.push(i.unix())
      }
    }
    createPlanBody.task_periods = []
    if (TaskType.Immediate !== planBody.task_type) {
      for (let i = 0; i < planBody.select_time.length; i++) {
        const result = []
        result.push(planBody.select_time[i][0].unix())
        if (TaskType.Condition === planBody.task_type) {
          result.push(planBody.select_time[i][1].unix())
        }
        createPlanBody.task_periods.push(result)
      }
    }
    createPlanBody.rth_altitude = Number(createPlanBody.rth_altitude)
    if (wayline.value && wayline.value.template_types && wayline.value.template_types.length > 0) {
      createPlanBody.wayline_type = wayline.value.template_types[0]
    }
    // console.log('任务执行信息', createPlanBody)
    const obj = {
      sub_code: createPlanBody.sub_code,
      major: createPlanBody.major,
      plan_source: createPlanBody.plan_source,
      name: createPlanBody.name,
      file_id: createPlanBody.file_id,
      dock_sn: createPlanBody.dock_sn,
      workspace_id: createPlanBody.workspace_id,
      task_type: createPlanBody.task_type,
      wayline_type: createPlanBody.wayline_type,
      begin_time: createPlanBody.task_days.length > 0 ? createPlanBody.task_days[0] : createPlanBody.begin_time,
      end_time: createPlanBody.task_days.length > 0 ? createPlanBody.task_days[createPlanBody.task_days.length - 1] : createPlanBody.end_time,
      // execute_time: createPlanBody.task_periods,
      status: createPlanBody.status,
      username: createPlanBody.username,
      rth_altitude: createPlanBody.rth_altitude,
      out_of_control: 1,
      enable_status: createPlanBody.enable_status,
      plan_priority: createPlanBody.plan_priority
    }
    console.log('新建计划', obj)
    // createPlan(workspaceId, createPlanBody)
    //   .then(res => {
    //     disabled.value = false
    //   }).finally(() => {
    //     closePlan()
    //   })
    createFlyPlan(obj)
      .then(res => {
        disabled.value = false
      }).finally(() => {
        closePlan()
      })
  }).catch((e: any) => {
    console.log('validate err', e)
  })
}

// function  createFlyPlan () {
//   nextTick(()=>{
//     // createFlyPlan().then(res => {
//     //   if (res.code !== 0) {
//     //     return
//     //   }
//     //   waylineInfo.value = res.data
//     // })
//   })

// }

/**
 * @description: 查询所有的场站
 * @param {string} waylineInfo 航线信息
 * */
const subOption = ref([])
function getSubInfo () {
  getAllSub().then(res => {
    if (res.code !== 0) {
      return
    }
    subOption.value = res.data.map(item => ({
      value: item.sub_code,
      label: item.sub_name
    }))
  })
}

function closePlan () {
  // root.$router.push('/' + ERouterName.TASK)
  router.push({ path: '/fly-plan' })
}

function closePanel () {
  drawerVisible.value = false
  routeName.value = ''
}

function selectRoute () {
  drawerVisible.value = true
  routeName.value = 'WaylinePanel'
}

function selectDevice () {
  drawerVisible.value = true
  routeName.value = 'DockPanel'
}

function addTime () {
  valueRef.value.validateFields(['select_execute_time']).then(() => {
    planBody.select_time_number++
    planBody.select_time.push([])
  })
}
function removeTime () {
  if (planBody.select_time_number === 1) return
  planBody.select_time_number--
  planBody.select_time.splice(planBody.select_time_number)
}
</script>

<style lang="scss" scoped>
.main-box {
  display: flex;
  /* 使用 flexbox 布局 */
  height: 100vh;
  /* 设置容器高度为视口高度 */
}
.box-left {
  background: rgba(59, 116, 255, 0.15);
  // -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  // box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
  width: 440px;
  /* 左侧占据 20% 的宽度 */
  // background-color: #4CAF50; /* 绿色背景 */
  padding: 20px;
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
      width: 24px;
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
      font-size: 24px;
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
}
.create-plan-wrapper {
  background-color: #09214B;
  color: #fff;
  padding-bottom: 0;
  /* height: fit-content; */
  display: flex;
  flex-direction: column;
  /* width: 100%; */
  width: 400px;
  overflow-y: auto;
  height: calc(100vh - 210px);
  // border: 1px solid #c5c8cc;

  .header {
    height: 52px;
    border-bottom: 1px solid #4f4f4f;
    font-weight: 700;
    font-size: 16px;
    padding-left: 10px;
    display: flex;
    align-items: center;
  }

  ::-webkit-scrollbar {
    display: none;
  }

  .content {
    height: calc(100% - 100px);
    // overflow-y: auto;

    form {
      margin: 10px;
    }

    :deep(form label, input, .ant-input, .ant-calendar-range-picker-separator,
    .ant-input:hover, .ant-time-picker .anticon, .ant-calendar-picker .anticon) {
      background-color: #031846;
      color: #fff;
    }

    .ant-input-suffix {
      color: #fff;
    }

    .plan-timer-form-item {

      .ant-radio-button-wrapper{
        background-color: #031846;
        color: #fff;
        width: 33%;
        text-align: center;
        &.ant-radio-button-wrapper-checked{
          background-color: #1890ff;
        }
      }
    }
  }

  .footer {
    display: flex;
    padding:10px 0;

    button {
      width: 45%;
      color: #fff ;
      border: 0;
    }
  }
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
.block_2 {
  box-shadow: 0px 0px 4px 0px rgba(6, 151, 255, 0.5);
  background-color: rgba(0, 87, 218, 0.11);
  border-radius: 2px;
  width: 100px;
  height: 24px;
  border: 0.800000011920929px solid rgba(0, 120, 218, 0.78);
  margin-left: 212px;
  padding: 3px;
}
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
  ::v-deep(.el-radio-button__inner) {
    box-shadow: inset 0px 0px 15px 1px rgba(154, 206, 255, 0.5) ;
    background-color: rgba(24, 118, 224, 0.1) ;
    // border-radius: 4px;
    height: 36px;
    margin-left: 7px;
    width: 85px;
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
    width: 85px;
    border: none !important;
    // border-radius: 4px;
  }
  ::v-deep(.el-radio-button:first-child .el-radio-button__inner){
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
    width: 85px;
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
    width: 85px;
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

:deep(.custom-label .ant-form-item-required) {
  background-image: linear-gradient(180deg,
      rgba(255, 255, 255, 1) 0,
      rgba(192, 228, 255, 1) 100%);

  // color: rgb(255, 0, 0);
  font-size: 18px;
  font-family: YouSheBiaoTiHei-Regular;
  font-weight: normal;
  text-align: left;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
::v-deep .custom-label1 .ant-form-item-label>label {
  background-image: linear-gradient(180deg,
      rgba(255, 255, 255, 1) 0,
      rgba(192, 228, 255, 1) 100%);
  // color: rgb(255, 0, 0);
  font-size: 18px;
  font-family: YouSheBiaoTiHei-Regular;
  font-weight: normal;
  text-align: left;
  white-space: nowrap;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

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
