<template>
    <b-form-checkbox v-bind="$props" class="bm-form-checkbox" :class="{ 'left-label': leftLabel }" v-on="$listeners">
        <slot />
    </b-form-checkbox>
</template>

<script>
import { BFormCheckbox } from "bootstrap-vue";

export default {
    name: "BmFormCheckbox",
    components: { BFormCheckbox },
    extends: BFormCheckbox,
    props: {
        leftLabel: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "../../css/_mixins.scss";
@import "../../css/_variables.scss";
@import "../../css/_type.scss";

$checkbox-offset: math.div($line-height-regular - $custom-checkbox-size, 2);

$switch-padding: math.div($custom-switch-height - $custom-switch-indicator-size, 2);
$switch-indicator-travel: $custom-switch-width - $custom-switch-indicator-size - 2 * $switch-padding;

.bm-form-checkbox {
    line-height: $line-height-sm;
}

.bm-form-checkbox * {
    cursor: pointer;
    @extend %regular;
}

.bm-form-checkbox .custom-control-input:focus {
    &:checked ~ .custom-control-label::before,
    &:indeterminate ~ .custom-control-label::before {
        background-color: $fill-secondary-bg-hi1;
        border-color: $fill-secondary-bg-hi1;
        @include default-focus($fill-secondary-bg-hi1, true);
    }

    &:checked ~ .custom-control-label::after,
    &:indeterminate ~ .custom-control-label::after {
        background: $fill-secondary-fg;
    }

    ~ .custom-control-label::before {
        border-color: $neutral-fg;
        @include default-focus($neutral-fg, true);
    }
}

.bm-form-checkbox:not(.custom-switch) {
    & > .custom-control-input {
        width: $custom-checkbox-size;
        height: $custom-checkbox-size;
    }
    .custom-control-label {
        &::before {
            // checkbox body
            width: $custom-checkbox-size;
            height: $custom-checkbox-size;
            top: $checkbox-offset;
            left: -$custom-control-indicator-size - $custom-control-gutter + $checkbox-offset;
            border-width: 2px;
            border-radius: 1px;
        }
    }
}

.bm-form-checkbox.custom-switch {
    .custom-control-label {
        &::before {
            // switch body
            top: 0;
            height: $custom-switch-height;
            border-radius: 0.5rem;
        }
        &::after {
            // switch indicator (inner dot)
            background-color: $fill-neutral-bg;
            top: $switch-padding;
            left: -$custom-switch-width - $custom-control-gutter + $switch-padding;
        }
    }

    &.left-label {
        $offset-x: $custom-switch-width + $custom-control-gutter;
        padding-right: $offset-x;
        padding-left: 0;
        .custom-control-label {
            &::before {
                // switch body
                right: -$offset-x;
                left: unset;
            }
            &::after {
                // switch indicator (inner dot)
                // positioned from right, so x axis goes from right to left
                right: -$offset-x + $switch-padding + $switch-indicator-travel;
                left: unset;
            }
        }
    }

    .custom-control-input:checked ~ .custom-control-label {
        &::after {
            transform: translateX($switch-indicator-travel);
        }
    }

    .custom-control-input {
        &:active ~ .custom-control-label::before {
            border-color: $secondary-fg;
        }
        &:active:checked ~ .custom-control-label::before {
            background-color: $fill-secondary-bg;
        }
        &:checked:hover ~ .custom-control-label::before,
        &:checked:focus ~ .custom-control-label::before {
            background-color: $fill-secondary-bg-hi1;
        }
        &:focus ~ .custom-control-label::before,
        &:hover ~ .custom-control-label::before {
            border-color: $secondary-fg-hi1;
        }
        &:focus:not(:checked) ~ .custom-control-label::before,
        &:hover:not(:checked) ~ .custom-control-label::before {
            border-color: $neutral-fg;
        }
        &:focus:not(:checked) ~ .custom-control-label::after,
        &:hover:not(:checked) ~ .custom-control-label::after {
            background-color: $neutral-fg;
        }
    }
}
</style>
