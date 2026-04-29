<template>
  <div class="main-box">
    <div class="box-left">
      <div class="box_title">
        <div class="thumbnail_1"></div>
        <div class="box_text">新建计划</div>
      </div>

      <div class="create-plan-wrapper">
        <div class="content">
          <el-form
            :model="planBody"
            label-width="110px"
            ref="valueRef"
            :rules="rules"
            label-position="left"
            style="padding: 10px;"
          >
            <el-form-item label="计划名称" required prop="name">
              <el-input v-model="planBody.name" maxlength="50"></el-input>
            </el-form-item>
            <el-form-item
              label="光伏板区域"
              required
              prop="solar_panel_id"
            >
              <el-select v-model="planBody.solar_panel_id" @change="handleSolarPanelChange">
                <el-option
                  v-for="item in solarTable"
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
                <span><RocketOutlined /></span>
                <span
                  class="ml5"
                  >{{ DEVICE_NAME[wayline.drone_model_key] }}</span
                >
                <span class="ml10"
                  ><CameraFilled
                    style="border-top: 1px solid; padding-top: -3px;"
                /></span>
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

            <el-form-item label="执行设备" required prop="dock_sn">
              <div
                @click="selectDevice"
                style="margin-left: 120px"
                class="block_2"
              >
                选择设备
              </div>
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
                <span><RocketOutlined /></span>
                <span
                  class="ml5"
                  >{{ dock.children?.nickname ?? 'No drone' }}</span
                >
              </div>
            </div>
            <el-form-item
              label="时间方案"
              required
              prop="task_type"
              label-position="top"
            >
              <el-radio-group v-model="planBody.task_type" size="large">
                <el-radio-button style="width: 0" />
                <el-radio-button
                  v-for="type in TaskTypeOptions"
                  :value="type.value"
                  :key="type.value"
                  class="radio-custom"
                  >{{ type.label }}</el-radio-button
                >
              </el-radio-group>
            </el-form-item>
            <el-form-item
              label="开始时间"
              required
              prop="begin_time"
              v-if="planBody.task_type === TaskType.Timed || planBody.task_type === TaskType.Condition"
            >
              <el-date-picker
                v-model="planBody.begin_time"
                type="datetime"
                placeholder="选择日期和时间"
                value-format="x"
                format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
            <el-form-item label="返航高度(m)" prop="rth_altitude" required>
              <el-input v-model="planBody.rth_altitude"></el-input>
            </el-form-item>
            <el-form-item style="margin-bottom: 40px;">
              <div class="footer">
                <el-button
                  class="mr10"
                  style="background-color: rgba(255, 255, 255, 0.05); width: 100px; border: 1px solid rgba(206, 227, 255, 0.42);"
                  @click="closePlan"
                  >取消
                </el-button>
                <el-button
                  type="primary"
                  style="background-color: rgba(7, 75, 208, 1); width: 100px; border: 1px solid rgba(0, 64, 147, 1)"
                  @click="onSubmit"
                  >确认
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
    <div class="box-right">
      <loadSolarPanel :imagePath="selectedImagePath" :detectAreas="selectedDetectAreas" />
    </div>
  </div>

  <div
    v-if="drawerVisible"
    style="position: absolute; left: 460px; width: 280px; height: 605px; float: right; top: 145px; z-index: 1000; color: white; background: #282828;"
  >
    <div >
      <SelectDock />
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
import { ElTable, ElTableColumn, ElFormItem, ElForm, ElInput, ElButton, ElSelect, ElOption, ElPagination, ElContainer, ElHeader, ElMain, ElFooter, ElDialog, ElMessage, ElMessageBox, ElText, ElLink, ElTag, ElTooltip } from 'element-plus'
import loadSolarPanel from '/@/components/task/solarflyPlan/loadSolarPanel.vue'
import SelectDock from '/@/pages/page-web/projects/dock.vue'
import { useRouter, useRoute } from 'vue-router'
import { getAllWindTurbineApi, getAllInserestPointApi, getAllSolarPanelApi, getSolarPanelImgByIdApi, getOrthophotoListApi, getSolarPanelByIdApi } from '/@/api/turbine/turbineMgt'
const router = useRouter()
const route = useRoute()
const store = useMyStore()
const selectType = ref('')

const workspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)!

const wayline = computed<WaylineFile>(() => {
  return store.state.waylineInfo
})

const dock = computed<Device>(() => {
  return store.state.dockInfo
})
const solarTable = ref([]) // 光伏板区域列表
const selectedImagePath = ref('') // 选中光伏板区域对应的正射图path
const selectedDetectAreas = ref<any>(null) // 选中光伏板区域的检测区域
const planBody = reactive({
  plan_source: '系统创建',
  name: '',
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
  plan_priority: 1
})

const drawerVisible = ref(false)
const valueRef = ref()
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
  ]
}

onMounted(async () => {
  getSolarPanel()
})

/**
 * 创建航线
 */
async function onSubmit () {
  try {
    const valid = await valueRef.value.validate()
    if (valid) {
      // 1.创建飞行计划
      const res = await createFlyPlan(planBody)
      if (res.code !== 0) {
        ElMessage.warning('请填写必填项!')
        return
      }
      ElMessage.success('创建成功!')
      // 2. 执行飞行计划 单次定时计划，立即执行飞行任务
      // if (planBody.task_type === TaskType.Timed) {

      // }
      // 返回飞行计划管理页面
      closePlan()
    }
  } catch (error) {

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
      id: ''
    }).then(res => {
      if (res.code !== 0) {
        return
      }
      solarTable.value = res.data.list
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

// 光伏板区域选择变化
async function handleSolarPanelChange (id: string | number) {
  selectedImagePath.value = ''
  selectedDetectAreas.value = null

  if (!id) return
  console.log(solarTable.value)
  const orthophotoItem = solarTable.value.find((item) => item.id === id
  )
  try {
    // selectedDetectAreas.value = orthophotoItem
    // selectedImagePath.value = 'D:\\orthophoto\\测试.jpg'
    if (orthophotoItem.orthophoto_id) {
      const path = await getOrthophotoPath(orthophotoItem.orthophoto_id)
      selectedDetectAreas.value = orthophotoItem
      selectedImagePath.value = path
    } else {
      ElMessage.warning('为查询到关联正射图!')
    }
  } catch (error) {
    console.error('获取光伏板区域详情失败:', error)
  }
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
</script>

<style lang="scss" scoped>
.main-box {
  display: flex;
  /* 使用 flexbox 布局 */
  height: 100vh;
  /* 设置容器高度为视口高度 */
}

//表单样式
:deep(.el-form-item__label){
    background-image: linear-gradient(180deg,
      rgba(255, 255, 255, 1) 0,
      rgba(192, 228, 255, 1) 100%);
  font-size: 21px;
  font-family: YouSheBiaoTiHei-Regular;
  font-weight: normal;
  text-align: left;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
:deep(.el-input__wrapper){
  background-color: #041b75;;
}
:deep(.el-input__inner){
  color: white;
}
:deep(.el-select__placeholder){
  color: white;
}
:deep(.el-form-item){
  margin: 30px auto;
}
:deep(.el-select__wrapper){
  background-color: #041b75;
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
  color: #1890ff;
  line-height: 24px;
  text-align: center;
  border: 0.800000011920929px solid rgba(0, 120, 218, 0.78);
  margin-left: 232px;
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
