<template>
    <div class="bm-form-date-picker form-input" :class="{ underline: variant === 'underline', disabled }">
        <form-date-picker
            v-bind="[$attrs, $props]"
            class="with-icon icon-left"
            :class="{ 'with-reset': showReset }"
            :disabled="disabled"
            v-on="$listeners"
            @input="value_ = $event"
        />
        <bm-button-close
            v-if="showReset"
            size="sm"
            :aria-label="$t('styleguide.input.clear')"
            @click.stop="resetInput"
        />
    </div>
</template>

<script>
import { BFormDatepicker } from "bootstrap-vue";
import CalendarMixin from "../../mixins/CalendarMixin";
import BmButtonClose from "../buttons/BmButtonClose";

const FormDatePicker = {
    extends: BFormDatepicker,
    mixins: [CalendarMixin]
};
export default {
    name: "BmFormDatePicker",
    components: { FormDatePicker, BmButtonClose },
    inheritAttrs: false,
    props: {
        resettable: {
            type: Boolean,
            default: false
        },
        variant: {
            type: String,
            default: "outline",
            validator: function (value) {
                return ["outline", "underline"].includes(value);
            }
        },
        value: {
            type: [String, Date, Boolean],
            default: null
        },
        disabled: {
            type: Boolean,
            default: false
        }
    },
    data: () => ({ value_: null }),
    computed: {
        showReset() {
            return this.value_ && this.resettable && !this.disabled;
        }
    },
    watch: {
        value: {
            handler() {
                this.value_ = this.value;
            },
            immediate: true
        }
    },
    methods: {
        resetInput() {
            this.$emit("reset");
        }
    }
};
</script>

<style lang="scss">
@import "../../css/utils/variables.scss";

.bm-form-date-picker {
    // Clear button

    position: relative;

    .bm-button-close {
        position: absolute;
        top: 0;
        height: 100%;
        right: 0;
        margin-right: $sp-5;
    }

    // Inner Bootstrap date picker

    .b-form-datepicker {
        height: $input-height !important;

        label.form-control {
            display: flex;
            align-items: center;
            min-height: 0 !important;
            overflow: hidden;
            white-space: nowrap !important;
            text-overflow: ellipsis;
            padding: 0 !important;
        }
    }

    // Icon

    .b-form-datepicker > button {
        position: absolute;
        top: 50%;
        transform: translate(0, -50%);
    }

    &:not(.underline) {
        .b-form-datepicker > button {
            left: base-px-to-rem(3);
        }
        &:focus-within .b-form-datepicker > button {
            left: calc(#{base-px-to-rem(3)} - #{$input-border-width});
        }
    }

    &.underline {
        .b-form-datepicker > button {
            left: base-px-to-rem(2);
        }
    }

    // Calendar widget ("dropdown menu")

    .b-form-datepicker > .dropdown-menu {
        padding: 0 !important;
        box-shadow: $box-shadow-sm;
    }

    $offset-y: base-px-to-rem(2) + $sp-3;
    .b-form-datepicker > .dropdown-menu {
        top: $offset-y !important;
    }
    &:focus-within .b-form-datepicker > .dropdown-menu {
        top: calc(#{$offset-y} - #{$input-border-width}) !important;
    }

    &:not(.underline) {
        $offset-x: base-px-to-rem(-3);
        .b-form-datepicker > .dropdown-menu {
            left: $offset-x !important;
        }
        &:focus-within .b-form-datepicker > .dropdown-menu {
            left: calc(#{$offset-x} - #{$input-border-width}) !important;
        }
    }

    &.underline {
        $offset-x: base-px-to-rem(-2);
        .b-form-datepicker > .dropdown-menu {
            left: $offset-x !important;
        }
    }
}
</style>
