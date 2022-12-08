<template>
    <div class="bm-form-input-number d-flex">
        <bm-button
            :disabled="disabled || readOnly"
            :aria-label="labelDecrement_"
            class="btn-minus"
            variant="outline"
            size="lg"
            icon="minus"
            @click="decrement"
        />
        <bm-form-input type="number" v-bind="[$attrs, $props]" class="flex-fill" @update="onInput" />
        <bm-button
            :disabled="disabled || readOnly"
            :aria-label="labelIncrement_"
            class="btn-plus"
            variant="outline"
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
        wrap: {
            type: Boolean,
            default: false
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
                } else if (this.wrap && newValue < min) {
                    this.$emit("input", this.max);
                } else if (this.wrap && newValue > max) {
                    this.$emit("input", this.min);
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
    }

    .btn-minus,
    .btn-plus {
        flex: none;
        width: $input-height;
        gap: 0 !important;
        .bm-icon {
            position: absolute;
            $icon-size: map-get($icon-sizes, "md");
            $icon-offset: math.div(-($icon-size), 2);
            top: $icon-offset;
            left: $icon-offset;
        }
    }
    .btn-minus {
        border-right: none !important;
    }
    .btn-plus {
        border-left: none !important;
    }
}
</style>
