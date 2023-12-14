<script setup>
import { ref } from "vue";
import { BmButtonExpand } from "@bluemind/ui-components";

const props = defineProps({
    label: { type: String, required: true },
    entries: { type: Array, default: () => [] },
    expandable: { type: Boolean, default: true }
});

const expanded = ref(false);
function expand() {
    if (props.expandable) {
        expanded.value = !expanded.value;
    }
}
</script>

<template>
    <div class="event-footer-section" :class="{ 'section-expanded': expanded }">
        <div class="event-footer-section-header" @click.prevent="expand">
            <bm-button-expand :expanded="expanded" size="sm" :disabled="!expandable" />
            <span class="bold">
                {{ label }}
            </span>
        </div>
        <div v-if="expanded" class="event-footer-section-body">
            <slot>
                <div v-for="(entry, index) in entries" :key="index" class="event-footer-entry">
                    <span class="text-truncate">
                        <span class="bold">{{ entry.name }}</span> &lt;{{ entry.text }}&gt;
                    </span>
                </div>
            </slot>
        </div>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-footer-section {
    display: flex;
    flex-direction: column;
    gap: $sp-4;

    .event-footer-section-header {
        display: flex;
        align-items: center;
        gap: $sp-2;
        @include until-lg {
            gap: $sp-3;
            padding-left: $sp-3 + $sp-2;
        }
        cursor: pointer;
        color: $neutral-fg;
        &:hover {
            color: $neutral-fg-hi1;
        }
    }
    &.section-expanded .event-footer-section-header {
        color: $neutral-fg-hi1;
    }

    .event-footer-section-body {
        display: flex;
        flex-direction: column;
        gap: $sp-3;
        padding-bottom: $sp-2;
        padding-left: $sp-5;
        @include from-lg {
            padding-left: $icon-btn-width-compact-sm + $sp-2;
        }

        .event-footer-entry {
            display: flex;
            align-items: baseline;
            color: $neutral-fg;
            gap: $sp-3;
        }
    }
}
</style>
