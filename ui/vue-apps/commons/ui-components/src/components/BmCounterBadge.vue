<template>
    <div
        class="bm-counter-badge"
        :class="{
            'counter-badge-light': variant === 'light',
            'counter-badge-active': active,
            'count-below-100': count < 100,
            'count-overflows': countOverflows
        }"
    >
        {{ boundedCount }}
    </div>
</template>

<script>
const countLimit = 999;

export default {
    name: "BmCounterBadge",
    props: {
        count: {
            type: Number,
            required: true
        },
        variant: {
            type: String,
            default: "default",
            validator: function (value) {
                return ["default", "light"].includes(value);
            }
        },
        active: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        countOverflows() {
            return this.count > countLimit;
        },
        boundedCount() {
            return this.countOverflows ? "+" + countLimit : this.count.toString();
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "../css/_variables";

.bm-counter-badge {
    display: inline-flex;
    justify-content: center;
    flex: none;

    border-radius: math.div($counter-badge-height, 2);
    height: $counter-badge-height;
    width: $counter-badge-width;
    font-size: base-px-to-rem(12);
    line-height: base-px-to-rem(12);
    padding-top: base-px-to-rem(4.25);
    font-weight: $font-weight-bold;
    letter-spacing: -0.02em;

    &.count-below-100 {
        width: $counter-badge-width-below-100;
        letter-spacing: 0;
    }

    &.count-overflows {
        font-size: base-px-to-rem(11);
        letter-spacing: -0.04em;
    }
}

.bm-counter-badge {
    background-color: $fill-neutral-bg;
    color: $fill-neutral-fg;
    &.counter-badge-active {
        background-color: $fill-secondary-bg;
        color: $fill-secondary-fg;
    }
}

.bm-counter-badge.counter-badge-light {
    background-color: $neutral-bg;
    color: $neutral-fg;
    &.counter-badge-active {
        background-color: $secondary-bg;
        color: $secondary-fg;
    }
}

.bm-counter-badge-container:hover,
.bm-counter-badge-container.hover {
    .bm-counter-badge {
        background-color: $fill-neutral-bg-hi1;
        &.counter-badge-active {
            background-color: $fill-secondary-bg-hi1;
        }
    }

    .bm-counter-badge.counter-badge-light {
        background-color: $neutral-bg-hi1;
        color: $neutral-fg-hi1;
        &.counter-badge-active {
            background-color: $secondary-bg-hi1;
            color: $secondary-fg-hi1;
        }
    }
}

.bm-counter-badge-container[disabled],
.bm-counter-badge-container.disabled {
    .bm-counter-badge {
        background-color: $fill-neutral-bg-disabled;
    }

    .bm-counter-badge.counter-badge-light {
        background-color: $neutral-bg-lo1;
        color: $neutral-fg-disabled;
    }
}
</style>
