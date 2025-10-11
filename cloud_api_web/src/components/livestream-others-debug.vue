<template>

  <div style="position: relative; width: 100%; height: 100%;">
    <video id="videoElement" autoplay controls :style="{ width: '100%', height: '100%' }"></video>
    <div style="position: absolute; left: 10px; top: 10px;">
      <el-select v-model="lensSelected" placeholder="镜头切换" size="large" class="select-operation" :teleported='false' @change="onSwitch">
        <el-option v-for="item in lensOption" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { message } from 'ant-design-vue'
import { onMounted, defineProps, reactive, ref, onUnmounted, watch, computed } from 'vue'
import { DeviceInfoType } from '/@/types/device'
import { CURRENT_CONFIG as config } from '/@/api/http/config'
import { changeLivestreamLens, getLiveCapacity, setLivestreamQuality, startLivestream, stopLivestream } from '/@/api/manage'
import { getRoot } from '/@/root'
import { useMyStore } from '/@/store'
import jswebrtc from '/@/vendors/jswebrtc.min.js'
import srs from '/@/vendors/srs.sdk.js'
import flvjs from 'flv.js'
const store = useMyStore()
const props = defineProps<{
  sn: string,
  deviceInfo: DeviceInfoType,
}>()
// flV视频流播放

const root = getRoot()

interface SelectOption {
  value: any,
  label: string,
  more?: any
}

const lensOption: SelectOption[] = [
  {
    value: 'normal',
    label: '普通'
  },
  {
    value: 'wide',
    label: '广角'
  },
  {
    value: 'zoom',
    label: '变焦'
  },
  {
    value: 'ir',
    label: '红外'
  }
]
// 需要传递参数
const osdVisible = computed(() => {
  return store.state.osdVisible
})
const device_sn = ref(osdVisible.value.sn)
const videowebrtc = ref(null)
const livestreamSource = ref()
const droneList = ref()
const cameraList = ref()
const videoList = ref()
const droneSelected = ref()
const cameraSelected = ref()
const videoSelected = ref()
const claritySelected = ref()
const videoId = ref()
const liveState = ref<boolean>(false)
const livetypeSelected = ref()
const rtspData = ref()
const lensList = ref<string[]>([])
const lensSelected = ref<String>()
const isDockLive = ref(false)
const nonSwitchable = 'normal'
const webrtc: any = null
const flvURL = ref()

const flvPlayer: any = ref() // flv 参数声明
const videoRef = ref<HTMLVideoElement | null>(null)

const isPlay = ref(false)
onMounted(() => {
  // onRefresh()
  videoRef.value?.addEventListener('play', () => {
    const end = flvPlayer.value.buffered.end(0) - 1
    flvPlayer.value.currentTime = end
  })

  window.onfocus = () => {
    const end = flvPlayer.value.buffered.end(0) - 1
    flvPlayer.value.currentTime = end
  }
  // getcameraInfo()
  // setTimeout(() => {
  //   initFlv()
  // }, 1000)
})

/* 请求后端相机信息
*
*/
const timestamp = new Date().getTime().toString()
const liveURL = config.rtmpURL + timestamp
livetypeSelected.value = 1
claritySelected.value = 2
async function getcameraInfo () {
  await getLiveCapacity({})
    .then(res => {
      if (res.code !== 0) {
        isPlay.value = false
        return
      }
      const cameraData = res.data.find(item => item.sn === device_sn.value)
      if (!cameraData) {
        isPlay.value = false
        return
      }
      droneSelected.value = cameraData.sn
      cameraSelected.value = cameraData.cameras_list[0].index
      videoId.value = droneSelected.value + '/' + cameraSelected.value + '/' + (videoSelected.value || nonSwitchable + '-0')
      console.log(res.data)
      getLiveHttp()
    })
}

/* 请求后端获取视频流地址
*
*/
async function getLiveHttp () {
  await startLivestream({
    url: liveURL,
    video_id: videoId.value,
    url_type: livetypeSelected.value,
    video_quality: claritySelected.value
  }).then(res => {
    // if (res.code !== 0) {
    //   return
    // }
    // flvURL.value = res.data.url.replace('webrtc://', 'http://').replace(':2035', ':9080') + '.flv'
    if (res.code === 0) {
      flvURL.value = res.data.url.replace('webrtc://', 'http://').replace(':2035', ':9080') + '.flv'
      initFlv()
    }
    if (res.code === 513003) {
      onStop()
      isPlay.value = false
      // setTimeout(() => {
      //   getLiveHttp()
      // }, 500)
      // setTimeout(() => {
      //   initFlv()
      // }, 1000)
    }
  })
}

/* 请求后端停止推流
*
*/
const onStop = () => {
  videoId.value =
    droneSelected.value + '/' + cameraSelected.value + '/' + (videoSelected.value || nonSwitchable + '-0')

  stopLivestream({
    video_id: videoId.value
  }).then(res => {
    if (res.code === 0) {
      // message.success(res.message)
      liveState.value = false
      lensSelected.value = undefined
      console.log('stop play livestream')
    }
  })
}
/**
 * 初始化
 */
function initFlv () {
  console.log('无人机视频', flvURL.value)
  videoRef.value = document.getElementById('videoElement') as HTMLVideoElement
  if (videoRef.value) {
    if (flvjs.isSupported()) {
      try {
        flvPlayer.value = flvjs.createPlayer({
          type: 'flv',
          // url: 'http://127.0.0.1:80/live?port=1935&app=live&stream=test',
          // url: 'http://172.20.63.56:8180/live/12345.flv',
          url: flvURL.value,
          isLive: true,
          hasAudio: false,
          hasVideo: true
        }, {
          enableWorker: false,
          enableStashBuffer: false,
          lazyLoad: false,
          lazyLoadMaxDuration: 0,
          lazyLoadRecoverDuration: 0,
          deferLoadAfterSourceOpen: false,
          fixAudioTimestampGap: true,
          autoCleanupSourceBuffer: true,
          // fit: 'fill'
        })
        flvPlayer.value.on(flvjs.Events.ERROR, (errorType, errorDetail, errorInfo) => {
          if (flvPlayer.value) {
            flvPlayer.value.pause()
            flvPlayer.value.unload()
            flvPlayer.value.detachMediaElement()
            flvPlayer.value.destroy()
            flvPlayer.value = null
            initFlv() // 重新调用 initFlv 函数重新创建播放器
          }
        })
        if (flvPlayer.value) {
          flvPlayer.value.attachMediaElement(videoRef.value)
          flvPlayer.value.load()
          if (videoRef.value.readyState >= 2) {
            flvPlayer.value.play()
          } else {
            videoRef.value.addEventListener('loadedmetadata', () => {
              flvPlayer.value.play()
            })
          }
        }
        isPlay.value = true
      } catch (error) {
        console.log('创建播放器实例时发生错误:', error)
        isPlay.value = false
      }
    } else {
      isPlay.value = false
      console.log('由于视频文件损坏或是该视频使用了你的浏览器不支持的功能')
    }
  }
}

/**
 * 播放
 */
function onStart () {
  flvPlayer.value.play()
}
/**
 * 暂停
 */
const onPause = () => flvPlayer.value.pause()
/**
 * 销毁
 */
const destory = () => {
  flvPlayer.value.pause()
  flvPlayer.value.unload()
  flvPlayer.value.detachMediaElement()
  flvPlayer.value.destroy()
  flvPlayer.value = null
}
onUnmounted(() => {
  // onStop()
  // destory()
})

// 根据设备osd信息更新信息
watch(() => props.deviceInfo, (value) => {
  // console.log('我被你检测11111')
  if (value.device && !isPlay.value) {
    console.log('执行请求视频')
    isPlay.value = true
    device_sn.value = osdVisible.value.sn
    getcameraInfo()
  }
}, {
  immediate: true,
  deep: true
})

const onSwitch = () => {
  if (lensSelected.value === undefined || lensSelected.value === nonSwitchable) {
    message.info('The ' + nonSwitchable + ' lens cannot be switched, please select the lens to be switched.', 8)
    return
  }
  changeLivestreamLens({
    video_id: videoId.value,
    video_type: lensSelected.value
  }).then(res => {
    if (res.code === 0) {
      // message.success('Switching live camera successfully.')
    }
  })
}

</script>

<style lang="scss" scoped>
@import '/@/styles/index.scss';

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
    width: 100px;
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
</style>
