<template>
    <div class="bm-form-input-number d-flex" :class="{ underline: variant === 'underline' }">
        <bm-button
            v-if="showButtons"
            :disabled="disabled || readOnly"
            :aria-label="labelDecrement_"
            class="btn-minus"
            :variant="btnVariant"
            size="lg"
            icon="minus"
            @click="$emit()"
        />
        <bm-form-input type="number" v-bind="[$attrs, $props]" class="flex-fill" @update="onInput" />
        <bm-button
            v-if="showButtons"
            :disabled="disabled || readOnly"
            :aria-label="labelIncrement_"
            class="btn-plus"
            :variant="btnVariant"
            size="lg"
            icon="plus"
            @click="increment"
        />
    </div>
</template>

<script>
import BmButton from "../buttons/BmButton";
import BmFormInput from "./BmFormInput";

export default {
    name: "BmFormInputNumber",
    components: { BmButton, BmFormInput },
    props: {
        variant: {
            type: String,
            default: "outline",
            validator: function (value) {
                return ["outline", "underline"].includes(value);
            }
        },
        showButtons: {
            type: Boolean,
            default: false
        },
        value: {
            type: String,
            default: "0"
        },
        step: {
            type: Number,
            default: 1
        },
        disabled: {
            type: Boolean,
            default: false
        },
        labelDecrement: {
            type: String,
            default: ""
        },
        labelIncrement: {
            type: String,
            default: ""
        },
        min: {
            type: String,
            default: "-Infinity"
        },
        max: {
            type: String,
            default: "+Infinity"
        },
        readOnly: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            labelDecrement_: this.labelDecrement,
            labelIncrement_: this.labelIncrement
        };
    },
    computed: {
        btnVariant() {
            return this.variant === "outline" ? "outline" : "text";
        }
    },
    created: function () {
        if (!this.labelDecrement) {
            this.labelDecrement_ = this.$t("styleguide.input.number.decrement");
        }
        if (!this.labelIncrement_) {
            this.labelIncrement_ = this.$t("styleguide.input.number.increment");
        }
    },
    methods: {
        decrement() {
            this.onInput(parseInt(this.value) - this.step);
        },
        increment() {
            this.onInput(parseInt(this.value) + this.step);
        },
        onInput(newValue) {
            if (newValue !== "" && !this.readOnly) {
                const min = parseFloat(this.min);
                const max = parseFloat(this.max);
                if (newValue >= min && newValue <= max) {
                    this.$emit("input", newValue.toString());
                } else {
                    // FIXME: update BmFormInput state with this.value, but how.. ?
                }
            }
        }
    }
};
</script>

<style lang="scss">
@use "sass:map";
@use "sass:math";
@import "../../css/_variables.scss";

.bm-form-input-number {
    .bm-form-input input[type="number"] {
        text-align: center;
        height: 100%;
        -moz-appearance: textfield;
        &::-webkit-inner-spin-button {
            -webkit-appearance: none;
        }

        &[readonly]:not(.disabled):not(:disabled) {
            background: none;
        }
    }

    .btn {
        flex: none;
        width: $input-height;
        gap: 0 !important;
        z-index: 1;
    }

    &:not(.underline) {
        .btn:not(.disabled):not(:disabled) {
            border-color: $neutral-fg-lo1 !important;
        }
        .btn-minus {
            border-right: none !important;
        }
        .btn-plus {
            border-left: none !important;
        }
    }

    &.underline {
        position: relative;

        .btn {
            position: absolute;
            outline-offset: $icon-btn-height - $icon-btn-height-lg;
            &::before {
                display: none !important;
            }
        }
        .btn-minus {
            left: 0;
        }
        .btn-plus {
            right: 0;
        }

        &:hover .bm-form-input > .form-control {
            border-color: $neutral-fg;
        }

        &:focus-within .bm-form-input > .form-control {
            border-width: 2 * $input-border-width;
            border-color: $secondary-fg;
        }
    }
}
</style>
