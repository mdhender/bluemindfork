<template>
    <div
        class="bm-form-input position-relative"
        :class="{
            underline: variant === 'underline',
            ['bm-form-input-' + size]: true,
            'is-invalid': state === false,
            'is-valid': state === true,
            hover: hovered,
            focus: focused,
            disabled: disabled
        }"
    >
        <b-form-input
            ref="input"
            v-bind="[$props, $attrs]"
            :class="{
                'with-icon': hasIcon,
                'with-reset': resettable,
                'icon-left': leftIcon
            }"
            v-on="$listeners"
            @mouseover="hovered = true"
            @mouseleave="hovered = false"
            @focus="focused = true"
            @blur="focused = false"
        />
        <bm-button-close
            v-if="displayReset"
            size="sm"
            class="reset-btn position-absolute"
            :class="{ 'icon-left': leftIcon }"
            :aria-label="$t('styleguide.input.clear')"
            :title="$t('styleguide.input.clear')"
            :disabled="disabled"
            @click.stop="resetInput"
        />
        <div v-else-if="hasIcon" class="icon-wrapper position-absolute" :class="{ 'icon-left': leftIcon }">
            <bm-icon-button
                v-if="actionableIcon"
                variant="compact"
                class="actionable-icon"
                :icon="icon"
                @click="$emit('icon-click')"
            />
            <bm-icon
                v-else-if="state === false && !focused"
                class="text-danger state-icon"
                icon="exclamation-circle-fill"
            />
            <bm-icon v-else-if="state === true && !focused" class="text-success state-icon" icon="check-circle" />
            <bm-icon v-else-if="icon" :icon="icon" class="ornament-icon" @click.stop />
        </div>
    </div>
</template>

<script>
import { BFormInput } from "bootstrap-vue";
import BmIconButton from "../buttons/BmIconButton";
import BmButtonClose from "../buttons/BmButtonClose";
import BmIcon from "../BmIcon";

export default {
    name: "BmFormInput",
    components: {
        BFormInput,
        BmIconButton,
        BmButtonClose,
        BmIcon
    },
    extends: BFormInput,
    inheritAttrs: false,
    props: {
        variant: {
            type: String,
            default: "outline",
            validator: function (value) {
                return ["outline", "underline"].includes(value);
            }
        },
        size: {
            type: String,
            default: "md",
            validator: function (value) {
                return ["sm", "md"].includes(value);
            }
        },
        icon: {
            type: String,
            default: undefined
        },
        actionableIcon: {
            type: Boolean,
            default: false
        },
        resettable: {
            type: Boolean,
            default: false
        },
        leftIcon: {
            type: Boolean,
            default: false
        },
        disabled: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            hovered: false,
            focused: false
        };
    },
    computed: {
        displayReset() {
            return this.value && this.resettable && !this.disabled;
        },
        hasIcon() {
            if (this.icon) {
                return true;
            }
            if (this.state === true || this.state === false) {
                return !this.focused && !this.disabled;
            }
            return false;
        }
    },
    methods: {
        resetInput() {
            this.$emit("reset");
            this.focus();
        },
        focus() {
            this.$refs["input"].focus();
        },
        setSelectionRange(start, end) {
            start = start || 0;
            end = end || this.$refs.input.value.length;
            this.$refs.input.$el.setSelectionRange(start, end);
        }
    }
};
</script>

<style lang="scss">
@use "sass:map";
@import "../../css/_variables.scss";
@import "../../css/_type.scss";

// Component size

.bm-form-input-md {
    height: $input-height;
}
.bm-form-input-sm {
    height: $input-height-sm;
}

// Core input border

.bm-form-input.underline .form-control {
    border-radius: 0;
    border-top-color: transparent !important; // keep it to simplify vertical padding management
    border-left: none;
    border-right: none;
}

.bm-form-input {
    &:focus-within,
    &:focus,
    &.focus {
        .form-control {
            border-width: 2 * $input-border-width;
        }
    }
}

// Paddings

$items-info: (
    "icon": (
        "width": map-get($icon-sizes, "md"),
        "selector": ".icon-wrapper"
    ),
    "reset": (
        "width": map-get($btn-close-sizes, "sm"),
        "selector": ".reset-btn"
    )
);

@mixin generate-paddings-for-root($underline, $padding-x) {
    @if $underline {
        .bm-form-input.underline .form-control {
            padding: 0 $padding-x !important;
        }
    } @else {
        .bm-form-input:not(.underline) {
            .form-control {
                padding: 0 $padding-x !important;
            }
            &:focus-within,
            &:focus,
            &.focus {
                .form-control {
                    // compensate padding for thicker lateral border:
                    padding: 0 calc(#{$padding-x} - #{$input-border-width}) !important;
                }
            }
        }
    }
}

@mixin padding-in-form-control($value) {
    &.icon-left {
        padding-left: $value !important;
    }
    &:not(.icon-left) {
        padding-right: $value !important;
    }
}

@mixin generate-paddings-for($item, $underline, $padding-x, $spacing-x) {
    $offset: $padding-x + map-get($items-info, $item, "width") + $spacing-x;
    $root-selector: ".bm-form-input" + if($underline, ".underline", ":not(.underline)");
    $control-selector: ".form-control.with-" + $item;
    #{$root-selector} #{$control-selector} {
        @include padding-in-form-control($offset);
        @if not $underline {
            &:focus-within,
            &:focus,
            &.focus {
                // compensate padding for thicker lateral border:
                @include padding-in-form-control(calc(#{$offset} - #{$input-border-width}));
            }
        }
    }
}

@mixin generate-variant-paddings($underline, $padding-x, $spacing-x) {
    @include generate-paddings-for-root($underline, $padding-x);
    @each $item in "icon", "reset" {
        @include generate-paddings-for($item, $underline, $padding-x, $spacing-x);
    }
}

@include generate-variant-paddings($underline: false, $padding-x: $sp-5, $spacing-x: $sp-4);
@include generate-variant-paddings($underline: true, $padding-x: $sp-4, $spacing-x: $sp-4);

// Icon and reset button padding and margin

.bm-form-input .icon-wrapper {
    display: flex;
    align-items: center;
    gap: $sp-3;
}

@each $selector, $margin-x in (":not(.underline)": $sp-5, ".underline": $sp-4) {
    .bm-form-input#{$selector} {
        .icon-wrapper,
        .reset-btn {
            top: 0;
            bottom: 0;
            &:not(.icon-left) {
                left: auto;
                right: $margin-x;
                margin-left: $margin-x;
                margin-right: 0;
            }
            &.icon-left {
                left: $margin-x;
                right: auto;
                margin-left: 0;
                margin-right: $margin-x;
            }
        }
    }
}

// Cursor

.bm-form-input {
    .ornament-icon,
    .state-icon {
        cursor: text;
    }
}

// Colors

@mixin bm-form-input-colors(
    $stroke,
    $hover-stroke,
    $focus-stroke: $secondary-fg,
    $icon: $stroke,
    $hover-icon: $hover-stroke,
    $focus-icon: $focus-stroke
) {
    .form-control {
        background-color: $surface-bg;
        color: $neutral-fg;
        border-color: $stroke;
        background-image: none !important;
        box-shadow: none !important;
        &::placeholder {
            color: $neutral-fg-lo1;
        }
    }
    .ornament-icon {
        color: $icon;
    }

    &:hover,
    &.hover {
        .form-control {
            color: $neutral-fg-hi1;
            border-color: $hover-stroke;
        }
        .ornament-icon {
            color: $hover-icon;
        }
    }

    &:focus-within,
    &:focus,
    &.focus {
        .form-control {
            color: $neutral-fg-hi1;
            border-color: $focus-stroke;
        }
        .ornament-icon {
            color: $focus-icon;
        }
    }

    &.disabled {
        .form-control {
            color: $neutral-fg-lo1;
            border-color: $neutral-fg-disabled;
            &::placeholder {
                color: $neutral-fg-lo2;
            }
        }
        &:not(.underline) .form-control {
            background-color: $neutral-bg-lo1;
        }
        &.underline .form-control {
            border-color: transparent;
        }
        .ornament-icon {
            color: $neutral-fg-disabled;
        }
    }
}

.bm-form-input {
    &:not(.underline) {
        @include bm-form-input-colors($neutral-fg, $neutral-fg-hi1);
    }
    &.underline {
        @include bm-form-input-colors($neutral-fg-lo2, $neutral-fg, $icon: $neutral-fg, $hover-icon: $neutral-fg-hi1);
    }
}

.bm-form-input.is-invalid {
    @include bm-form-input-colors($danger-fg, $danger-fg-hi1, $danger-fg-hi1);
}

.bm-form-input.is-valid {
    @include bm-form-input-colors($success-fg, $success-fg-hi1);
}

// Form group

.form-group label {
    @extend %regular;
    text-align: start;
    margin-bottom: $sp-2;
}

.bm-form-input {
    & ~ small,
    & ~ .invalid-feedback,
    & ~ .valid-feedback {
        @extend %caption;
        text-align: start;
        margin-top: $sp-3;
    }
}
</style>
