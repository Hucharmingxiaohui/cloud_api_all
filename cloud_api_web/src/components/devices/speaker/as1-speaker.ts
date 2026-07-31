import { md5 } from 'js-md5'

export enum SpeakerSystemState {
  IDLE = 0,
  TRANSFERRING = 1,
  PLAYING = 2,
  ERROR = 3,
  TTS_PROCESSING = 4,
  DOWNLOADING = 99,
}

export enum SpeakerPlayMode {
  SINGLE = 0,
  LOOP = 1,
}

export enum SpeakerTtsType {
  MALE = 0,
  FEMALE = 1,
}

export enum SpeakerTtsLanguage {
  CHINESE = 0,
  ENGLISH = 1,
}

export interface SpeakerSettings {
  volume: number
  type: SpeakerTtsType
  language: SpeakerTtsLanguage
  speed: number
  playMode: SpeakerPlayMode
}

export interface SpeakerRuntimeState {
  work_mode?: number
  play_mode?: SpeakerPlayMode
  system_state?: SpeakerSystemState
  play_volume?: number
  play_file_name?: string
  play_file_md5?: string
  tts_volume?: number
  tts_type?: SpeakerTtsType
  tts_language?: SpeakerTtsLanguage
  tts_speed?: number
  [key: string]: unknown
}

export interface As1State {
  psdk_index: number
  psdk_type?: number
  psdk_name: 'Speaker'
  psdk_sn?: string
  psdk_version?: string
  psdk_lib_version?: string
  speaker?: SpeakerRuntimeState
}

export interface SpeakerDrcMessage<T = Record<string, unknown>> {
  method: string
  seq?: number
  data: T
}

export interface SpeakerProgressData extends Partial<As1State> {
  result?: number
  status?: 'failed' | 'in_progress' | 'success'
  progress?: {
    percent?: number
    step_key?: string
    [key: string]: unknown
  }
}

export interface SpeakerAudioProgressData {
  result?: number
  output?: {
    psdk_index?: number
    status?: 'failed' | 'in_progress' | 'ok' | 'success'
    md5?: string
    progress?: {
      percent?: number
      step_key?: string
      [key: string]: unknown
    }
    [key: string]: unknown
  }
}

export const SPEAKER_PROGRESS_STATUS_TEXT: Record<string, string> = {
  failed: '播放失败',
  in_progress: '处理中',
  success: '播放完成',
}

const STATUS_TEXT: Record<number, string> = {
  [SpeakerSystemState.IDLE]: '空闲',
  [SpeakerSystemState.TRANSFERRING]: '正在传输',
  [SpeakerSystemState.PLAYING]: '正在播放',
  [SpeakerSystemState.ERROR]: '设备异常',
  [SpeakerSystemState.TTS_PROCESSING]: 'TTS 文本转换中',
  [SpeakerSystemState.DOWNLOADING]: '正在下载',
}

export const SPEAKER_STEP_TEXT: Record<string, string> = {
  change_work_mode: '正在切换喊话模式',
  download: '正在下载内容',
  encoding: '正在编码音频',
  tts_processing: '正在合成语音',
  upload: '正在传输内容',
  play: '正在播放',
}

export const SPEAKER_AUDIO_METHOD = {
  PLAY_START: 'speaker_audio_play_start',
  PLAY_PROGRESS: 'speaker_audio_play_start_progress',
  PLAY_VOLUME_SET: 'speaker_play_volume_set',
  PLAY_MODE_SET: 'speaker_play_mode_set',
  PLAY_STOP: 'speaker_play_stop',
  REPLAY: 'speaker_replay',
} as const

export function buildSpeakerServiceCommand (method: string, data: Record<string, unknown>) {
  return {
    bid: crypto.randomUUID(),
    data,
    method,
    tid: crypto.randomUUID(),
    timestamp: Date.now(),
  }
}

const ERROR_TEXT: Record<number, string> = {
  327050: '当前设备状态不支持播放',
  327051: '下载音频失败',
  327052: '工作模式切换失败',
  327054: '播放失败',
  327057: '停止播放失败',
  327058: '设置播放模式失败',
  327059: '设置音量失败',
  327061: '发送文本失败',
  327062: '切换语言失败',
  327068: '音频 MD5 校验失败',
  327071: 'TTS 文本 MD5 校验失败',
  327072: '上传 TTS 文本失败',
  327073: 'TTS 上传异常终止',
  327074: '重播失败',
  327075: '编码失败',
}

export function md5Utf8 (text: string): string {
  return md5(text)
}

export function createSpeakerFileName (timestamp = Date.now()): string {
  return `as1-${timestamp}`
}

export function buildSpeakerCommand<T extends Record<string, unknown>> (
  method: string,
  seq: number,
  data: T
): SpeakerDrcMessage<T> {
  return { method, seq, data }
}

export function findAs1State (data: unknown): As1State | null {
  const candidates = Array.isArray(data) ? data : [data]
  for (const candidate of candidates) {
    if (!candidate || typeof candidate !== 'object') continue
    const state = candidate as Partial<As1State>
    if (state.psdk_name === 'Speaker' && typeof state.psdk_index === 'number') {
      return state as As1State
    }
  }
  return null
}

export function getSpeakerStatusText (state?: number): string {
  if (state === undefined) return '等待设备状态'
  return STATUS_TEXT[state] || `未知状态 (${state})`
}

export function getSpeakerErrorMessage (result: number): string {
  return ERROR_TEXT[result] || `喊话器操作失败 (${result})`
}

export function clampSpeakerValue (value: number, min = 1, max = 100): number {
  return Math.min(max, Math.max(min, Math.round(value)))
}

export function clampSpeakerSpeed (value: number): number {
  return clampSpeakerValue(value, 0, 100)
}
