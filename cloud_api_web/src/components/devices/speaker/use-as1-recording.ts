import { computed, onBeforeUnmount, ref } from 'vue'
import { message } from 'ant-design-vue'
import EventBus from '/@/event-bus'
import { DeviceTopicInfo, DrcMqttPublisher } from '/@/components/g-map/use-mqtt'
import { useMyStore } from '/@/store'
import { deleteSpeakerAudio, uploadSpeakerAudio } from '/@/api/speaker'
import {
  As1State,
  buildSpeakerServiceCommand,
  clampSpeakerValue,
  SPEAKER_AUDIO_METHOD,
  SPEAKER_STEP_TEXT,
  SpeakerAudioProgressData,
  SpeakerPlayMode,
} from './as1-speaker'

const TARGET_SAMPLE_RATE = 16000

export function useAs1Recording (
  deviceTopicInfo: DeviceTopicInfo,
  mqttHooks: DrcMqttPublisher,
  device: { value: As1State | null }
) {
  const store = useMyStore()
  const recording = ref(false)
  const busy = ref(false)
  const canStopPlayback = ref(false)
  const seconds = ref(0)
  const progress = ref(0)
  const volume = ref(50)
  const playMode = ref<SpeakerPlayMode>(SpeakerPlayMode.SINGLE)
  const statusText = ref('等待录音')
  const errorText = ref('')
  const lastObjectKey = ref('')
  const lastMd5 = ref('')
  const pending = new Map<string, {
    method: string
    resolve:() => void
    reject: (error: Error) => void
    timer: ReturnType<typeof setTimeout>
  }>()
  let stream: MediaStream | null = null
  let audioContext: AudioContext | null = null
  let source: MediaStreamAudioSourceNode | null = null
  let processor: ScriptProcessorNode | null = null
  let silentGain: GainNode | null = null
  let timer: ReturnType<typeof setInterval> | null = null
  let rawMqttAttached = false
  const chunks: Float32Array[] = []

  const available = computed(() => Boolean(deviceTopicInfo.sn && device.value))
  async function start () {
    if (!available.value || !device.value) {
      message.error('未识别到 AS1 喊话器')
      return
    }
    if (recording.value || busy.value) return
    errorText.value = ''
    progress.value = 0
    seconds.value = 0
    chunks.length = 0
    try {
      stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: false,
          noiseSuppression: false,
          autoGainControl: false,
          channelCount: 1,
        },
      })
      audioContext = new AudioContext()
      source = audioContext.createMediaStreamSource(stream)
      processor = audioContext.createScriptProcessor(4096, 1, 1)
      silentGain = audioContext.createGain()
      silentGain.gain.value = 0
      processor.onaudioprocess = event => {
        if (!recording.value) return
        chunks.push(new Float32Array(event.inputBuffer.getChannelData(0)))
      }
      source.connect(processor)
      processor.connect(silentGain)
      silentGain.connect(audioContext.destination)
      recording.value = true
      statusText.value = '录音中'
      timer = setInterval(() => {
        seconds.value += 1
      }, 1000)
    } catch (error: any) {
      errorText.value = error?.message || '无法访问麦克风'
      cleanupRecorder()
    }
  }

  async function stop () {
    if (!recording.value) return
    recording.value = false
    if (!audioContext) return
    const sourceRate = audioContext.sampleRate
    cleanupRecorder(false)
    busy.value = true
    statusText.value = '正在生成音频'
    try {
      const pcm = encodePcm(chunks, sourceRate, TARGET_SAMPLE_RATE)
      cleanupRecorder()
      if (pcm.size <= 0) throw new Error('录音内容为空')
      statusText.value = `正在上传音频 (${Math.round(pcm.size / 1024)}KB)`
      const uploadResp = await uploadSpeakerAudio(deviceTopicInfo.sn, pcm)
      if (uploadResp.code !== 0) throw new Error(uploadResp.message || '上传录音失败')
      const audio = uploadResp.data
      if (lastMd5.value && lastMd5.value === audio.md5) {
        errorText.value = '本次录音和上次音频完全相同，请确认浏览器麦克风输入是否正确'
      }
      lastMd5.value = audio.md5
      const objectKey = audio.object_key || audio.objectKey || ''
      lastObjectKey.value = objectKey
      statusText.value = `正在下发播放指令 (${audio.md5.slice(0, 8)})`
      await publishService(SPEAKER_AUDIO_METHOD.PLAY_VOLUME_SET, {
        psdk_index: device.value?.psdk_index,
        play_volume: clampSpeakerValue(volume.value),
      })
      await publishService(SPEAKER_AUDIO_METHOD.PLAY_MODE_SET, {
        psdk_index: device.value?.psdk_index,
        play_mode: playMode.value,
      })
      publishAudio(audio.name, audio.url, audio.md5, audio.format || 'pcm')
      canStopPlayback.value = true
    } catch (error: any) {
      errorText.value = error?.message || '录音喊话失败'
      busy.value = false
      await deleteLastObject()
    }
  }

  function cancel () {
    recording.value = false
    cleanupRecorder()
    chunks.length = 0
    seconds.value = 0
    statusText.value = '录音已取消'
  }

  function publishAudio (name: string, url: string, md5: string, format: string) {
    if (!device.value) throw new Error('未识别到 AS1 喊话器')
    publishService(SPEAKER_AUDIO_METHOD.PLAY_START, {
      psdk_index: device.value.psdk_index,
      file: { name, url, md5, format },
    }, false)
    statusText.value = '命令已下发，等待播放进度'
  }

  function publishService (method: string, data: Record<string, unknown>, waitReply = true): Promise<void> {
    if (!store.state.mqttState) throw new Error('MQTT 连接未就绪')
    attachRawMqttListener()
    store.state.mqttState.subscribeMqtt(`thing/product/${deviceTopicInfo.sn}/services_reply`)
    store.state.mqttState.subscribeMqtt(`thing/product/${deviceTopicInfo.sn}/events`)
    const command = buildSpeakerServiceCommand(method, data)
    const topic = `thing/product/${deviceTopicInfo.sn}/services`
    mqttHooks.publishMqtt(topic, command)
    if (!waitReply) return Promise.resolve()
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        pending.delete(command.tid)
        reject(new Error(`${method} 指令响应超时`))
      }, 8000)
      pending.set(command.tid, { method, resolve, reject, timer })
    })
  }

  async function stopPlayback () {
    if (!device.value) return
    errorText.value = ''
    statusText.value = '正在停止播放'
    try {
      await publishService(SPEAKER_AUDIO_METHOD.PLAY_STOP, { psdk_index: device.value.psdk_index })
      busy.value = false
      canStopPlayback.value = false
      progress.value = 0
      statusText.value = '已停止'
      await deleteLastObject()
    } catch (error: any) {
      errorText.value = error?.message || '停止播放失败'
      busy.value = false
      canStopPlayback.value = false
    }
  }

  async function replay () {
    if (!device.value) return
    errorText.value = ''
    statusText.value = '正在重新播放'
    busy.value = true
    try {
      await publishService(SPEAKER_AUDIO_METHOD.REPLAY, { psdk_index: device.value.psdk_index })
      statusText.value = '重播指令已下发'
    } catch (error: any) {
      errorText.value = error?.message || '重播失败'
      busy.value = false
    }
  }

  async function deleteLastObject () {
    if (!lastObjectKey.value) return
    const objectKey = lastObjectKey.value
    lastObjectKey.value = ''
    try {
      await deleteSpeakerAudio(objectKey)
    } catch (error) {
      console.warn('Failed to delete temporary speaker audio:', objectKey, error)
    }
  }

  function onMqttMessage (payload: any) {
    resolveServiceReply(payload)
    if (payload.method !== SPEAKER_AUDIO_METHOD.PLAY_PROGRESS) return
    const data = payload.data as SpeakerAudioProgressData
    if (data.output?.psdk_index !== device.value?.psdk_index) return
    const stepKey = data.output?.progress?.step_key
    if (typeof data.output?.progress?.percent === 'number') progress.value = data.output.progress.percent
    if (stepKey) statusText.value = SPEAKER_STEP_TEXT[stepKey] || stepKey
    if (data.output?.status === 'ok' || data.output?.status === 'success') {
      progress.value = 100
      statusText.value = '录音播放完成'
      busy.value = false
      canStopPlayback.value = false
      deleteLastObject()
    }
    if (data.output?.status === 'failed' || (typeof data.result === 'number' && data.result !== 0)) {
      errorText.value = `录音播放失败：${data.result ?? 'unknown'}`
      busy.value = false
      canStopPlayback.value = false
      deleteLastObject()
    }
  }

  function resolveServiceReply (payload: any) {
    const tid = payload?.tid
    if (!tid || !pending.has(tid)) return
    const request = pending.get(tid)!
    clearTimeout(request.timer)
    pending.delete(tid)
    const result = payload?.data?.result
    if (typeof result === 'number' && result !== 0) {
      request.reject(new Error(`${request.method} 执行失败：${result}`))
    } else {
      request.resolve()
    }
  }

  function cleanupRecorder (closeContext = true) {
    if (timer) clearInterval(timer)
    timer = null
    processor?.disconnect()
    silentGain?.disconnect()
    source?.disconnect()
    stream?.getTracks().forEach(track => track.stop())
    processor = null
    silentGain = null
    source = null
    stream = null
    if (closeContext && audioContext) {
      audioContext.close()
      audioContext = null
    }
  }

  EventBus.on('droneControlMqttInfo', onMqttMessage)
  attachRawMqttListener()
  onBeforeUnmount(() => {
    EventBus.off('droneControlMqttInfo', onMqttMessage)
    store.state.mqttState?.off('onMessageMqtt', onRawMqttMessage)
    pending.forEach(request => {
      clearTimeout(request.timer)
      request.reject(new Error('录音喊话组件已卸载'))
    })
    pending.clear()
    cleanupRecorder()
  })

  function attachRawMqttListener () {
    if (rawMqttAttached || !store.state.mqttState) return
    store.state.mqttState.on('onMessageMqtt', onRawMqttMessage)
    rawMqttAttached = true
  }

  return {
    available,
    recording,
    busy,
    canStopPlayback,
    volume,
    playMode,
    seconds,
    progress,
    statusText,
    errorText,
    start,
    stop,
    stopPlayback,
    replay,
    cancel,
    deleteLastObject,
  }
}

function onRawMqttMessage (message: any) {
  const topic = message?.topic || ''
  if (!topic.endsWith('/events') && !topic.endsWith('/services_reply')) return
  try {
    const payloadStr = new TextDecoder('utf-8').decode(message?.payload)
    const payloadObj = JSON.parse(payloadStr)
    if (payloadObj?.method === SPEAKER_AUDIO_METHOD.PLAY_PROGRESS) {
      EventBus.emit('droneControlMqttInfo', payloadObj)
    }
  } catch (error) {
    console.warn('Failed to parse speaker audio mqtt message:', error)
  }
}

function encodePcm (chunks: Float32Array[], sourceRate: number, targetRate: number): Blob {
  const sourceLength = chunks.reduce((sum, chunk) => sum + chunk.length, 0)
  const source = new Float32Array(sourceLength)
  let offset = 0
  for (const chunk of chunks) {
    source.set(chunk, offset)
    offset += chunk.length
  }
  const targetLength = Math.max(1, Math.round(source.length * targetRate / sourceRate))
  const pcm = new Int16Array(targetLength)
  for (let i = 0; i < targetLength; i++) {
    const srcIndex = i * sourceRate / targetRate
    const before = Math.floor(srcIndex)
    const after = Math.min(before + 1, source.length - 1)
    const weight = srcIndex - before
    const sample = source[before] * (1 - weight) + source[after] * weight
    const clamped = Math.max(-1, Math.min(1, sample))
    pcm[i] = clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff
  }
  return new Blob([pcm.buffer], { type: 'application/octet-stream' })
}
