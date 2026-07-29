<template>
  <section class="speaker-panel">
    <header class="speaker-header">
      <div>
        <div class="speaker-title">AS1 喊话器</div>
        <div class="speaker-device">
          {{ speaker.device?.psdk_sn || '等待设备上报' }}
          <span v-if="speaker.device?.psdk_version"> · {{ speaker.device.psdk_version }}</span>
        </div>
      </div>
      <div class="speaker-state" :class="stateClass">
        <span class="state-dot"></span>{{ speaker.statusText }}
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
          {{ speaker.drcReady ? '关闭DRC' : '建立DRC' }}
        </el-button>
      </div>
    </div>

    <template v-else>
    <div class="drc-actions">
      <span>{{ speaker.drcReady ? 'DRC 链路已建立' : 'DRC 链路未建立' }}</span>
      <el-button size="small" :type="speaker.drcReady ? 'warning' : 'primary'" plain :loading="drcLoading" @click="toggleDrc">
        {{ speaker.drcReady ? '关闭DRC链路' : '建立DRC链路' }}
      </el-button>
    </div>

    <div v-if="!speaker.drcReady" class="speaker-notice">
      可直接点击上方按钮建立 DRC 链路；若失败，请查看弹出的接口错误信息。
    </div>
    <div v-else-if="!speaker.available" class="speaker-notice">
      未识别到 AS1 喊话器。
      <el-button link type="primary" @click="speaker.discover">重新检测</el-button>
    </div>

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
    <div class="speaker-hint">语速范围 0-100；音量会同时写入 TTS 音量和播放音量。</div>

    <div v-if="speaker.busy || speaker.progress" class="speaker-progress">
      <el-progress :percentage="speaker.progress" :stroke-width="8" />
    </div>
    <div v-if="speaker.errorText" class="speaker-error">{{ speaker.errorText }}</div>

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
  </section>
</template>

<script setup lang="ts">
import { computed, defineProps, PropType, reactive, ref, toRef } from 'vue'
import { DeviceTopicInfo, DrcMqttPublisher } from '/@/components/g-map/use-mqtt'
import {
  SpeakerPlayMode,
  SpeakerSystemState,
  SpeakerTtsLanguage,
  SpeakerTtsType,
} from './as1-speaker'
import { useAs1Speaker } from './use-as1-speaker'

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

const speaker = reactive(useAs1Speaker(props.deviceTopicInfo, props.mqttHooks))
const expanded = ref(false)
const stateClass = computed(() => ({
  ready: speaker.available && !speaker.busy,
  active: speaker.busy,
  error: speaker.systemState === SpeakerSystemState.ERROR || Boolean(speaker.errorText),
}))
const compactText = computed(() => {
  if (!speaker.drcReady) return '未建立 DRC 链路'
  if (!speaker.available) return '等待识别 AS1 设备'
  return speaker.device?.psdk_sn ? `设备 ${speaker.device.psdk_sn}` : 'AS1 已识别'
})

async function toggleDrc () {
  if (speaker.drcReady) {
    await props.onExitDrc()
  } else {
    await props.onEnterDrc()
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
.speaker-actions :deep(.el-button) { flex: 1; margin-left: 0; }

@media (max-width: 1280px) {
  .speaker-options { flex-wrap: wrap; }
  .speaker-options label { flex-basis: calc(50% - 4px); }
}
</style>
