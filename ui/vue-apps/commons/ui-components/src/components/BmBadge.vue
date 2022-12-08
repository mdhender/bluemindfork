<template>
    <b-badge v-bind="$attrs" class="bm-badge d-inline-flex align-items-center" v-on="$listeners">
        <slot />
        <bm-button-close
            v-if="closeable"
            class="ml-1"
            size="xs"
            :aria-label="closeButtonAriaLabel"
            @click="$emit('close', $event)"
            @keydown.enter.stop
        />
    </b-badge>
</template>

<script>
import { BBadge } from "bootstrap-vue";
import BmButtonClose from "./buttons/BmButtonClose";

export default {
    name: "BmBadge",
    components: { BBadge, BmButtonClose },
    props: {
        closeable: {
            type: Boolean,
            default: false
        },
        closeButtonAriaLabel: {
            type: String,
            default: undefined
        }
    }
};
</script>

<style lang="scss">
@import "../css/_variables";

.bm-badge {
    background-color: $neutral-bg !important;
    color: $neutral-fg !important;

    .close {
        line-height: 0;
    }
}
</style>
