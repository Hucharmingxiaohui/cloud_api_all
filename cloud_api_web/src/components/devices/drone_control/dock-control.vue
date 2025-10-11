<template>
    <!-- title -->
    <!-- <div class="dock-control-panel-header fz16 pl5 pr5 flex-align-center flex-row flex-justify-between">
        <span>jichang<span class="fz12 pl15">{{ props.sn}}</span></span>
        <span @click="closeControlPanel">
        <CloseOutlined />
        </span>
    </div> -->
    <!-- setting -->
    <!-- <DeviceSettingBox :sn="props.sn" :deviceInfo="props.deviceInfo"></DeviceSettingBox> -->
    <!-- cmd -->
    <div class="content_title">
            <span style="margin-right: 10px;">设备调试:</span>
            <a-switch class="debug-btn" checked-children="开" un-checked-children="关" v-model:checked="debugStatus" @change="onDeviceStatusChange"/>
    </div>
    <div style="padding: 5px;">
        <div class="items">
            <div class="item" v-for="(cmdItem, index) in cmdList" :key="cmdItem.cmdKey">
                <div style="display: flex; flex-direction: column;">
                    <span style="font-size: 14px;">{{ cmdItem.label }}</span>
                    <span>{{ cmdItem.status }}</span>
                </div>
                <div class="control-cmd-item-right">
                    <a-button :disabled="!debugStatus || cmdItem.disabled" :loading="cmdItem.loading" size="small" type="primary" @click="sendControlCmd(cmdItem, index)">
                    {{ cmdItem.operateText }}
                    </a-button>
                </div>
            </div>
        </div>

    </div>
</template>
<script setup lang="ts">
import { defineProps, defineEmits, ref, watch } from 'vue'
import {
  CloseOutlined
} from '@ant-design/icons-vue'
import { useDockControl } from '/@/components/g-map/use-dock-control'
import { DeviceInfoType, EDockModeCode } from '/@/types/device'
import { cmdList as baseCmdList, DeviceCmdItem } from '/@/types/device-cmd'
import { useMyStore } from '/@/store'
import { updateDeviceCmdInfoByOsd, updateDeviceCmdInfoByExecuteInfo } from '/@/utils/device-cmd'
const props = defineProps<{
  sn: string,
  deviceInfo: DeviceInfoType,
}>()

const store = useMyStore()
const initCmdList = baseCmdList.map(cmdItem => Object.assign({}, cmdItem))
const cmdList = ref(initCmdList)

// 根据机场指令执行状态更新信息
watch(() => store.state.devicesCmdExecuteInfo, (devicesCmdExecuteInfo) => {
  if (props.sn && devicesCmdExecuteInfo[props.sn]) {
    updateDeviceCmdInfoByExecuteInfo(cmdList.value, devicesCmdExecuteInfo[props.sn])
  }
}, {
  immediate: true,
  deep: true,
})

// 根据设备osd信息更新信息
watch(() => props.deviceInfo, (value) => {
  updateDeviceCmdInfoByOsd(cmdList.value, value)
  // console.log('deviceInfo', value)
}, {
  immediate: true,
  deep: true
})

// dock 控制指令
const debugStatus = ref(props.deviceInfo.dock?.basic_osd?.mode_code === EDockModeCode.Remote_Debugging)
const emit = defineEmits(['close-control-panel'])

function closeControlPanel () {
  emit('close-control-panel', props.sn, debugStatus.value)
}

async function onDeviceStatusChange (status: boolean) {
  let result = false
  if (status) {
    result = await dockDebugOnOff(props.sn, true)
  } else {
    result = await dockDebugOnOff(props.sn, false)
  }
  if (!result) {
    if (status) {
      debugStatus.value = false
    } else {
      debugStatus.value = true
    }
  }
}

const {
  sendDockControlCmd,
  dockDebugOnOff
} = useDockControl()

async function sendControlCmd (cmdItem: DeviceCmdItem, index: number) {
  const success = await sendDockControlCmd({
    sn: props.sn,
    cmd: cmdItem.cmdKey,
    action: cmdItem.action
  }, true)
  if (success) {
    // updateDeviceSingleCmdInfo(cmdList.value[index])
  }
}
</script>
<style  lang="scss" scoped>
.content_title{
  background:rgba(59, 116, 255, 0.15);
  width: 100%;
  height: 35px;
  margin: 5px 0;
  padding-left: 10px;
  font-size: 14px;
  // font-weight: bold;
  line-height: 35px;
  color: #ffffff;
}
.debug-btn{
  margin-left: 15px;
}
.setup-box{
  padding: 5px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-family: PingFangSC;
  font-weight: 500;
  font-size: 14px;
  color: #ffffff;
  font-style: normal;
}
.items {
    display: grid;
    grid-template-columns: repeat(2, 1fr); /* 修改为两列布局 */
    gap: 10px;
    padding: 5px;
}

.item {
    width: 100%;
    text-align: left;
    overflow: hidden;
    display: flex;
    padding: 10px;
    justify-content: space-between;
    align-items: center;
    color: #ffffff;
    display: flex;
    justify-items: space;
    background-color: rgba(0, 0, 0, 0.15);
}

.item span:nth-child(2) {
    margin-left: 10px;
}

/*  按钮 */
.btn{
    background: rgba(59, 116, 255, 0.15);
    -webkit-box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    box-shadow: inset 0px 0px 15px 1px rgba(34, 135, 255, 0.5);
    border: 1px solid #719fff;
    border-radius: 4px;
    width: 100px;
    height: 30px;
    font-family: PingFangSC, PingFang SC;
    font-weight: 500;
    font-size: 14px;
    color: #ffffff;
    &:hover{
      background: skyblue;
    }

}

</style>
