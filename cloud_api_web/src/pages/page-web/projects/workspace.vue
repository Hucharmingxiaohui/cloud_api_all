<template>
  <!-- <div class="project-app-wrapper">
    <div class="left">
      <Sidebar />
      <div class="main-content uranus-scrollbar dark" >
        <router-view />
      </div>
    </div>
    <div class="right">
      <div class="map-wrapper">
        <GMap />
      </div>
      <div class="media-wrapper" v-if="root.$route.name === ERouterName.MEDIA">
        <MediaPanel />
      </div>
      <div class="task-wrapper" v-if="root.$route.name === ERouterName.TASK">
        <TaskPanel />
      </div>

      <div class="Organization-wrapper" v-if="$route.name === ERouterName.Organization">
        <Organization />
      </div>
    </div>
  </div> -->
  <a-layout class="width-100 flex-display" style="height: 100vh;">
    <a-layout-header class="header left" style="padding: 0">
      <Sidebar />
    </a-layout-header>
    <a-layout-content class="content-bg" >
      <router-view />
      <!-- <div class="map-wrapper" v-if="root.$route.name === ERouterName.LAYER">
        <router-view />
      </div>
      <div class="Organization-wrapper" v-else-if="root.$route.name === ERouterName.Organization">
        <Organization />
      </div>
      <div class="media-wrapper" v-else-if="root.$route.name === ERouterName.MEDIA">
        <MediaPanel />
      </div>
      <div class="task-wrapper" v-else-if="root.$route.name === ERouterName.TASK">
        <TaskPanel />
      </div>
      <div class="task-wrapper" v-else-if="root.$route.name === ERouterName.NEW_WAYLINE">
        <myWayline />
      </div>
      <div class="task-wrapper" v-else>
        <router-view />
      </div> -->
    </a-layout-content>

  </a-layout>
</template>
<script lang="ts" setup>
import { onMounted } from 'vue'
// import video from './livestream.vue'
import Sidebar from '/@/components/common/sidebar.vue'
// import MediaPanel from '/@/components/MediaPanel.vue'
// import TaskPanel from '/@/components/task/TaskPanel.vue'
import { EBizCode, ERouterName } from '/@/types'
import { getRoot } from '/@/root'
import { useMyStore } from '/@/store'
import { useConnectWebSocket } from '/@/hooks/use-connect-websocket'
import EventBus from '/@/event-bus'
// import Organization from '/@/components/Organization.vue'
// import myTsa from '/@/components/TsaPanel.vue'
// import myWayline from '/@/components/WaylinePanel.vue'
// import GMap from '/@/components/GMap.vue'
// import myWayline from './wayline.vue'

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
      store.commit('SET_DOCK_INFO', payload.data)
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

// 监听ws 消息
useConnectWebSocket(messageHandler)

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
