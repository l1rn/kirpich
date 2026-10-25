<template>
    <dialog ref="dlg" @close="emit('close')" @click.self="emit('close')">
        <div class="body"><slot /></div>
    </dialog>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue';

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ close: [] }>();
const dlg = ref<HTMLDialogElement>();
watch(
    () => props.open, 
    (o) => (o ? dlg.value?.showModal() : dlg.value?.close())
);
</script>

<style lang="scss" scoped>
dialog { 
    border: 0;
    outline: 0;
    padding: 0; 
    border-radius: var(--r-lg); 
    background: var(--surface); 
    margin: auto;
}

dialog::backdrop { 
    background: rgba(18, 22, 43, 0.55); 
}
.body { 
    padding: 24px; 
    width: min(380px, 92vw); 
}
</style>