<template>
  <!-- <div class="flex-column flex-justify-start flex-align-center">
    <div id="player" style="width: 720px; height: 420px; border: 1px solid"></div>
    <p class="fz24">直播源选择</p>
    <div class="flex-row flex-justify-center flex-align-center mt10">
      <template v-if="livePara.liveState && dronePara.isDockLive">
        <span class="mr10">镜头:</span>
        <a-radio-group v-model:value="dronePara.lensSelected" button-style="solid">
          <a-radio-button v-for="lens in dronePara.lensList" :key="lens" :value="lens">{{ lens }}</a-radio-button>
        </a-radio-group>
      </template>
      <template v-else>
        <a-select style="width:150px" placeholder="Select Drone" v-model:value="dronePara.droneSelected">
          <a-select-option v-for="item in dronePara.droneList" :key="item.value" :value="item.value"
            @click="onDroneSelect(item)">{{ item.label }}</a-select-option>
        </a-select>
        <a-select class="ml10" style="width:150px" placeholder="Select Camera" v-model:value="dronePara.cameraSelected">
          <a-select-option v-for="item in dronePara.cameraList" :key="item.value" :value="item.value"
            @click="onCameraSelect(item)">{{ item.label }}</a-select-option>
        </a-select>
      </template>
      <a-select class="ml10" style="width:150px" placeholder="Select Clarity" @select="onClaritySelect">
        <a-select-option v-for="item in clarityList" :key="item.value" :value="item.value">{{ item.label
          }}</a-select-option>
      </a-select>
    </div>
    <p class="fz16 mt10">
      注意：请从 https://console.agora.io 获取以下参数
    </p>
    <div class="flex-row flex-justify-center flex-align-center">
      <span class="mr10">AppId:</span>
      <a-input v-model:value="agoraPara.appid" placeholder="APP ID"></a-input>
      <span class="ml10">Token:</span>
      <a-input class="ml10" v-model:value="agoraPara.token" placeholder="Token"></a-input>
      <span class="ml10">Channel:</span>
      <a-input class="ml10" v-model:value="agoraPara.channel" placeholder="Channel"></a-input>
    </div>
    <div class="mt20 flex-row flex-justify-center flex-align-center">
      <a-button v-if="livePara.liveState && dronePara.isDockLive" type="primary" large @click="onSwitch">转换镜头</a-button>
      <a-button v-else type="primary" large @click="onStart">播放</a-button>
      <a-button class="ml20" type="primary" large @click="onStop">停止</a-button>
      <a-button class="ml20" type="primary" large @click="onUpdateQuality">更新清晰度</a-button>
      <a-button v-if="!livePara.liveState || !dronePara.isDockLive" class="ml20" type="primary" large
        @click="onRefresh">刷新实时容量</a-button>
    </div>
  </div> -->
  <div style="margin: 20px 0; padding: 0 15px;" >
    <video id="videoElement" autoplay controls :style="{ width: '100%', height: '400px' }"></video>
  </div>
</template>

<script lang="ts" setup>
import AgoraRTC, { IAgoraRTCClient, IAgoraRTCRemoteUser } from 'agora-rtc-sdk-ng'
import { message } from 'ant-design-vue'
import { onMounted, reactive, ref, onUnmounted } from 'vue'
import { uuidv4 } from '../utils/uuid'
import { CURRENT_CONFIG as config } from '/@/api/http/config'
import { changeLivestreamLens, getLiveCapacity, setLivestreamQuality, startLivestream, stopLivestream } from '/@/api/manage'
import { getRoot } from '/@/root'
// flV视频流播放
import flvjs from 'flv.js'

const root = getRoot()

const clarityList = [
  {
    value: 0,
    label: 'Adaptive'
  },
  {
    value: 1,
    label: 'Smooth'
  },
  {
    value: 2,
    label: 'Standard'
  },
  {
    value: 3,
    label: 'HD'
  },
  {
    value: 4,
    label: 'Super Clear'
  }
]

interface SelectOption {
  value: any,
  label: string,
  more?: any
}

const agoraClient = {} as IAgoraRTCClient
const agoraPara = reactive({
  appid: config.agoraAPPID,
  token: config.agoraToken,
  channel: config.agoraChannel,
  uid: 123456,
  stream: {}
})
const dronePara = reactive({
  livestreamSource: [],
  droneList: [] as SelectOption[],
  cameraList: [] as SelectOption[],
  videoList: [] as SelectOption[],
  droneSelected: undefined as string | undefined,
  cameraSelected: undefined as string | undefined,
  videoSelected: undefined as string | undefined,
  claritySelected: 0,
  lensList: [] as string[],
  lensSelected: undefined as string | undefined,
  isDockLive: false
})
const livePara = reactive({
  url: '',
  webrtc: {} as any,
  videoId: '',
  liveState: false
})
const nonSwitchable = 'normal'
const onRefresh = async () => {
  dronePara.droneList = []
  dronePara.cameraList = []
  dronePara.videoList = []
  dronePara.droneSelected = undefined
  dronePara.cameraSelected = undefined
  dronePara.videoSelected = undefined
  await getLiveCapacity({})
    .then(res => {
      if (res.code === 0) {
        if (res.data === null) {
          console.warn('warning: get live capacity is null!!!')
          return
        }
        dronePara.livestreamSource = res.data
        dronePara.droneList = []

        console.log('live_capacity:', dronePara.livestreamSource)

        if (dronePara.livestreamSource) {
          dronePara.livestreamSource.forEach((ele: any) => {
            dronePara.droneList.push({ label: ele.name + '-' + ele.sn, value: ele.sn, more: ele.cameras_list })
          })
        }
      }
    })
    .catch(error => {
      message.error(error)
      console.error(error)
    })
}
// =====================视频代码===============================================
const flvPlayer: any = ref() // flv 参数声明
const videoRef = ref<HTMLVideoElement | null>(null)
onMounted(() => {
  // onRefresh()
  initFlv()
})
/**
 * 初始化
 */
function initFlv () {
  videoRef.value = document.getElementById('videoElement') as HTMLVideoElement
  if (videoRef.value) {
    if (flvjs.isSupported()) {
      try {
        flvPlayer.value = flvjs.createPlayer({
          type: 'flv',
          url: config.flvURL,
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
})
// ======================================================================================================
// onMounted(() => {
//   onRefresh()
//   agoraClient = AgoraRTC.createClient({ mode: 'live', codec: 'vp8' })
//   agoraClient.setClientRole('audience', { level: 2 })
//   if (agoraClient.connectionState === 'DISCONNECTED') {
//     agoraClient.join(agoraPara.appid, agoraPara.channel, agoraPara.token)
//   }
//   // Subscribe when a remote user publishes a stream
//   agoraClient.on('user-joined', async (user: IAgoraRTCRemoteUser) => {
//     message.info('user[' + user.uid + '] join')
//   })
//   agoraClient.on('user-published', async (user: IAgoraRTCRemoteUser, mediaType: 'audio' | 'video') => {
//     await agoraClient.subscribe(user, mediaType)
//     if (mediaType === 'video') {
//       console.log('subscribe success')
//       // Get `RemoteVideoTrack` in the `user` object.
//       const remoteVideoTrack = user.videoTrack!
//       // Dynamically create a container in the form of a DIV element for playing the remote video track.
//       remoteVideoTrack.play(document.getElementById('player') as HTMLElement)
//     }
//   })
//   agoraClient.on('user-unpublished', async (user: any) => {
//     console.log('unpublish live:', user)
//     message.info('unpublish live')
//   })
//   agoraClient.on('exception', async (e: any) => {
//     console.log(e)
//     message.error(e.msg)
//   })
// })
const handleError = (err: any) => {
  console.error(err)
}
const handleJoinChannel = (uid: any) => {
  agoraPara.uid = uid
}
// const onStart = async () => {
//   const that = this
//   console.log(
//     'drone parameter：',
//     dronePara.droneSelected,
//     dronePara.cameraSelected,
//     dronePara.videoSelected,
//     dronePara.claritySelected
//   )
//   const timestamp = new Date().getTime().toString()
//   const liveTimestamp = timestamp
//   if (
//     dronePara.droneSelected == null ||
//     dronePara.cameraSelected == null ||
//     dronePara.claritySelected == null
//   ) {
//     message.warn('waring: not select live para!!!')
//     return
//   }
//   agoraClient.setClientRole('audience', { level: 2 })
//   if (agoraClient.connectionState === 'DISCONNECTED') {
//     await agoraClient.join(agoraPara.appid, agoraPara.channel, agoraPara.token)
//   }
//   livePara.videoId =
//     dronePara.droneSelected +
//     '/' +
//     dronePara.cameraSelected + '/' + (dronePara.videoSelected || nonSwitchable + '-0')
//   console.log(agoraPara)

//   livePara.url =
//     'channel=' +
//     agoraPara.channel +
//     '&sn=' +
//     dronePara.droneSelected +
//     '&token=' +
//     encodeURIComponent(agoraPara.token) +
//     '&uid=' +
//     agoraPara.uid

//   startLivestream({
//     url: livePara.url,
//     video_id: livePara.videoId,
//     url_type: 0,
//     video_quality: dronePara.claritySelected
//   })
//     .then(res => {
//       if (res.code !== 0) {
//         return
//       }
//       livePara.liveState = true
//     })
//     .catch(err => {
//       console.error(err)
//     })
// }
const onStop = async () => {
  if (
    dronePara.droneSelected == null ||
    dronePara.cameraSelected == null ||
    dronePara.claritySelected == null
  ) {
    message.warn('waring: not select live para!!!')
    return
  }
  livePara.videoId =
    dronePara.droneSelected +
    '/' +
    dronePara.cameraSelected + '/' + (dronePara.videoSelected || nonSwitchable + '-0')

  stopLivestream({
    video_id: livePara.videoId
  }).then(res => {
    if (res.code === 0) {
      message.success(res.message)
      livePara.liveState = false
      dronePara.lensSelected = ''
      console.log('stop play livestream')
    }
  })
}
const onDroneSelect = (val: SelectOption) => {
  dronePara.cameraList = []
  dronePara.videoList = []
  dronePara.lensList = []

  dronePara.cameraSelected = undefined
  dronePara.videoSelected = undefined
  dronePara.lensSelected = undefined
  dronePara.droneSelected = val.value
  if (!val.more) {
    return
  }
  val.more.forEach((ele: any) => {
    dronePara.cameraList.push({ label: ele.name, value: ele.index, more: ele.videos_list })
  })
}
const onCameraSelect = (val: SelectOption) => {
  dronePara.cameraSelected = val.value
  dronePara.videoSelected = undefined
  dronePara.lensSelected = undefined
  dronePara.videoList = []
  dronePara.lensList = []
  if (!val.more) {
    return
  }

  val.more.forEach((ele: any) => {
    dronePara.videoList.push({ label: ele.type, value: ele.index, more: ele.switch_video_types })
  })
  if (dronePara.videoList.length === 0) {
    return
  }
  const firstVideo: SelectOption = dronePara.videoList[0]
  dronePara.videoSelected = firstVideo.value
  dronePara.lensList = firstVideo.more
  dronePara.lensSelected = firstVideo.label
  dronePara.isDockLive = dronePara.lensList?.length > 0
}
const onVideoSelect = (val: SelectOption) => {
  dronePara.videoSelected = val.value
  dronePara.lensList = val.more
  dronePara.lensSelected = val.label
}
const onClaritySelect = (val: any) => {
  dronePara.claritySelected = val
}
const onUpdateQuality = () => {
  if (!livePara.liveState) {
    message.info('Please turn on the livestream first.')
    return
  }
  setLivestreamQuality({
    video_id: livePara.videoId,
    video_quality: dronePara.claritySelected
  }).then(res => {
    if (res.code === 0) {
      message.success('Set the clarity to ' + clarityList[dronePara.claritySelected].label)
    }
  })
}

const onSwitch = () => {
  if (dronePara.lensSelected === undefined || dronePara.lensSelected === nonSwitchable) {
    message.info('The ' + nonSwitchable + ' lens cannot be switched, please select the lens to be switched.', 8)
    return
  }
  changeLivestreamLens({
    video_id: livePara.videoId,
    video_type: dronePara.lensSelected
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
