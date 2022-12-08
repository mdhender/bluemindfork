<template>
    <div
        class="bm-chip"
        v-bind="$attrs"
        :class="{
            'has-left-part': hasLeftPart,
            'is-selected': selected,
            ['bm-chip-' + size]: true
        }"
        v-on="$listeners"
        @keydown.delete="closeable && $emit('remove')"
    >
        <div class="chip-part chip-left-part"><slot name="left-part" /></div>
        <div class="chip-part chip-main-part">
            <div class="text-truncate" :class="variant === 'caption' ? 'caption-bold' : 'regular'">
                <slot />
            </div>
            <bm-button-close
                v-if="closeable"
                size="xs"
                :aria-label="$t('styleguide.chip.close')"
                tabindex="-1"
                @click="$emit('remove')"
            />
        </div>
    </div>
</template>

<script>
import BmButtonClose from "./buttons/BmButtonClose";

export default {
    name: "BmChip",
    components: { BmButtonClose },
    props: {
        closeable: {
            type: Boolean,
            default: false
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
        variant: {
            type: String,
            default: "regular",
            validator: function (value) {
                return ["caption", "regular"].includes(value);
            }
        }
    },
    computed: {
        hasLeftPart() {
            return !!this.$slots["left-part"];
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "../css/_variables";

$chip-border-radius: math.div($chip-height-lg, 2);

.bm-chip {
    display: inline-flex;
    align-items: center;
    min-width: 0;
    flex: 1;

    .chip-main-part {
        min-width: 0;
        flex: 1;

        display: flex;
        align-items: center;
        gap: $sp-4;
        overflow: hidden;
        padding: 0 $sp-5;
        border-radius: $chip-border-radius;
    }

    &.bm-chip-md .chip-main-part {
        height: $chip-height;
    }
    &.bm-chip-lg .chip-main-part {
        height: $chip-height-lg;
    }
}

.bm-chip.has-left-part {
    // transfer left styling of chip main part to its left part slot
    .chip-main-part {
        padding-left: 0;
        border-top-left-radius: 0;
        border-bottom-left-radius: 0;
    }
    .chip-left-part {
        border-top-left-radius: $chip-border-radius;
        border-bottom-left-radius: $chip-border-radius;
    }
}

.bm-chip {
    .chip-part {
        color: $neutral-fg;
        background-color: $neutral-bg;
    }
    &.is-selected .chip-part {
        background-color: $secondary-bg;
    }
}

.bm-chip.hover,
.bm-chip:hover {
    .chip-part {
        color: $neutral-fg-hi1;
        background-color: $neutral-bg-hi1;
    }
    &.is-selected .chip-part {
        background-color: $secondary-bg-hi1;
    }
}
</style>
