<template>
    <div
        class="bm-form-input form-input position-relative"
        :class="{
            underline: variant === 'underline',
            ['form-input-' + size]: true,
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
