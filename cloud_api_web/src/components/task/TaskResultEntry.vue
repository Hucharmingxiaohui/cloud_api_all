<template>
  <ConstructionWorkspace v-if="route.query.demo_task" :key="String(route.query.demo_task)" />
  <div v-else-if="loading" class="result-entry-status">正在读取任务类型…</div>
  <div v-else-if="error" class="result-entry-status"><p>{{ error }}</p><el-button @click="load">重试</el-button><el-button @click="back">返回飞行任务</el-button></div>
  <ConstructionTaskResult v-else-if="construction" :key="jobId" :job-id="jobId" />
  <LegacyTaskResult v-else-if="jobId" :key="jobId" />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTaskResultTypeApi } from '/@/api/media'
import { ERouterName } from '/@/types'
import { isConstructionTask } from '../construction/constructionTaskType'
import ConstructionTaskResult from '../construction/ConstructionTaskResult.vue'
import LegacyTaskResult from './task_result.vue'
import ConstructionWorkspace from '../construction/ConstructionWorkspace.vue'

const route = useRoute(); const router = useRouter()
const loading = ref(true); const error = ref(''); const jobId = ref(''); const construction = ref(false)
let generation = 0
function back () { router.push('/' + ERouterName.TASK) }
async function load () {
  const current = ++generation
  loading.value = true; error.value = ''; jobId.value = ''
  if (route.query.demo_task) { loading.value = false; return }
  try {
    const saved = JSON.parse(localStorage.getItem('TaskInfo') || '{}')
    const id = typeof route.query.job_id === 'string' ? route.query.job_id : saved.job_id
    if (!id) throw new Error('缺少任务信息，请从飞行任务列表重新进入')
    const response = await getTaskResultTypeApi(id)
    if (current !== generation) return
    if (response.code !== 0) throw new Error('无法确认任务类型，请重试')
    construction.value = isConstructionTask({ plan_type: response.data })
    // The existing result component still reads TaskInfo. Never show another task's cached data.
    if (!construction.value && saved.job_id !== id) throw new Error('任务上下文已变化，请从飞行任务列表重新进入')
    jobId.value = id
  } catch (cause) {
    if (current === generation) error.value = cause instanceof Error ? cause.message : '读取任务失败'
  } finally { if (current === generation) loading.value = false }
}
watch(() => route.fullPath, load, { immediate: true })
</script>

<style scoped>
.result-entry-status { padding: 32px; color: #d7e7ff; }
</style>
