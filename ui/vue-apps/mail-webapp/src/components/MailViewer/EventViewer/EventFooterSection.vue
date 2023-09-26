<script setup>
import { computed, ref } from "vue";
import { BmButtonExpand } from "@bluemind/ui-components";

const props = defineProps({
    label: { type: String, required: true },
    entries: { type: Array, default: () => [] }
});

const expanded = ref(false);
</script>

<template>
    <div class="event-footer-section">
        <div class="event-footer-section-header">
            <bm-button-expand :expanded="expanded" @click.prevent="expanded = !expanded" />
            <span class="font-weight-bold">
                {{ label }}
            </span>
        </div>
        <div v-if="expanded" class="event-footer-section-body">
            <slot>
                <div v-for="(entry, index) in entries" :key="index" class="event-footer-entry">
                    <span class="text-truncate">
                        <span class="font-weight-bold">{{ entry.name }}</span> &lt;{{ entry.text }}&gt;
                    </span>
                    <span v-if="entry.detail" class="font-weight-bold">({{ entry.detail }})</span>
                </div>
            </slot>
        </div>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-footer-section {
    display: flex;
    flex-direction: column;
    gap: $sp-4;

    .event-footer-section-header {
        display: flex;
        align-items: center;
    }
    .event-footer-section-body {
        display: flex;
        flex-direction: column;
        gap: $sp-3;
        padding: 0 0 $sp-2 $sp-6;

        .event-footer-entry {
            margin-left: $sp-2;
            display: flex;
            color: $neutral-fg;
            gap: $sp-4;
            line-height: $line-height-sm;
        }
    }
}
</style>
