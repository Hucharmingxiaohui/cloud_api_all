<template>
    <video id="videoElement1" autoplay controls :style="{ width: '100%', height: '100%' }"></video>
</template>

<script lang="ts" setup>
import { message } from 'ant-design-vue'
import { onMounted, reactive, nextTick, ref, onUnmounted, defineProps, watch, computed } from 'vue'
import { CURRENT_CONFIG as config } from '/@/api/http/config'
import { getImageUrl } from '/@/common/url'
import { DeviceInfoType } from '/@/types/device'
import { changeLivestreamLens, getLiveCapacity, setLivestreamQuality, startLivestream, stopLivestream } from '/@/api/manage'
import { getRoot } from '/@/root'
import { useMyStore } from '/@/store'
import jswebrtc from '/@/vendors/jswebrtc.min.js'
import srs from '/@/vendors/srs.sdk.js'
// flV视频流播放
import flvjs from 'flv.js'
import { resolveLiveVideoSource } from '/@/components/livestream/use-live-video-source'
const store = useMyStore()
const props = defineProps<{
  sn: string,
  deviceInfo: DeviceInfoType,
}>()
const root = getRoot()

interface SelectOption {
  value: any,
  label: string,
  more?: any
}

// 需要传递参数

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
let retryTimer: number | undefined
let startingStreamKey = ''
let activeStreamKey = ''

const osdVisible = computed(() => {
  return store.state.osdVisible || { gateway_sn: '' }
})
const device_sn = ref(props.sn)
cameraSelected.value = '165-0-7'
const isPlay = ref(false)
const isStartSteam = ref(false)
onMounted(() => {
  getcameraInfo()
})

watch(() => props.sn, sn => {
  if (!sn || sn === device_sn.value) return
  destroyFlv()
  startingStreamKey = ''
  activeStreamKey = ''
  device_sn.value = sn
  isStartSteam.value = false
  isPlay.value = false
  getcameraInfo()
})

/* 请求后端相机信息
*
*/
const timestamp = new Date().getTime().toString()
const liveURL = config.rtcURL
livetypeSelected.value = 4
claritySelected.value = 2
async function getcameraInfo () {
  const source = await resolveLiveVideoSource({
    role: 'dock',
    sn: props.sn,
    deviceInfo: props.deviceInfo,
    flvBaseUrl: config.flvURL
  })
  if (!source) return
  device_sn.value = source.deviceSn
  droneSelected.value = source.deviceSn
  cameraSelected.value = source.cameraIndex
  videoId.value = source.videoId
  flvURL.value = source.flvUrl
  console.log('[dock-live] direct flv url', flvURL.value)
  getLiveHttp(getStreamKey())
}

function getStreamKey () {
  if (!device_sn.value || !cameraSelected.value || !videoId.value) return ''
  return `${device_sn.value}/${cameraSelected.value}/${videoId.value}`
}

/* 请求后端获取视频流地址
*
*/
async function getLiveHttp (streamKey = getStreamKey()) {
  try {
    if (!videoId.value || !device_sn.value) return
    if (streamKey && (streamKey === startingStreamKey || (streamKey === activeStreamKey && flvPlayer.value))) return
    startingStreamKey = streamKey
    console.log('[dock-live] start', { sn: device_sn.value, videoId: videoId.value })
    const res = await startLivestream({
      url: liveURL,
      video_id: videoId.value,
      url_type: livetypeSelected.value,
      video_quality: claritySelected.value
    })
    if (res.code === 0) {
      isStartSteam.value = true
      const whepUrl = res.data.url
      const urlObj = new URL(whepUrl)
      const streamName = urlObj.searchParams.get('stream') // "8UUXN3U00A046E-165-0-7"
      const flvFileName = streamName + '.flv'
      flvURL.value = getImageUrl(config.flvURL, flvFileName)
      console.log('[dock-live] flv url', flvURL.value)
      nextTick(() => {
        initFlv(streamKey)
      })
      return
    }
    if (res.code === 513003 || res.code === 13003) {
      isStartSteam.value = true
      flvURL.value = getImageUrl(config.flvURL, device_sn.value + '-' + cameraSelected.value + '.flv')
      console.log('[dock-live] fallback flv url', flvURL.value)
      nextTick(() => {
        initFlv(streamKey)
      })
      return
    }
    isStartSteam.value = false
    isPlay.value = false
    activeStreamKey = ''
  } catch (error) {
    isStartSteam.value = false
    isPlay.value = false
    activeStreamKey = ''
  } finally {
    if (startingStreamKey === streamKey) startingStreamKey = ''
  }
}

function destroyFlv () {
  if (retryTimer) {
    window.clearTimeout(retryTimer)
    retryTimer = undefined
  }
  if (!flvPlayer.value) return
  flvPlayer.value.pause()
  flvPlayer.value.unload()
  flvPlayer.value.detachMediaElement()
  flvPlayer.value.destroy()
  flvPlayer.value = null
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
function initFlv (streamKey = getStreamKey()) {
  videoRef.value = document.getElementById('videoElement1') as HTMLVideoElement
  if (!videoRef.value || !flvURL.value) {
    retryTimer = window.setTimeout(() => initFlv(streamKey), 1000)
    return
  }
  if (videoRef.value) {
    if (flvjs.isSupported()) {
      try {
        destroyFlv()
        flvPlayer.value = flvjs.createPlayer({
          type: 'flv',
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
          console.warn('[dock-live] flv error', errorType, errorDetail, errorInfo)
          if (flvPlayer.value) {
            destroyFlv()
            isPlay.value = false
            activeStreamKey = ''
            retryTimer = window.setTimeout(() => initFlv(streamKey), 1500)
          }
        })
        if (flvPlayer.value) {
          flvPlayer.value.attachMediaElement(videoRef.value)
          flvPlayer.value.load()
          if (videoRef.value.readyState >= 2) {
            flvPlayer.value.play().catch(() => {})
          } else {
            videoRef.value.addEventListener('loadedmetadata', () => {
              flvPlayer.value?.play().catch(() => {})
            }, { once: true })
          }
        }
        isPlay.value = true
        activeStreamKey = streamKey
      } catch (error) {
        isPlay.value = false
        activeStreamKey = ''
        isStartSteam.value = false
        console.log('创建播放器实例时发生错误:', error)
      }
    } else {
      isPlay.value = false
      activeStreamKey = ''
      isStartSteam.value = false
      console.log('由于视频文件损坏或是该视频使用了你的浏览器不支持的功能')
    }
  }
}

/* 刷新视频
*
*/
function refresh () {
  getcameraInfo()
  onStop()
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
  if (retryTimer) window.clearTimeout(retryTimer)
  destroyFlv()
  startingStreamKey = ''
  activeStreamKey = ''
}
onUnmounted(() => {
  destory()
  // onStop()
})

// 根据设备osd信息更新信息
watch(() => props.deviceInfo, (value) => {
  if (!isPlay.value) {
    isPlay.value = true
    device_sn.value = props.sn
    if (!isStartSteam.value) {
      getcameraInfo()
    }
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
      message.success('Switching live camera successfully.')
    }
  })
}

</script>

<style lang="scss" scoped>
@import '/@/styles/index.scss';
video::-webkit-media-controls-enclosure{
        display: none;
    }
</style>
