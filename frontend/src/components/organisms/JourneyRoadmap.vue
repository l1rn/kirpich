<script setup lang="ts">
import RoadmapStep from '../molecules/RoadmapStep.vue'
import { useJourney } from '@/composables/useJourney'

const { steps } = useJourney()
</script>

<template>
  <ol class="roadmap">
     <RoadmapStep v-for="(s, i) in steps" :key="s.title" :step="s" :index="i" />
  </ol>
</template>

<style scoped>
.roadmap { list-style: none; padding: 20px; display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-lg); }
@media (max-width: 720px) { .roadmap { grid-template-columns: 1fr; gap: 20px; } }
</style>