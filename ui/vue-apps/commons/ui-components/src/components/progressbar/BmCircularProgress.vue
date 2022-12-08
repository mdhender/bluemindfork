<template>
    <svg class="bm-circular-progress" :class="variant" viewbox="0 0 100 100" :height="height || 100" :width="width">
        <circle class="background" cx="50" cy="50" r="30" :stroke-width="SIZE" fill="none" />
        <circle
            v-if="max"
            class="foreground"
            stroke-linecap="round"
            cx="50"
            cy="50"
            r="30"
            :stroke-width="SIZE"
            fill="none"
            :stroke-dasharray="dash"
            stroke-dashoffset="70"
        />
        <text v-if="showProgress" x="50" y="50" text-anchor="middle" dy="7">
            <slot>{{ computePrecision }}%</slot>
        </text>
    </svg>
</template>

<script>
import { BProgress } from "bootstrap-vue";

export default {
    name: "BmCircularProgress",
    extends: BProgress,
    data() {
        return { width: 100, SIZE: "6" };
    },
    computed: {
        computePrecision() {
            return ((this.value / this.max) * 100).toFixed(this.precision) || 0;
        },
        dash() {
            const filled = Math.ceil((210 * this.computePrecision) / 100);
            return filled + "," + (210 - filled);
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables.scss";

.bm-circular-progress {
    .background {
        stroke: $neutral-bg;
    }
    .foreground {
        stroke: $secondary-fg;
    }

    @each $variant in "danger", "warning", "success" {
        &.#{$variant} {
            .background {
                stroke: var(--#{$variant}-bg);
            }
            .foreground {
                stroke: var(--#{$variant}-fg);
            }
        }
    }
}
</style>
