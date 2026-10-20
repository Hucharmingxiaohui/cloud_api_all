<template>
  <div class="frog-jump-landing">
    <div>起飞机场：{{ takeoffDockSn }}</div>
    <el-alert v-if="warning" :title="warning" type="warning" :closable="false" show-icon />
    <div v-if="recommended" class="recommendation-card">
      <strong>推荐降落机场：{{ recommended.name }}</strong>
      <div>在线且可选、坐标已知的最近机场 · {{ formatLandingDistance(recommended.distance_meters) }}</div>
    </div>
    <el-alert v-else title="暂无有效推荐，请手动选择降落机场" type="info" :closable="false" />
    <div>距航线终点的水平直线距离</div>
    <el-table :data="candidates" max-height="300" empty-text="没有可选择的降落机场">
      <el-table-column label="选择" width="65">
        <template #default="scope">
          <el-radio :model-value="modelValue" :label="scope.row.dock_sn" :disabled="!scope.row.selectable" @update:model-value="$emit('update:modelValue', $event)">
            <span class="sr-only">{{ scope.row.name }}</span>
          </el-radio>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="机场" min-width="120" />
      <el-table-column label="距离" width="110">
        <template #default="scope">{{ formatLandingDistance(scope.row.distance_meters) }}</template>
      </el-table-column>
      <el-table-column label="状态 / 原因" min-width="170">
        <template #default="scope">
          <div>{{ scope.row.online === true ? '在线' : scope.row.online === false ? '离线' : '在线状态未知' }} · {{ scope.row.selectable ? '可选' : '不可选' }}</div>
          <div>{{ scope.row.reason }}</div>
        </template>
      </el-table-column>
    </el-table>
    <p class="safety-note">推荐仅依据水平直线距离，不保证航路安全、电量充足或设备兼容。确认后仍须通过现有任务准备阶段校验，不会自动下发。</p>
  </div>
</template>

<script lang="ts" setup>
import { computed, defineProps, defineEmits, PropType } from 'vue'
import { ElAlert, ElTable, ElTableColumn, ElRadio } from 'element-plus'
import type { LandingCandidate } from '/@/api/frogJump'
import { formatLandingDistance } from './frogJumpLanding'

const props = defineProps({
  modelValue: { type: String, default: '' },
  takeoffDockSn: { type: String, required: true },
  candidates: { type: Array as PropType<LandingCandidate[]>, required: true },
  recommendedDockSn: { type: String, default: '' },
  warning: { type: String, default: '' },
})
defineEmits(['update:modelValue'])
const recommended = computed(() => props.candidates.find(item => item.dock_sn === props.recommendedDockSn && item.selectable && item.distance_meters !== null))
</script>

<style scoped>
.frog-jump-landing { display: flex; flex-direction: column; gap: 12px; min-width: 0; }
.recommendation-card { padding: 12px; border: 1px solid #a9c8ea; border-radius: 7px; background: #f1f6fc; color: #2f6fae; }
.safety-note { margin: 0; color: #946124; font-size: 12px; line-height: 1.6; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); }
</style>
