<template>
  <a-layout class="width-100 flex-display" style="height: 100vh;">
    <a-layout-header class="header left" style="padding: 0">
      <Sidebar />
    </a-layout-header>
    <a-layout-content class="content-bg" >
      <router-view />
    </a-layout-content>

  </a-layout>
</template>
<script lang="ts" setup>
import { onMounted, ref } from 'vue'
// import video from './livestream.vue'
import Sidebar from '/@/components/common/sidebar.vue'
// import MediaPanel from '/@/components/MediaPanel.vue'
// import TaskPanel from '/@/components/task/TaskPanel.vue'
import { EBizCode, ERouterName } from '/@/types'
import { getRoot } from '/@/root'
import { useMyStore } from '/@/store'
import { useConnectWebSocket } from '/@/hooks/use-connect-websocket'
import EventBus from '/@/event-bus'

// @ts-ignore
import { startStream } from '/@/components/watchDevice/controlLivestream.js'
let dockOsdEmptyTimer = null
let dockOsdEmptyStartTime = 0

// 当前暂存的设备状态
const dock_sn = ref('')
const drone_online_status = ref(false)

const root = getRoot()
const store = useMyStore()
const messageHandler = async (payload: any) => {
  if (!payload) {
    return
  }
  switch (payload.biz_code) {
    case EBizCode.GatewayOsd: {
      store.commit('SET_GATEWAY_INFO', payload.data)
      break
    }
    case EBizCode.DeviceOsd: {
      store.commit('SET_DEVICE_INFO', payload.data)

      break
    }
    case EBizCode.DockOsd: {
      const currentData = payload.data
      const hasEnvironmentTemperature = currentData.host?.environment_temperature !== ''
      // console.log(currentData.host?.environment_temperature)

      if (hasEnvironmentTemperature) {
        // 不为空，立即提交
        clearTimeout(dockOsdEmptyTimer)
        dockOsdEmptyStartTime = 0
        store.commit('SET_DOCK_INFO', currentData)
        // store?.state.deviceState

        // 开始推流
        if (currentData.sn && currentData.sn !== dock_sn.value) {
          dock_sn.value = currentData.sn
          startStream(currentData.sn, 'dock')
          drone_online_status.value = currentData.host?.sub_device.device_online_status
          if (currentData.host?.sub_device.device_online_status === true && currentData.host?.sub_device.device_online_status !== drone_online_status.value) {
            startStream(currentData.host?.sub_device.device_sn, 'drone')
          }
        }
      } else {
        // 为空，检查是否已经持续2秒
        const now = Date.now()

        if (dockOsdEmptyStartTime === 0) {
          // 第一次检测到为空，开始计时
          dockOsdEmptyStartTime = now
        }

        clearTimeout(dockOsdEmptyTimer)
        dockOsdEmptyTimer = setTimeout(() => {
          if (Date.now() - dockOsdEmptyStartTime >= 2000) {
            store.commit('SET_DOCK_INFO', currentData)
          }
          dockOsdEmptyStartTime = 0 // 重置计时
        }, 2000)
      }
      break
    }
    case EBizCode.MapElementCreate: {
      store.commit('SET_MAP_ELEMENT_CREATE', payload.data)
      break
    }
    case EBizCode.MapElementUpdate: {
      store.commit('SET_MAP_ELEMENT_UPDATE', payload.data)
      break
    }
    case EBizCode.MapElementDelete: {
      store.commit('SET_MAP_ELEMENT_DELETE', payload.data)
      break
    }
    case EBizCode.DeviceOnline: {
      store.commit('SET_DEVICE_ONLINE', payload.data)
      break
    }
    case EBizCode.DeviceOffline: {
      store.commit('SET_DEVICE_OFFLINE', payload.data)
      break
    }
    case EBizCode.FlightTaskProgress:
    case EBizCode.FlightTaskMediaProgress:
    case EBizCode.FlightTaskMediaHighestPriority: {
      EventBus.emit('flightTaskWs', payload)
      break
    }
    case EBizCode.DeviceHms: {
      store.commit('SET_DEVICE_HMS_INFO', payload.data)
      break
    }
    case EBizCode.DeviceReboot:
    case EBizCode.DroneOpen:
    case EBizCode.DroneClose:
    case EBizCode.CoverOpen:
    case EBizCode.CoverClose:
    case EBizCode.PutterOpen:
    case EBizCode.PutterClose:
    case EBizCode.ChargeOpen:
    case EBizCode.ChargeClose:
    case EBizCode.DeviceFormat:
    case EBizCode.DroneFormat:
    {
      store.commit('SET_DEVICES_CMD_EXECUTE_INFO', {
        biz_code: payload.biz_code,
        timestamp: payload.timestamp,
        ...payload.data,
      })
      break
    }
    case EBizCode.ControlSourceChange:
    case EBizCode.FlyToPointProgress:
    case EBizCode.TakeoffToPointProgress:
    case EBizCode.JoystickInvalidNotify:
    case EBizCode.DrcStatusNotify:
    {
      EventBus.emit('droneControlWs', payload)
      break
    }
    case EBizCode.FlightAreasSyncProgress: {
      EventBus.emit('flightAreasSyncProgressWs', payload.data)
      break
    }
    case EBizCode.FlightAreasDroneLocation: {
      EventBus.emit('flightAreasDroneLocationWs', payload)
      break
    }
    case EBizCode.FlightAreasUpdate: {
      EventBus.emit('flightAreasUpdateWs', payload.data)
      break
    }
    default:
      break
  }
}
useConnectWebSocket(messageHandler)
onMounted(() => {
  // 监听ws 消息

})
</script>
<style lang="scss" scoped>
.content-bg{

    // background: url('/@/assets/icons/bg.png')  no-repeat;
    // background-size: 100% 100%;
    // background-color: #05204B;
    // margin-top: 10px;
    height: calc(100vh - 100px);
}
.map-wrapper{
    width: 100%;
    height: 100%;
  }
// @import '/@/styles/index.scss';

// .project-app-wrapper {
//   display: flex;
//   transition: width 0.2s ease;
//   height: 100%;
//   width: 100%;

//   .left {
//     display: flex;
//     width: 335px;
//     flex: 0 0 335px;
//     background-color: #232323;

//     .main-content {
//       flex: 1;
//       color: $text-white-basic;
//       width: 200px;
//     }
//   }

@import '/@/styles/index.scss';

.fontBold {
  font-weight: 500;
  font-size: 18px;
}

.header {
  background-color: black;
  color: white;
  height: 100px;
  font-size: 15px;
  padding: 0 20px;
}
</style>
