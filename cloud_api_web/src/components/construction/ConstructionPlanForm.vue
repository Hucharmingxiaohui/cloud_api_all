<template>
  <el-dialog :model-value="true" title="新建工地巡检计划" width="620px" append-to-body :close-on-click-modal="false" :close-on-press-escape="false" :show-close="false">
    <el-form label-width="110px" :disabled="saving">
      <el-form-item :label="overallId ? '总体航线' : '巡检区域'"><strong>{{ area.name }} · v{{ area.version }}</strong></el-form-item>
      <el-form-item label="计划名称" required><el-input v-model="form.name" maxlength="50" placeholder="例如：一号区域每日巡检" data-testid="plan-name" /></el-form-item>
      <el-form-item label="巡检航线" required>
        <el-select v-model="form.file_id" placeholder="请选择当前区域的有效航线" filterable style="width: 100%" @change="emit('preview', $event)">
          <el-option v-for="route in routes" :key="route.wayline_id" :value="route.wayline_id" :label="route.name" :disabled="route.status !== 'VALID'" />
        </el-select>
        <p class="form-note">{{ overallId ? '按当前总体航线的区域与航点绑定创建一个飞行任务。' : '仅显示本区域绑定航线，失效航线须先在施工区域页重新校验。' }}</p>
      </el-form-item>
      <el-form-item label="执行机巢" required><el-select v-model="form.dock_sn" filterable placeholder="请选择执行机巢" style="width: 100%" @change="emit('dock', $event)"><el-option v-for="dock in devices" :key="dock.device_sn" :value="dock.device_sn" :label="dock.nickname || dock.device_name || dock.device_sn" /></el-select></el-form-item>
      <el-form-item label="执行方式"><el-radio-group v-model="form.task_type"><el-radio :label="0">立即任务</el-radio><el-radio :label="1">定时任务</el-radio></el-radio-group></el-form-item>
      <el-form-item v-if="form.task_type === 1" label="执行时间" required><el-date-picker v-model="form.begin_time" type="datetime" value-format="x" placeholder="选择执行时间" /></el-form-item>
      <el-form-item label="返航高度" required><el-input-number v-model="form.rth_altitude" :min="30" :max="1500" :precision="0" /> 米</el-form-item>
      <el-form-item label="失联动作"><el-select v-model="form.out_of_control"><el-option :value="0" label="返航" /><el-option :value="1" label="悬停" /><el-option :value="2" label="降落" /></el-select></el-form-item>
    </el-form>
    <p class="form-note">保存后进入计划列表；点击“下发任务”才会提交执行。拍摄动作和巡检高度沿用航线设置。</p>
    <p v-if="error" class="form-error" role="alert">{{ error }}</p>
    <template #footer><el-button :disabled="saving" @click="cancel">取消</el-button><el-button type="primary" :loading="saving" :disabled="!routes.some(r => r.status === 'VALID') || !devices.length" @click="save">保存计划</el-button></template>
  </el-dialog>
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { createConstructionPlan, createConstructionOverallPlan } from '/@/api/construction'
import type { ConstructionAreaRecord, ConstructionWayline } from '/@/api/construction'
import { uuidv4 } from '/@/utils/uuid'
/* global defineProps, defineEmits */
const props = defineProps<{ area: ConstructionAreaRecord; routes: ConstructionWayline[]; devices: Array<{ device_sn: string; nickname?: string; device_name?: string }>; overallId?: string }>()
const emit = defineEmits(['close', 'saved', 'busy', 'preview', 'dock'])
const form = reactive({ name: '', file_id: '', dock_sn: '', task_type: 0, begin_time: '', rth_altitude: 100, out_of_control: 0 })
if (props.overallId) { form.file_id = props.routes[0]?.wayline_id || ''; form.dock_sn = props.devices[0]?.device_sn || '' }
const initial = JSON.stringify(form)
const saving = ref(false); const error = ref('')
let retryBody = ''; let requestId = ''
async function cancel () {
  if (JSON.stringify(form) !== initial) { try { await ElMessageBox.confirm('放弃未保存的计划？', '取消新建') } catch { return } }
  emit('close')
}
async function save () {
  if (saving.value) return
  error.value = ''
  if (!form.name.trim() || !form.dock_sn || !props.routes.some(r => r.wayline_id === form.file_id && r.status === 'VALID')) { error.value = '请填写计划名称、有效航线和执行机巢'; return }
  if (form.task_type === 1 && (!form.begin_time || Number(form.begin_time) <= Date.now() + 30000)) { error.value = '执行时间须至少晚于当前时间30秒'; return }
  const body = { ...form, name: form.name.trim(), begin_time: form.task_type === 1 ? Number(form.begin_time) : undefined, expected_version: props.area.version }
  const serialized = JSON.stringify(body)
  if (serialized !== retryBody) { retryBody = serialized; requestId = uuidv4() }
  saving.value = true; emit('busy', true)
  try { const row = await (props.overallId ? createConstructionOverallPlan(props.overallId, { ...body, request_id: requestId }) : createConstructionPlan(props.area.id, { ...body, request_id: requestId })); emit('saved', row) } catch (cause) { error.value = cause instanceof Error ? cause.message : '保存失败，请重试' } finally { saving.value = false; emit('busy', false) }
}
</script>
<style scoped>
.form-note { color: #8c9fb7; font-size: 12px; line-height: 1.7; margin: 8px 0 0; }
.form-error { color: #f56c6c; }
</style>
