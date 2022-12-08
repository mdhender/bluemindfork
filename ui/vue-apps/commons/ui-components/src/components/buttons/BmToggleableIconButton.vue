<template>
    <bm-icon-button
        class="bm-toggleable-icon-button"
        :pressed="pressed"
        :variant="variant === 'on-fill-primary' ? 'compact-on-fill-primary' : 'compact'"
        v-bind="[$attrs, $props]"
        v-on="$listeners"
    >
        <slot />
    </bm-icon-button>
</template>

<script>
import BmIconButton from "./BmIconButton";

export default {
    name: "BmToggleableIconButton",
    components: { BmIconButton },
    props: {
        variant: {
            type: String,
            default: ""
        },
        pressed: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables";
@import "../../css/mixins/_buttons";

.bm-toggleable-icon-button.bm-icon-button {
    &.active:not(.on-fill-primary) {
        &:not(.disabled),
        &:not(:disabled) {
            background-color: $neutral-bg-hi1;
        }

        &.disabled,
        &:disabled {
            background-color: $neutral-bg-lo1;
        }
    }

    &.active.on-fill-primary {
        &:not(.disabled),
        &:not(:disabled) {
            background-color: $fill-primary-bg-hi1;
        }

        &.disabled,
        &:disabled {
            background-color: $fill-primary-bg-lo1;
        }
    }
}

@include set-button-icon-sizes(
    ".bm-icon-button.compact.bm-toggleable-icon-button",
    (
        "sm": "sm",
        "md": "sm",
        "lg": "md"
    )
);

@include set-button-icon-sizes(
    ".bm-icon-button.compact.bm-toggleable-icon-button:not(.active):not(:disabled):hover",
    (
        "sm": "md",
        "md": "md",
        "lg": "lg"
    )
);

@include set-button-icon-sizes(
    ".bm-icon-button.compact.bm-toggleable-icon-button.active",
    (
        "sm": "lg",
        "md": "lg",
        "lg": "xl"
    )
);
</style>
