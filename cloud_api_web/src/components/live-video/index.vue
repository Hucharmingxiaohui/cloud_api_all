<template>
  <div class="drone-live-container">
    <div class="live-header">
      <div class="header-left">
        <span class="header-title">无人机直播</span>
        <span class="status-dot" :class="{ active: isPlaying }"></span>
        <span class="status-text">{{ isPlaying ? '直播中' : '已停止' }}</span>
      </div>
      <div class="header-right">
        <span class="stream-label">视频流：</span>
        <span class="stream-url">{{ streamUrl }}</span>
      </div>
    </div>

    <div class="live-body">
      <div class="video-wrapper">
        <video
          id="droneVideo"
          class="video-player"
          autoplay
          controls
          :style="{ width: '100%', height: '100%' }"
        ></video>
        <div v-if="!isPlaying && !isLoading" class="video-overlay">
          <div class="overlay-content">
            <div class="play-icon" @click="startPlay">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="11" stroke="currentColor" stroke-width="1.5" fill="rgba(255,255,255,0.1)"/>
                <polygon points="10,8 16,12 10,16" fill="currentColor"/>
              </svg>
            </div>
            <p class="overlay-tip">点击播放直播流</p>
          </div>
        </div>
        <div v-if="isLoading" class="video-overlay">
          <div class="loading-spinner"></div>
          <p class="overlay-tip">正在连接视频流...</p>
        </div>
      </div>
    </div>

    <div class="live-footer">
      <div class="footer-controls">
        <el-button
          :type="isPlaying ? 'danger' : 'primary'"
          :icon="isPlaying ? 'VideoPause' : 'VideoPlay'"
          @click="togglePlay"
        >
          {{ isPlaying ? '停止' : '播放' }}
        </el-button>
        <el-button @click="refreshStream" :disabled="isLoading">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:middle">
            <path d="M1 4v6h6M23 20v-6h-6"/>
            <path d="M20.49 9A9 9 0 005.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 013.51 15"/>
          </svg>
          刷新
        </el-button>
        <el-button @click="toggleMute">
          <svg v-if="!isMuted" width="16" height="16" viewBox="0 0 24 24" fill="currentColor" style="margin-right:4px;vertical-align:middle">
            <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3A4.5 4.5 0 0014 8.5v7a4.49 4.49 0 002.5-3.5zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/>
          </svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor" style="margin-right:4px;vertical-align:middle">
            <path d="M16.5 12A4.5 4.5 0 0014 7.5v2.05l2.02 2.02c.2-.37.38-.75.48-1.07zM19 12c0 .82-.13 1.6-.38 2.35l1.45 1.45A9.5 9.5 0 0021 12c0-4.28-2.99-7.86-7-8.77v2.06a7.5 7.5 0 015.38 6.71h.62zM19.96 20.37L3.67 4.1 2.25 5.52 6.28 9.55H5v5h4l5 5v-3.73l5.53 5.53 1.43-1.43zM12 19.04v-2.08l-2.15-2.15-1.96-1.96H7v-1.73l4.81 4.81L12 19.04z"/>
          </svg>
          {{ isMuted ? '取消静音' : '静音' }}
        </el-button>
      </div>
      <div class="footer-info">
        <span class="info-item">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" style="margin-right:4px;vertical-align:middle">
            <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
          </svg>
          固定地址
        </span>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from 'vue'
import flvjs from 'flv.js'
import { CURRENT_CONFIG as config } from '/@/api/http/config'

const streamUrl = config.droneLiveURL || 'http://172.20.63.157:9080/live/8UUXN3U00A046E-165-0-7.flv'
const isPlaying = ref(false)
const isLoading = ref(false)
const isMuted = ref(false)
const flvPlayer = ref<any>(null)
const videoRef = ref<HTMLVideoElement | null>(null)

function initFlv () {
  videoRef.value = document.getElementById('droneVideo') as HTMLVideoElement
  if (!videoRef.value) return

  if (!flvjs.isSupported()) {
    console.log('FLV not supported')
    return
  }

  try {
    flvPlayer.value = flvjs.createPlayer({
      type: 'flv',
      url: streamUrl,
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
      autoCleanupSourceBuffer: true
    })

    flvPlayer.value.on(flvjs.Events.ERROR, () => {
      isPlaying.value = false
      isLoading.value = false
    })

    flvPlayer.value.on(flvjs.Events.PLAYING, () => {
      isPlaying.value = true
      isLoading.value = false
    })

    flvPlayer.value.attachMediaElement(videoRef.value)
    flvPlayer.value.load()
    videoRef.value.addEventListener('loadedmetadata', () => {
      flvPlayer.value.play()
    }, { once: true })
  } catch (error) {
    console.log('创建播放器失败:', error)
    isPlaying.value = false
    isLoading.value = false
  }
}

function startPlay () {
  isLoading.value = true
  if (!flvPlayer.value) {
    initFlv()
  } else {
    flvPlayer.value.play()
    isLoading.value = false
    isPlaying.value = true
  }
}

function stopPlay () {
  if (flvPlayer.value) {
    flvPlayer.value.pause()
  }
  isPlaying.value = false
}

function togglePlay () {
  if (isPlaying.value) {
    stopPlay()
  } else {
    startPlay()
  }
}

function refreshStream () {
  stopPlay()
  if (flvPlayer.value) {
    flvPlayer.value.unload()
    flvPlayer.value.detachMediaElement()
    flvPlayer.value.destroy()
    flvPlayer.value = null
  }
  isLoading.value = true
  setTimeout(() => {
    initFlv()
  }, 500)
}

function toggleMute () {
  if (videoRef.value) {
    videoRef.value.muted = !videoRef.value.muted
    isMuted.value = videoRef.value.muted
  }
}

onUnmounted(() => {
  if (flvPlayer.value) {
    flvPlayer.value.pause()
    flvPlayer.value.unload()
    flvPlayer.value.detachMediaElement()
    flvPlayer.value.destroy()
    flvPlayer.value = null
  }
})
</script>

<style lang="scss" scoped>
@use '/@/styles/index.scss';

.drone-live-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #0a1628;
  color: #e0e4ed;
}

.live-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: linear-gradient(135deg, #0f1f3d 0%, #162a4d 100%);
  border-bottom: 1px solid rgba(89, 151, 255, 0.2);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #666;
  transition: all 0.3s;

  &.active {
    background: #00e676;
    box-shadow: 0 0 8px rgba(0, 230, 118, 0.6);
    animation: pulse 2s infinite;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  font-size: 13px;
  color: #8899b4;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stream-label {
  font-size: 12px;
  color: #8899b4;
}

.stream-url {
  font-size: 12px;
  color: #5997ff;
  font-family: 'Consolas', monospace;
  background: rgba(89, 151, 255, 0.1);
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid rgba(89, 151, 255, 0.2);
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.live-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  overflow: hidden;
}

.video-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
}

.video-player {
  display: block;
  object-fit: contain;
}

.video-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  z-index: 10;
}

.overlay-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.play-icon {
  cursor: pointer;
  color: #5997ff;
  transition: all 0.3s;

  &:hover {
    color: #7ab3ff;
    transform: scale(1.1);
  }
}

.overlay-tip {
  font-size: 14px;
  color: #8899b4;
  margin: 0;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 3px solid rgba(89, 151, 255, 0.2);
  border-top-color: #5997ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.live-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: linear-gradient(135deg, #0f1f3d 0%, #162a4d 100%);
  border-top: 1px solid rgba(89, 151, 255, 0.2);
  flex-shrink: 0;
}

.footer-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.footer-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item {
  font-size: 12px;
  color: #8899b4;
  display: flex;
  align-items: center;
}
</style>
