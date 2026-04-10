import { CURRENT_CONFIG as config } from '/@/api/http/config'
import { changeLivestreamLens, getLiveCapacity, setLivestreamQuality, startLivestream, stopLivestream } from '/@/api/manage'
import { onMounted, reactive, ref, onUnmounted, defineProps, watch, computed } from 'vue'

// 机场默认
const droneSelected = ref()
const cameraSelected = ref('165-0-7')
const nonSwitchable = 'normal'
const videoId = ref('')
const livetypeSelected = 4
const claritySelected = 2

// 无人机
const droneCameraSelected = ref('99-0-0')

const timestamp = new Date().getTime().toString()
const liveURL = config.rtcURL

export const startStream = (device_sn, device_type) => {
  getcameraInfo(device_sn, device_type)
}

/** 获取设备信息 */
async function getcameraInfo (device_sn, device_type) {
  await getLiveCapacity({})
    .then(res => {
      const cameraData = res.data.find(item => item.sn === device_sn)
      if (!cameraData) {
        return
      }

      droneSelected.value = cameraData.sn

      if (device_type === 'drone') {
        cameraSelected.value = cameraData.cameras_list[0].index
      }
      videoId.value = droneSelected.value + '/' + cameraSelected.value + '/' + (nonSwitchable + '-0')

      getLiveHttp(device_type)
    })
}

/* 请求后端获取视频流地址
*
*/
async function getLiveHttp (device_type) {
  try {
    await startLivestream({
      url: liveURL,
      video_id: videoId.value,
      url_type: livetypeSelected,
      video_quality: claritySelected
    })
  } catch (error) {
  }
}
