<template>
    <video id="videoElement" autoplay controls :style="{ width: '100%', height: '100%' }"></video>
</template>

<script lang="ts" setup>
import { message } from 'ant-design-vue'
import { onMounted, reactive, ref, onUnmounted, defineProps } from 'vue'
import { CURRENT_CONFIG as config } from '/@/api/http/config'
import { changeLivestreamLens, getLiveCapacity, setLivestreamQuality, startLivestream, stopLivestream } from '/@/api/manage'
import { getRoot } from '/@/root'
import jswebrtc from '/@/vendors/jswebrtc.min.js'
import srs from '/@/vendors/srs.sdk.js'
// flV视频流播放
import flvjs from 'flv.js'
const props = defineProps<{
  sn: string
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

const device_sn = ref(props.sn)
cameraSelected.value = '165-0-7'
onMounted(() => {
  // refresh()
  // videoRef.value?.addEventListener('play', () => {
  //   const end = flvPlayer.value.buffered.end(0) - 1
  //   flvPlayer.value.currentTime = end
  // })

  // window.onfocus = () => {
  //   const end = flvPlayer.value.buffered.end(0) - 1
  //   flvPlayer.value.currentTime = end
  // }
  getcameraInfo()
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
      console.log('wdsdsdaaa', res.data)
      const cameraData = res.data.find(item => item.sn === device_sn.value)
      droneSelected.value = cameraData.sn
      // cameraSelected.value = cameraData.cameras_list[0].index
      videoId.value = droneSelected.value + '/' + cameraSelected.value + '/' + (videoSelected.value || nonSwitchable + '-0')
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
    if (res.code === 0) {
      flvURL.value = res.data.url.replace('webrtc://', 'http://').replace(':2035', ':9080') + '.flv'
      initFlv()
    }
    if (res.code === 513003) {
      onStop()
      setTimeout(() => {
        getLiveHttp()
      }, 500)
      setTimeout(() => {
        initFlv()
      }, 1000)
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
  console.log('机场视频', flvURL.value)
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
      } catch (error) {
        console.log('创建播放器实例时发生错误:', error)
      }
    } else {
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
  flvPlayer.value.pause()
  flvPlayer.value.unload()
  flvPlayer.value.detachMediaElement()
  flvPlayer.value.destroy()
  flvPlayer.value = null
}
onUnmounted(() => {
  destory()
  onStop()
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
</style>
