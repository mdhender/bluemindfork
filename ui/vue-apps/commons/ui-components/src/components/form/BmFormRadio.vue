<template>
    <div :class="{ 'with-img': withImage }" class="bm-form-radio">
        <label
            v-if="withImage"
            :for="_uid + JSON.stringify(value)"
            class="bm-form-radio-img-label"
            :class="{ 'bm-form-radio-checked': isChecked }"
        >
            <slot name="img" />
        </label>
        <b-form-radio
            :id="_uid + JSON.stringify(value)"
            inline
            :class="{ 'd-flex flex-fill justify-content-center': withImage }"
            v-bind="$props"
            v-on="$listeners"
        >
            <template v-if="!withImage"><slot /></template>
        </b-form-radio>
        <label v-if="withImage" :for="_uid + JSON.stringify(value)"><slot /></label>
    </div>
</template>

<script>
import { BFormRadio } from "bootstrap-vue";

export default {
    name: "BmFormRadio",
    components: { BFormRadio },
    extends: BFormRadio,
    computed: {
        withImage() {
            return !!this.$slots.img;
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "../../css/_mixins.scss";
@import "../../css/_variables.scss";
@import "../../css/_type.scss";

$radio-button-offset: math.div($line-height-regular - $custom-radio-size, 2);

.bm-form-radio {
    & * {
        cursor: pointer !important;
        @extend %regular;
    }

    & > .custom-radio > .custom-control-input {
        width: $custom-radio-size;
        height: $custom-radio-size;
    }

    .custom-control-label {
        &::before {
            // radio button body
            width: $custom-radio-size;
            height: $custom-radio-size;
            top: $radio-button-offset;
            left: -$custom-control-indicator-size - $custom-control-gutter + $radio-button-offset;
            border-width: 2px;
        }
    }

    &:hover .custom-control-input:not(:disabled) {
        & ~ .custom-control-label::before {
            border-color: $neutral-fg-hi1;
        }

        &:checked ~ .custom-control-label::before {
            border-color: $secondary-fg-hi1;
            background-color: $fill-secondary-bg-hi1;
        }
    }

    .custom-radio .custom-control-input {
        & ~ .custom-control-label::before {
            border-color: $input-border-color;
        }

        &:checked ~ .custom-control-label::before {
            border-color: $fill-secondary-bg;
        }

        &:focus {
            & ~ .custom-control-label::before {
                box-shadow: none;
                border-color: $neutral-fg-hi1;
                @include default-focus($fill-secondary-bg-hi1, true);
            }
            &:checked ~ .custom-control-label::before,
            &:indeterminate ~ .custom-control-label::before {
                border-color: $secondary-fg-hi1;
                background-color: $fill-secondary-bg-hi1;
            }
        }
    }
}

.bm-form-radio.with-img {
    $width: base-px-to-rem(154);

    .custom-radio {
        margin: 0;
        padding-bottom: base-px-to-rem(5);
        padding-left: 0;
        padding-right: 0;
    }

    label:not(.custom-control-label) {
        width: $width;
        text-align: center;
    }

    .bm-form-radio-img-label {
        border: transparent 2px solid;
        margin-bottom: base-px-to-rem(17);
        &.bm-form-radio-checked {
            border: $secondary-fg 2px solid;
        }
    }
    .bm-form-radio-img-label svg {
        width: 100%;
    }

    .custom-control-label {
        &::before {
            // radio button body
            left: -$custom-radio-size + $radio-button-offset;
            right: 0;
        }
        &::after {
            // radio button indicator (inner dot)
            left: -$custom-radio-size;
            right: 0;
        }
    }

    .custom-control-input {
        position: unset;
    }
}
</style>
