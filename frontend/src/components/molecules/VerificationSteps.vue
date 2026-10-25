<script setup lang="ts">
import { computed } from 'vue'
import { checkLabel, checkTone } from '@/utils/application'

const props = defineProps<{ bki: string; egrul: string; reasons?: string }>()
const reasonList = computed(() => props.reasons?.split('; ').filter(Boolean) ?? [])
</script>

<template>
  <div class="wrap">
    <ul class="steps">
      <li>
        <span class="dot" :class="checkTone(bki)" />
        <span>Кредитная история</span>
        <strong>{{ checkLabel(bki) }}</strong>
      </li>
      <li>
        <span class="dot" :class="checkTone(egrul)" />
        <span>Проверка в ЕГРЮЛ</span>
        <strong>{{ checkLabel(egrul) }}</strong>
      </li>
    </ul>
    <ul v-if="reasonList.length" class="reasons">
      <li v-for="r in reasonList" :key="r">{{ r }}</li>
    </ul>
  </div>
</template>

<style scoped>
.wrap { display: grid; gap: 10px; }
.steps { list-style: none; padding: 0; display: grid; gap: 8px; }
.steps li { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.steps strong { margin-left: auto; font-weight: 600; }
.dot { width: 8px; height: 8px; border-radius: 50%; }
.ok { background: var(--ok); }
.bad { background: #d33; }
.neutral { background: #c5c9dc; }
.reasons { font-size: 13px; color: #d33; padding-left: 18px; }
</style>