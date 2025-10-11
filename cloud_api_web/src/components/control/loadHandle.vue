<template>
    <div  :class="['container-right', { collapsed: !isRightPanelVisible }]" class="scrollbar" :style="{ height: scorllHeight + 'px'}"  v-if="showUavPanel">
      <div class="layout-arrow cursor-pointer" @click="toggleRightPanel">
        <img :src="arrowIcon" />
      </div>
      <!--  -->
      <div class="part3" v-show="isRightPanelVisible">
        <div class="middleRight1">
          <img src="../../assets/v3/icon/tool-name.png" />
          <span class="tool-name">无人机信息</span>
        </div>
        <div class="middleUavRight3 uav-operation">
          <img
            src="../../assets/v3/icon/uav-operation-data.png"
            class="uav-operation-text"
          />
        </div>
        <div class="middleUavRight5 uav-operation">
          <img src="../../assets/v3/icon/uav-fly.png" class="uav-fly" />
          <el-progress
            type="dashboard"
            :percentage="data.percentage"
            :color="data.colors"
            :show-text="false"
            :width="110"
            style="
              position: absolute;
              top: 54%;
              left: 50%;
              transform: translate(-50%, -50%);
            "
          ></el-progress>
        </div>
        <div class="middleUavRight7">
          <span class="battery-life">续航:{{ batteryLife }}s</span>
          <span class="uav-electricity-level">电量</span>
          <span class="uav-electricity-show">{{ percentage }}%</span>
        </div>
        <div class="middleUavRight4 uav-operation">
          <span
            v-for="(item, index) in data.uavOperationData"
            :key="index"
            class="uav-operation-info"
          >
            {{ item.label }}:{{ item.value }}
          </span>
        </div>

        <!-- 远程遥控 -->
        <div class="middleUavRight3 uav-operation">
            <img
              src="../../assets/v3/icon/uav-operation-data.png"
              class="uav-operation-text"
            />
        </div>
        <div class="middleRight6">
          <el-tabs type="card" v-model="data.elTabCurrentName" class="demo-tabs">
            <el-tab-pane label="云台遥控" name="remoteControl">
              <!-- <div style="display: flex; justify-content: center;">
                <div style="display: flex; justify-items: center;">
                  <el-button @click="getControlAuth" class="btn">开启云台控制</el-button>
                  <el-button @click="releaseControlAuth" class="btn">释放云台控制</el-button>
                </div>
              </div> -->
              <div style="position: relative; left: 0px; top: 0px;">
                <controlPanel></controlPanel>
              </div>
              </el-tab-pane>
            <el-tab-pane label="无人机控制" name="joystickControl">
              <div style="position: relative; left: 0px; top: 0px;">
                <UAVControlPanel></UAVControlPanel>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

      </div>
    </div>
</template>
<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import controlPanel from './ControlDegree.vue'
import UAVControlPanel from './ControlUAV.vue'
import { requestControlAuto } from '/@/api/pilot-control/pilot-control'
const scorllHeight = ref() // 左侧容器滚动高度
const isRightPanelVisible = ref(false)
const showUavPanel = ref(false)
const data = {
  infraredImaging: false,
  isLoadShow: false, // 是否显示系统上方三个菜单的按钮

  videoSourceId: '',
  videoChannelId: '',
  videoSourceList: [], // 所有视频源
  videoChannelList: [], // 所有视频通道
  userOperationTurret: {}, // 存储选择的视频通道设备的信息

  subMenuDisabled: true,
  showTurretPanel: false,
  showUavPanel: false,
  isShowSwitchButton: false,

  playState: 'realPlay',
  winInx: 0,
  deviceName: '',
  picList: [],
  winNum_play: 1, // 窗口数量，默认为4窗口
  curWinNum_play: 1, // 当前的窗口数量
  curVideoRowCls_play: ['one-row'], // 窗口布局样式

  azimuthValue: 0,
  pitchValue: 0,
  pitchInputValue: 0,
  infraredValue: 0,
  towerValue: 0,

  isWakeUpTurret: false,
  isWakeUpInfrared: false,
  cameraCode: '',
  openSlider: true,

  unitNetworkList: [{ id: 1, name: '集群控制' }],
  unitNetworkId: null,
  // 是否由无人机面板切换至光电转塔界面
  isUavTOTurretPanel: false,

  PTZInformationData: {},
  degreeEnabled: true,

  elTabCurrentName: 'remoteControl',

  portUavOriginal: 8004,
  pc: null,
  ip: '172.20.63.108',
  videoStream: null,

  percentage: 10,
  batteryLife: '30',
  colors: [
    { color: '#f56c6c', percentage: 20 },
    { color: '#e6a23c', percentage: 40 },
    { color: '#6f7ad3', percentage: 60 },
    { color: '#1989fa', percentage: 80 },
    { color: '#5cb87a', percentage: 100 },
  ],
  baseColor: '#1989fa',
  uavOperationData: [
    { label: '高度', value: '' },
    { label: '水平速度', value: '' },
    { label: '经度', value: '' },
    { label: '纬度', value: '' },
  ],

}
onMounted(() => {
  scorllHeight.value = window.innerHeight - 60
})
const toggleRightPanel = () => {
  isRightPanelVisible.value = !isRightPanelVisible.value
}
// 模拟一些逻辑来更新这两个值
setTimeout(() => {
  showUavPanel.value = true
  //   showTurretPanel.value = true
  isRightPanelVisible.value = true
}, 500)
const arrowIcon = computed(() => {
  return isRightPanelVisible.value
    ? new URL('../../assets/v3/icon/right-arrow.png', import.meta.url).href
    : new URL('../../assets/v3/icon/left-arrow.png', import.meta.url).href
})

const gatewaySn = '5YSZL4V00325YL' // 遥控器编号
const deviceSn = '1581F5FHD23BT00DRZM6' // 飞行器编号
const mothod = 'cloud_control_auth_request' // 请求云端控制权限
const autoValue = {
  user_id: 'dongfangpilot',
  user_callsign: 'df1500',
  control_keys: [
    'flight'
  ]
}
// 请求控制权
function getControlAuth () {
  requestControlAuto(gatewaySn, deviceSn, mothod, autoValue).then(res => {
    if (res.code !== 0) {
      return
    }
    console.log(res.data)
  })
}
</script>
<style lang="scss" scoped>
.cursor-pointer {
  cursor: pointer;
}
.demo-tabs :deep(.el-tabs__item) {
  color: #ffffff;
  font-weight: 600;
}
.demo-tabs :deep(.el-tabs__header .el-tabs__item.is-active ){
  background-color: #0f327f; /* 选中标签背景颜色 */
  color: #fff; /* 选中标签文字颜色 */
  border-radius: 4px; /* 选中标签圆角 */
}
// .demo-tabs :deep(.el-tabs__header) {
//   border: none; /* 去掉底部边框 */
// }

// el-button按钮设置
.btn{
  border: 2px solid #1299C3;
  margin-top: 10px;
  background: linear-gradient(to top, #11B4FB, #023956);
  color:rgba(255, 255, 255, 0.762);
}
.scrollbar {
    overflow: auto;
}
.container-right {
    position: relative;
    width: 100%;
    height: 100%;
    right: 30px;
    overflow-y: auto;
    overflow-x: hidden;
    margin-left: 30px;
    //margin-right: 35px;
    background: #0a2257;
    // box-shadow: inset 0px 0px 14px 0px rgba(59, 123, 235, 0.69);
    // border-radius: 8px;
    background-image: url("../../assets/v3/icon/right-indent.png");
    background-size: 100% 100%;
    background-repeat: no-repeat;
    transition: width 0.3s ease;

    &.collapsed {
      width: 20px;
    }
    .layout-arrow {
      position: absolute;
      top: 48.5%;
      transform: translateY(-50%);
      z-index: 10;
    }

    .part3 {
      height: 100%;
      width: 100%;
      display: flex;
      flex-direction: column;
      .middleRight1 {
        position: relative;
        top: 20px;
        left: 30px;
        width: 90%;
        height: 29px;
        display: flex;
        justify-items: left;
        background: linear-gradient(
          270deg,
          rgba(27, 83, 181, 0) 0%,
          #2061d5 100%
        );
        border-radius: 100px 0px 0px 100px;
        img {
          width: 30px;
          height: 30px;
          filter: brightness(1.8);
        }
        .tool-name {
          position: relative;
          top: 2px;
          left: 10px;
          font-family: YouSheBiaoTiHei;
          font-size: 17px;
          color: #ffffff;
          line-height: 21px;
          text-align: left;
          font-style: normal;
          text-shadow: 0px 0px 4px #6ae4fd;
          letter-spacing: 1px;
        }
      }
      .middleRight2 {
        .middleRight21 {
          display: flex;
          margin-left: 40px;
          img {
            width: 16px;
            height: 16px;
            position: relative;
            top: 28px;
            left: 17px;
          }
          .azimuth-angle {
            font-family: PingFangSC, PingFang SC;
            font-weight: 500;
            font-size: 14px;
            color: #ffffff;
            line-height: 22px;
            text-align: left;
            font-style: normal;
            display: flex;
            position: relative;
            top: 24px;
            left: 28px;
            letter-spacing: 1px;
          }
          .azimuth-input {
            display: flex;
            position: relative;
            top: 20px;
            left: 155px;
          }
        }
      }
      .middleRight3 {
        .middleRight21 {
          display: flex;
          margin-left: 40px;
          img {
            width: 16px;
            height: 16px;
            position: relative;
            top: 8px;
            left: 17px;
          }
          .pitch-angle {
            font-family: PingFangSC, PingFang SC;
            font-weight: 500;
            font-size: 14px;
            color: #ffffff;
            line-height: 22px;
            text-align: left;
            font-style: normal;
            display: flex;
            position: relative;
            top: 6px;
            left: 28px;
            letter-spacing: 1px;
          }
          .pitch-input {
            display: flex;
            position: relative;
            //top: 1px;
            left: 155px;
          }
        }
        .pitch-bar {
          width: 73%;
          margin-left: 55px;
          display: flex;
          position: relative;
          bottom: 4px;
        }
      }
      .middleRight4 {
        .middleRight21 {
          display: flex;
          margin-left: 30px;
          img {
            width: 16px;
            height: 16px;
            position: relative;
            top: 4px;
            left: 17px;
          }
          .infrared {
            font-family: PingFangSC, PingFang SC;
            font-weight: 500;
            font-size: 14px;
            color: #ffffff;
            line-height: 22px;
            text-align: left;
            font-style: normal;
            display: flex;
            position: relative;
            left: 28px;
            letter-spacing: 1px;
          }
        }
      }
      .middleRight5 {
        .middleRight21 {
          display: flex;
          margin-left: 30px;
          img {
            width: 16px;
            height: 16px;
            position: relative;
            top: 6px;
            left: 17px;
          }
          .tower-zoom {
            font-family: PingFangSC, PingFang SC;
            font-weight: 500;
            font-size: 14px;
            color: #ffffff;
            line-height: 22px;
            text-align: left;
            font-style: normal;
            display: flex;
            position: relative;
            top: 2px;
            left: 28px;
            letter-spacing: 1px;
          }
        }
      }
      .middleRight6{
        margin: 30px;

      }

      .tool-bar {
        display: flex;
        position: relative;
        top: 10px;
        left: 48px;
        .tool-bar-img {
          width: 103px;
        }
        .tool-bar-space {
          margin-left: 16px;
        }
      }
      .radius-bar {
        position: relative;
        left: 60px;
        top: 15px;
        .button-container {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: space-between;
          gap: 10px;
        }
        .button-circle {
          background: rgba(59, 116, 255, 0.15);
          -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
          box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
          color: #ffffff;
          border: none;
          width: 30px;
          height: 30px;
          transition: 0.5s;
          border-radius: 50%;
          cursor: pointer;
        }

        .button-circle:hover {
          background: skyblue;
        }
        .radius-bar-img {
          width: 103px;
        }

        .circle {
          position: absolute;
          width: 25px;
          height: 25px;
          border: none;
          background: none;
          border-radius: 50%;
          display: flex;
          justify-content: center;
          align-items: center;
          cursor: pointer;
          z-index: 2;
        }

        .circle:hover {
          background: skyblue;
        }

        .circle i {
          font-size: 16px;
          color: #cbe7ff;
        }

        .up {
          top: 3px;
          left: 50%;
          transform: translateX(-50%);
        }

        .down {
          bottom: 3px;
          left: 51%;
          transform: translateX(-50%);
        }

        .left {
          top: 50%;
          left: 3px;
          transform: translateY(-50%);
        }

        .right {
          top: 50%;
          right: 3px;
          transform: translateY(-50%);
        }
      }
      .middleUavRight3 {
        display: flex;
        justify-content: center;
        align-items: center;
        margin-top: 30px;
      }
      .middleUavRight4 {
        width: 50%;
        display: grid;
        grid-template-columns: repeat(2, 1fr); /* 两列布局 */
        gap: 12px;
        position: relative;
        left: 25px;
        margin-top: -30px;
        .uav-operation-info {
          display: flex;
          align-items: center;
          background: rgba(59, 116, 255, 0.15);
          -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
          box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
          border: 1px solid #719fff;
          border-radius: 4px;
          width: 157px;
          height: 36px;
          padding-left: 5px;
          font-family: PingFangSC, PingFang SC;
          font-weight: 500;
          font-size: 14px;
          color: #ffffff;
          text-align: left;
          font-style: normal;
          letter-spacing: 1px;
        }
      }
      .middleUavRight41 {
        width: 50%;
        display: grid;
        grid-template-columns: repeat(2, 1fr); /* 两列布局 */
        gap: 12px;
        position: relative;
        left: 25px;
        margin-top: 20px;
        .uav-operation-info {
          display: flex;
          align-items: center;
          background: rgba(59, 116, 255, 0.15);
          -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
          box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
          border: 1px solid #719fff;
          border-radius: 4px;
          width: 157px;
          height: 36px;
          padding-left: 5px;
          font-family: PingFangSC, PingFang SC;
          font-weight: 500;
          font-size: 14px;
          color: #ffffff;
          text-align: left;
          font-style: normal;
          letter-spacing: 1px;
        }
      }
      .middleUavRight42 {
        display: flex;
        position: relative;
        top: 30px;
        left: 25px;
        ::v-deep .el-select .el-input__inner::placeholder,
        .el-select-dropdown .el-select-dropdown__item {
          font-family: PingFangSC, PingFang SC;
          font-weight: 500;
          font-size: 14px;
          color: #fffffe;
          text-align: left;
          font-style: normal;
          letter-spacing: 1px;
        }
        ::v-deep .el-select {
          background: rgba(59, 116, 255, 0.15);
          -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
          box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
          border: 1px solid #719fff;
          border-radius: 4px;
        }
      }
      .middleUavRight5 {
        display: flex;
        justify-content: center;
        align-items: center;
        position: relative;
      }
      .uav-operation {
        position: relative;
        top: 12px;
      }
      .uav-status {
        position: relative;
        top: 20px;
      }
      .uav-operation-text {
        width: 350px;
        margin: 0 auto;
        display: flex;
        position: relative;
        top: 2px;
      }
      .uav-fly {
        width: 50%;
        height: auto;
        position: relative;
        top: 6px;
      }
      .middleUavRight7 {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        position: relative;
        bottom: 60px;
        .battery-life {
          font-family: YouSheBiaoTiHei;
          font-size: 20px;
          color: #2aea81;
          font-style: normal;
          letter-spacing: 1px;
          position: relative;
          bottom: 104px;
        }
        .uav-electricity-level {
          font-family: YouSheBiaoTiHei;
          font-size: 17px;
          color: #ffffff;
          font-style: normal;
          letter-spacing: 1px;
        }
        .uav-electricity-show {
          font-family: YouSheBiaoTiHei;
          font-size: 20px;
          color: #2aea81;
          font-style: normal;
          letter-spacing: 1px;
        }
      }
    }
  }
</style>
