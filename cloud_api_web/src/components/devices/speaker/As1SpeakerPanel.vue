<template>
  <section class="speaker-panel">
    <header class="speaker-header">
      <div>
        <div class="speaker-title">AS1 喊话器</div>
        <div class="speaker-device">
          {{ deviceSubtitle }}
        </div>
      </div>
      <div class="speaker-state" :class="stateClass">
        <span class="state-dot"></span>{{ displayStatus }}
      </div>
      <el-button class="speaker-toggle" size="small" link type="primary" @click="expanded = !expanded">
        {{ expanded ? '收起' : '展开' }}
      </el-button>
    </header>

    <div v-if="!expanded" class="speaker-compact">
      {{ compactText }}
      <div class="compact-actions">
        <el-button v-if="speaker.canStop" size="small" type="danger" plain @click="speaker.stop">停止播放</el-button>
        <el-button size="small" :type="speaker.drcReady ? 'warning' : 'primary'" plain :loading="drcLoading" @click="toggleDrc">
          {{ speaker.drcReady ? '断开设备' : '连接设备' }}
        </el-button>
      </div>
    </div>

    <template v-else>
    <div class="drc-actions">
      <span>{{ speaker.drcReady ? '设备连接正常' : '设备未连接' }}</span>
      <el-button size="small" :type="speaker.drcReady ? 'warning' : 'primary'" plain :loading="drcLoading" @click="toggleDrc">
        {{ speaker.drcReady ? '断开设备' : '连接设备' }}
      </el-button>
    </div>

    <div v-if="!speaker.drcReady" class="speaker-notice">
      请先连接设备，连接后即可使用文本播报和录音播报。
    </div>
    <div v-else-if="!speaker.available" class="speaker-notice">
      暂未检测到喊话器。
      <el-button link type="primary" @click="speaker.discover">重新检测</el-button>
    </div>

    <div class="speaker-tabs">
      <button :class="{ active: mode === 'tts' }" @click="mode = 'tts'">文本喊话</button>
      <button :class="{ active: mode === 'record' }" @click="mode = 'record'">录音喊话</button>
    </div>

    <template v-if="mode === 'tts'">
    <el-input
      v-model="speaker.text"
      type="textarea"
      :rows="3"
      maxlength="500"
      show-word-limit
      placeholder="请输入现场喊话内容"
      :disabled="!speaker.available || speaker.busy"
    />

    <div class="speaker-options">
      <label>
        <span>音色</span>
        <el-select v-model="speaker.settings.type" :disabled="speaker.busy">
          <el-option label="男声" :value="SpeakerTtsType.MALE" />
          <el-option label="女声" :value="SpeakerTtsType.FEMALE" />
        </el-select>
      </label>
      <label>
        <span>语言</span>
        <el-select v-model="speaker.settings.language" :disabled="speaker.busy">
          <el-option label="中文" :value="SpeakerTtsLanguage.CHINESE" />
          <el-option label="英文" :value="SpeakerTtsLanguage.ENGLISH" />
        </el-select>
      </label>
      <label>
        <span>模式</span>
        <el-select v-model="speaker.settings.playMode" :disabled="speaker.busy">
          <el-option label="单次播放" :value="SpeakerPlayMode.SINGLE" />
          <el-option label="循环播放" :value="SpeakerPlayMode.LOOP" />
        </el-select>
      </label>
    </div>

    <div class="speaker-slider">
      <span>音量</span>
      <el-slider v-model="speaker.settings.volume" :min="1" :max="100" :disabled="speaker.busy" />
      <strong>{{ speaker.settings.volume }}</strong>
    </div>
    <div class="speaker-slider">
      <span>语速</span>
      <el-slider v-model="speaker.settings.speed" :min="0" :max="100" :disabled="speaker.busy" />
      <strong>{{ speaker.settings.speed }}</strong>
    </div>
    <div class="speaker-hint">可根据现场环境调整音量、语速和播放模式。</div>

    <div v-if="speaker.busy || speaker.progress" class="speaker-progress">
      <el-progress :percentage="speaker.progress" :stroke-width="8" />
    </div>
    <div v-if="speaker.errorText" class="speaker-error">{{ toCustomerError(speaker.errorText) }}</div>

    <footer class="speaker-actions">
      <el-button
        type="primary"
        :loading="speaker.busy && !speaker.canStop"
        :disabled="!speaker.available || speaker.busy"
        @click="speaker.start"
      >开始播放</el-button>
      <el-button
        type="danger"
        plain
        :disabled="!speaker.canStop"
        @click="speaker.stop"
      >停止播放</el-button>
      <el-button
        :disabled="!speaker.available || speaker.busy"
        @click="speaker.replay"
      >重播</el-button>
    </footer>
    </template>

    <template v-else>
      <div class="record-card">
        <div class="record-options">
          <label>
            <span>音量</span>
            <el-slider v-model="recorder.volume" :min="0" :max="100" :disabled="recorder.recording || recorder.busy" />
            <strong>{{ recorder.volume }}</strong>
          </label>
          <label>
            <span>模式</span>
            <el-select v-model="recorder.playMode" :disabled="recorder.recording || recorder.busy">
              <el-option label="单次播放" :value="SpeakerPlayMode.SINGLE" />
              <el-option label="循环播放" :value="SpeakerPlayMode.LOOP" />
            </el-select>
          </label>
        </div>
        <div class="record-main">
          <button
            class="record-button"
            :class="{ recording: recorder.recording }"
            :disabled="!canStartRecord"
            @mousedown.prevent="recorder.start"
            @mouseup.prevent="recorder.stop"
            @mouseleave.prevent="recorder.stop"
            @touchstart.prevent="recorder.start"
            @touchend.prevent="recorder.stop"
          >{{ recorder.recording ? '松开播放' : '按住说话' }}</button>
          <div class="record-info">
            <strong>{{ displayRecordStatus }}</strong>
            <span>{{ recorder.seconds }}s</span>
          </div>
        </div>
        <div v-if="recorder.recording" class="speaker-progress">
          <el-progress :percentage="100" :indeterminate="true" :stroke-width="8" />
        </div>
        <div v-else-if="recorder.busy || recorder.progress" class="speaker-progress">
          <el-progress :percentage="recorder.progress" :stroke-width="8" />
        </div>
        <div class="speaker-hint">按住录音，松开后自动播放，播放结束自动清理临时音频。</div>
        <div v-if="recorder.errorText" class="speaker-error">{{ toCustomerError(recorder.errorText) }}</div>
        <footer class="speaker-actions">
          <el-button v-if="recorder.recording" type="warning" plain @click="recorder.cancel">取消录音</el-button>
          <el-button type="danger" plain :disabled="!canStopRecordPlayback" @click="stopRecordPlayback">停止播放</el-button>
          <el-button plain :disabled="!recorder.available || recorder.recording || recorder.busy" @click="recorder.replay">重播</el-button>
        </footer>
      </div>
    </template>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, defineProps, PropType, reactive, ref } from 'vue'
import { DeviceTopicInfo, DrcMqttPublisher } from '/@/components/g-map/use-mqtt'
import {
  SpeakerPlayMode,
  SpeakerSystemState,
  SpeakerTtsLanguage,
  SpeakerTtsType,
} from './as1-speaker'
import { useAs1Speaker } from './use-as1-speaker'
import { useAs1Recording } from './use-as1-recording'

const props = defineProps({
  deviceTopicInfo: {
    type: Object as PropType<DeviceTopicInfo>,
    required: true,
  },
  mqttHooks: {
    type: Object as PropType<DrcMqttPublisher>,
    required: true,
  },
  drcLoading: {
    type: Boolean,
    default: false,
  },
  onEnterDrc: {
    type: Function as PropType<() => Promise<boolean>>,
    required: true,
  },
  onExitDrc: {
    type: Function as PropType<() => Promise<boolean>>,
    required: true,
  },
})

const speakerState = useAs1Speaker(props.deviceTopicInfo, props.mqttHooks)
const speaker = reactive(speakerState)
const recorder = reactive(useAs1Recording(props.deviceTopicInfo, props.mqttHooks, speakerState.device))
const expanded = ref(false)
const mode = ref<'tts' | 'record'>('tts')
const stateClass = computed(() => ({
  ready: speaker.available && !speaker.busy && !recorder.busy,
  active: speaker.busy || recorder.busy || recorder.recording,
  error: speaker.systemState === SpeakerSystemState.ERROR || Boolean(speaker.errorText || recorder.errorText),
}))
const deviceSubtitle = computed(() => {
  if (!speaker.drcReady) return '请先连接设备'
  if (!speaker.available) return '正在检测喊话器'
  return '设备已就绪'
})
const displayStatus = computed(() => {
  if (speaker.systemState === SpeakerSystemState.ERROR || speaker.errorText || recorder.errorText) return '设备异常'
  if (recorder.recording) return '录音中'
  if (recorder.busy) return toCustomerStatus(recorder.statusText)
  if (speaker.busy) return toCustomerStatus(speaker.statusText)
  if (!speaker.drcReady) return '未连接'
  if (!speaker.available) return '检测中'
  return '空闲'
})
const displayRecordStatus = computed(() => toCustomerStatus(recorder.statusText))
const canStartRecord = computed(() => recorder.available && !recorder.recording && !recorder.busy)
const canStopRecordPlayback = computed(() => recorder.canStopPlayback || speaker.canStop || speaker.busy)
const compactText = computed(() => {
  if (!speaker.drcReady) return '设备未连接'
  if (!speaker.available) return '正在检测喊话器'
  return '喊话器已就绪'
})

function toCustomerStatus (status: string) {
  if (!status) return '准备就绪'
  if (status.includes('上传音频')) return '正在上传录音'
  if (status.includes('下发播放指令') || status.includes('命令已下发')) return '正在准备播放'
  if (status.includes('获取 AS1') || status.includes('等待 AS1')) return '正在检测喊话器'
  if (status.includes('DRC')) return status.replace(/DRC 链路/g, '设备连接').replace(/DRC/g, '设备')
  return status
}

function toCustomerError (error: string) {
  return error
    .replace(/DRC 链路/g, '设备连接')
    .replace(/DRC/g, '设备')
    .replace(/psdk_index/gi, '设备信息')
    .replace(/md5/gi, '音频校验')
    .replace(/objectKey/g, '临时文件')
}

async function toggleDrc () {
  if (speaker.drcReady) {
    await props.onExitDrc()
  } else {
    await props.onEnterDrc()
  }
}

async function stopRecordPlayback () {
  if (recorder.canStopPlayback) {
    await recorder.stopPlayback()
  }
  if (speaker.canStop || speaker.busy) {
    await speaker.stop()
  }
}
</script>

<style lang="scss" scoped>
.speaker-panel {
  margin: 10px 5px 5px;
  padding: 14px;
  color: #e9f2ff;
  background:
    radial-gradient(circle at 90% 0%, rgba(54, 180, 255, 0.18), transparent 34%),
    linear-gradient(135deg, #0b214d 0%, #0d2d65 100%);
  border: 1px solid rgba(93, 166, 255, 0.42);
  border-radius: 6px;
}

.speaker-header,
.speaker-actions,
.speaker-slider,
.speaker-options {
  display: flex;
  align-items: center;
}

.speaker-header {
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 8px;
}

.speaker-title { font-size: 16px; font-weight: 600; }
.speaker-device { margin-top: 2px; color: #8fb5e8; font-size: 11px; }
.speaker-state { color: #93a8c4; font-size: 12px; }
.speaker-state.ready { color: #55d99b; }
.speaker-state.active { color: #48bfff; }
.speaker-state.error { color: #ff7070; }
.speaker-toggle { padding: 0 4px; }
.state-dot { display: inline-block; width: 7px; height: 7px; margin-right: 6px; background: currentColor; border-radius: 50%; box-shadow: 0 0 8px currentColor; }
.speaker-compact { display: flex; align-items: center; justify-content: space-between; gap: 8px; color: #9ebbe0; font-size: 12px; }
.speaker-notice { margin-bottom: 10px; padding: 8px 10px; color: #b9d3f5; background: rgba(3, 17, 43, 0.48); border-radius: 4px; font-size: 12px; }
.compact-actions,
.drc-actions { display: flex; align-items: center; gap: 8px; }
.drc-actions { justify-content: space-between; margin-bottom: 10px; padding: 8px 10px; color: #b9d3f5; background: rgba(3, 17, 43, 0.36); border-radius: 4px; font-size: 12px; }
.speaker-options { gap: 8px; margin: 12px 0; }
.speaker-options label { flex: 1; min-width: 0; }
.speaker-options label > span { display: block; margin-bottom: 5px; color: #9ebbe0; font-size: 12px; }
.speaker-options :deep(.el-select) { width: 100%; }
.speaker-slider { gap: 10px; min-height: 32px; }
.speaker-slider > span { width: 30px; color: #9ebbe0; font-size: 12px; }
.speaker-slider :deep(.el-slider) { flex: 1; }
.speaker-slider strong { width: 28px; color: #fff; font-size: 12px; text-align: right; }
.speaker-progress { margin-top: 8px; }
.speaker-error { margin-top: 8px; color: #ff8181; font-size: 12px; }
.speaker-hint { margin-top: 5px; color: #7fa9da; font-size: 11px; }
.speaker-actions { gap: 8px; margin-top: 13px; }
.speaker-tabs { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 10px; }
.speaker-tabs button { height: 30px; color: #9fbfe5; background: rgba(6, 22, 52, 0.6); border: 1px solid rgba(96, 148, 208, 0.32); border-radius: 4px; cursor: pointer; }
.speaker-tabs button.active { color: #071426; background: linear-gradient(135deg, #7ed7ff, #baf0c6); border-color: transparent; }
.record-card { padding: 10px; background: rgba(4, 17, 39, 0.42); border: 1px solid rgba(103, 153, 207, 0.22); border-radius: 6px; }
.record-options { display: grid; grid-template-columns: 1fr 120px; gap: 10px; margin-bottom: 12px; }
.record-options label { display: flex; align-items: center; gap: 8px; min-width: 0; }
.record-options label span { color: #8fb7e2; font-size: 12px; white-space: nowrap; }
.record-options label strong { min-width: 26px; color: #e7f3ff; font-size: 12px; }
.record-main { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; padding: 16px 0 8px; text-align: center; }
.record-button { width: 96px; height: 96px; color: #09233d; font-weight: 700; border: 0; border-radius: 50%; background: radial-gradient(circle at 35% 30%, #ffffff, #7ed7ff 48%, #4d9bff); box-shadow: 0 0 24px rgba(126, 215, 255, 0.35); cursor: pointer; }
.record-button.recording { color: #fff; background: radial-gradient(circle at 35% 30%, #ffb9b9, #ff5151 48%, #b81414); box-shadow: 0 0 28px rgba(255, 81, 81, 0.42); }
.record-button:disabled { cursor: not-allowed; filter: grayscale(0.8); opacity: 0.55; }
.record-info { display: flex; flex-direction: column; gap: 6px; color: #cce5ff; font-size: 12px; }
.speaker-actions :deep(.el-button) { flex: 1; margin-left: 0; }

@media (max-width: 1280px) {
  .speaker-options { flex-wrap: wrap; }
  .speaker-options label { flex-basis: calc(50% - 4px); }
}
</style>
