<template>
    <div v-if="isLoading" class="loading-overlay" >
        <div v-loading="true"></div>
        <div style="margin-top: 20px; color: #3990EE;">页面努力加载中...</div>
    </div>
  </template>

<script setup lang="ts">
import { ref, defineProps, watch } from 'vue'
import LottieAnimation from './LottieAnimation.vue'
const props = defineProps({
  isLoading: {
    type: Boolean,
    default: true,
  },
})

function getRandomInt (): number {
  return Math.floor(Math.random() * 2)
}

const lottieList = ref([
  'https://lottie.host/4f0fb5d6-1873-4cfd-a8d5-8387f094d818/WCv6ECocUx.json',
  'https://lottie.host/a2e813e0-9448-420f-b0af-586eb9aaa738/uuz2oRP47a.json',
])

watch(() => props.isLoading, (newValue) => {
  if (newValue) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

</script>

  <style scoped>
  .loading-overlay {
    position: fixed;
    top: 50%;
    left: 50%;
    width: fit-content;
    height: 100px;
    background: transparent;
    transition: -50% -50%;

  }

  .spinner {
    border: 4px solid rgba(0, 0, 0, 0.1);
    border-left-color: #000;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
    opacity: 1;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
  </style>
