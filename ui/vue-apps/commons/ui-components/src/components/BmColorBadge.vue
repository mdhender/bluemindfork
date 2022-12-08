<template>
    <svg
        version="1.1"
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 30 30"
        aria-hidden="true"
        role="img"
        class="bm-color-badge"
        :class="{ 'color-badge-lg': size === 'lg' }"
        @click="$emit('click', value)"
    >
        <circle
            cx="15"
            cy="15"
            r="11"
            :class="{ selected, border: isTransparent, dashed: transparentVariant === 'dashed-border' }"
            :fill="value"
        />
        <line
            v-if="isTransparent && transparentVariant === 'cross-line'"
            stroke="red"
            stroke-width="2"
            x1="7"
            y1="7"
            x2="23"
            y2="23"
        />
    </svg>
</template>

<script>
export default {
    name: "BmColorBadge",
    props: {
        value: {
            type: String,
            required: true
        },
        selected: {
            type: Boolean,
            default: false
        },
        size: {
            type: String,
            default: "md",
            validator: function (value) {
                return ["md", "lg"].includes(value);
            }
        },
        transparentVariant: {
            type: String,
            default: "cross-line",
            validator: function (value) {
                return ["cross-line", "dashed-border"].includes(value);
            }
        }
    },
    computed: {
        isTransparent() {
            return this.value === "transparent";
        }
    }
};
</script>

<style lang="scss">
@import "../css/_variables";

.bm-color-badge {
    width: $color-badge-size;
    height: $color-badge-size;
    flex: none;

    &.color-badge-lg {
        width: $color-badge-size-lg;
        height: $color-badge-size-lg;
    }

    circle {
        &.selected {
            stroke: $secondary-fg;
            stroke-width: 3px;
        }
        &.border {
            stroke: $neutral-fg-lo3;
            stroke-width: 1px;
            &.dashed {
                stroke: $neutral-fg-lo2;
                stroke-width: 2px;
                stroke-dasharray: 6 4;
            }
        }
    }
}
</style>
