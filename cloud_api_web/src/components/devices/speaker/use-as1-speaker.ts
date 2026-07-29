import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import EventBus from '/@/event-bus'
import { DRC_METHOD } from '/@/types/drc'
import { DeviceTopicInfo, DrcMqttPublisher } from '/@/components/g-map/use-mqtt'
import {
  As1State,
  buildSpeakerCommand,
  clampSpeakerSpeed,
  clampSpeakerValue,
  createSpeakerFileName,
  findAs1State,
  getSpeakerErrorMessage,
  getSpeakerStatusText,
  md5Utf8,
  SPEAKER_STEP_TEXT,
  SPEAKER_PROGRESS_STATUS_TEXT,
  SpeakerDrcMessage,
  SpeakerPlayMode,
  SpeakerProgressData,
  SpeakerSettings,
  SpeakerSystemState,
  SpeakerTtsLanguage,
  SpeakerTtsType,
} from './as1-speaker'

const COMMAND_TIMEOUT_MS = 8000

export function useAs1Speaker (
  deviceTopicInfo: DeviceTopicInfo,
  mqttHooks: DrcMqttPublisher
) {
  const device = ref<As1State | null>(null)
  const settings = reactive<SpeakerSettings>({
    volume: 50,
    type: SpeakerTtsType.FEMALE,
    language: SpeakerTtsLanguage.CHINESE,
    speed: 50,
    playMode: SpeakerPlayMode.SINGLE,
  })
  const text = ref('')
  const busy = ref(false)
  const progress = ref(0)
  const statusText = ref('等待 AS1 状态')
  const errorText = ref('')
  let seq = 0
  const pending = new Map<number, {
    method: string
    resolve:(payload: SpeakerDrcMessage<any>) => void
    reject: (error: Error) => void
    timer: ReturnType<typeof setTimeout>
  }>()

  const drcReady = computed(() => Boolean(deviceTopicInfo.pubTopic && deviceTopicInfo.subTopic))
  const available = computed(() => drcReady.value && Boolean(device.value))
  const systemState = computed(() => device.value?.speaker?.system_state)
  const canStop = computed(() => busy.value || [
    SpeakerSystemState.TRANSFERRING,
    SpeakerSystemState.PLAYING,
    SpeakerSystemState.TTS_PROCESSING,
    SpeakerSystemState.DOWNLOADING,
  ].includes(systemState.value as SpeakerSystemState))

  function nextSeq () {
    seq += 1
    return seq
  }

  function publish (method: DRC_METHOD, data: Record<string, unknown>, waitForReply = true) {
    if (!deviceTopicInfo.pubTopic) {
      return Promise.reject(new Error('请先建立 DRC 链路'))
    }
    const commandSeq = nextSeq()
    const body = buildSpeakerCommand(method, commandSeq, data)
    if (!waitForReply) return Promise.resolve(body)
    return new Promise<SpeakerDrcMessage<any>>((resolve, reject) => {
      const timer = setTimeout(() => {
        pending.delete(commandSeq)
        reject(new Error(`${method} 响应超时`))
      }, COMMAND_TIMEOUT_MS)
      pending.set(commandSeq, { method, resolve, reject, timer })
      mqttHooks.publishMqtt(deviceTopicInfo.pubTopic, body, { qos: 1 })
    })
  }

  function applyDeviceState (state: As1State) {
    device.value = state
    const speaker = state.speaker
    if (!speaker) return
    if (typeof speaker.tts_volume === 'number') settings.volume = speaker.tts_volume
    if (typeof speaker.tts_type === 'number') settings.type = speaker.tts_type
    if (typeof speaker.tts_language === 'number') settings.language = speaker.tts_language
    if (typeof speaker.tts_speed === 'number') settings.speed = speaker.tts_speed
    if (typeof speaker.play_mode === 'number') settings.playMode = speaker.play_mode
    statusText.value = getSpeakerStatusText(speaker.system_state)
    busy.value = speaker.system_state !== undefined && speaker.system_state !== SpeakerSystemState.IDLE
    if (speaker.system_state === SpeakerSystemState.IDLE) progress.value = 0
  }

  function handleProgress (data: SpeakerProgressData) {
    const state = findAs1State(data)
    if (state) applyDeviceState(state)
    if (typeof data.progress?.percent === 'number') {
      progress.value = Math.min(100, Math.max(0, data.progress.percent))
    }
    const stepKey = data.progress?.step_key
    if (stepKey) statusText.value = SPEAKER_STEP_TEXT[stepKey] || stepKey
    if (typeof data.status === 'string') {
      busy.value = data.status === 'in_progress'
      if (data.status === 'success') progress.value = 100
      if (data.status === 'failed') errorText.value = errorText.value || '喊话器播放失败'
      if (!stepKey || data.status !== 'in_progress') {
        statusText.value = SPEAKER_PROGRESS_STATUS_TEXT[data.status] || data.status
      }
    }
    if (data.result) {
      errorText.value = getSpeakerErrorMessage(data.result)
      busy.value = false
    }
  }

  function resolveCommand (payload: SpeakerDrcMessage<any>) {
    if (typeof payload.seq !== 'number') return
    const request = pending.get(payload.seq)
    if (!request || request.method !== payload.method) return
    clearTimeout(request.timer)
    pending.delete(payload.seq)
    const result = payload.data?.result
    if (result) {
      const error = new Error(getSpeakerErrorMessage(result))
      request.reject(error)
      return
    }
    request.resolve(payload)
  }

  function handleMqttMessage (payload: SpeakerDrcMessage<any>) {
    if (!payload?.method) return
    if (payload.method === DRC_METHOD.PSDK_STATE_INFO) {
      const state = findAs1State(payload.data)
      if (state) applyDeviceState(state)
      return
    }
    if (payload.method === DRC_METHOD.SPEAKER_PLAY_PROGRESS) {
      handleProgress(payload.data || {})
      return
    }
    resolveCommand(payload)
  }

  async function discover () {
    if (!drcReady.value) return
    errorText.value = ''
    statusText.value = '正在获取 AS1 状态'
    try {
      await publish(DRC_METHOD.INITIAL_STATE_SUBSCRIBE, {})
    } catch (error: any) {
      errorText.value = error.message
    }
  }

  async function start () {
    const content = text.value.trim()
    if (!available.value || !device.value) {
      message.error('未识别到 AS1 喊话器，或 DRC 链路未建立')
      return
    }
    if (!content) {
      message.error('请输入喊话内容')
      return
    }
    errorText.value = ''
    busy.value = true
    progress.value = 0
    statusText.value = '正在配置喊话器'
    const psdkIndex = device.value.psdk_index
    try {
      await publish(DRC_METHOD.SPEAKER_TTS_SET, {
        psdk_index: psdkIndex,
        volume: clampSpeakerValue(settings.volume),
        type: settings.type,
        language: settings.language,
        speed: clampSpeakerSpeed(settings.speed),
      })
      await publish(DRC_METHOD.SPEAKER_PLAY_VOLUME_SET, {
        psdk_index: psdkIndex,
        play_volume: clampSpeakerValue(settings.volume),
      })
      await publish(DRC_METHOD.SPEAKER_PLAY_MODE_SET, {
        psdk_index: psdkIndex,
        play_mode: settings.playMode,
      })
      statusText.value = '正在发送 TTS 文本'
      await publish(DRC_METHOD.SPEAKER_TTS_PLAY_START, {
        psdk_index: psdkIndex,
        tts: {
          name: createSpeakerFileName(),
          text: content,
          md5: md5Utf8(content),
        },
      })
      statusText.value = '命令已受理，等待播放进度'
    } catch (error: any) {
      errorText.value = error.message
      statusText.value = '播放失败'
      busy.value = false
    }
  }

  async function stop () {
    if (!device.value) return
    try {
      statusText.value = '正在停止播放'
      await publish(DRC_METHOD.SPEAKER_PLAY_STOP, { psdk_index: device.value.psdk_index })
      busy.value = false
      progress.value = 0
      statusText.value = '已停止'
    } catch (error: any) {
      errorText.value = error.message
    }
  }

  async function replay () {
    if (!device.value) return
    try {
      errorText.value = ''
      busy.value = true
      statusText.value = '正在重新播放'
      await publish(DRC_METHOD.SPEAKER_REPLAY, { psdk_index: device.value.psdk_index })
    } catch (error: any) {
      errorText.value = error.message
      busy.value = false
    }
  }

  function reset () {
    device.value = null
    busy.value = false
    progress.value = 0
    statusText.value = '等待 AS1 状态'
    pending.forEach(request => {
      clearTimeout(request.timer)
      request.reject(new Error('DRC 链路已断开'))
    })
    pending.clear()
  }

  EventBus.on('droneControlMqttInfo', handleMqttMessage)
  watch(drcReady, ready => ready ? discover() : reset(), { immediate: true })

  onBeforeUnmount(() => {
    EventBus.off('droneControlMqttInfo', handleMqttMessage)
    reset()
  })

  return {
    available,
    busy,
    canStop,
    device,
    discover,
    drcReady,
    errorText,
    progress,
    replay,
    settings,
    start,
    statusText,
    stop,
    systemState,
    text,
  }
}
