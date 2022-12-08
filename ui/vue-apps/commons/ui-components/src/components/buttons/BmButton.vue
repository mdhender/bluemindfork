<template>
    <b-button class="bm-button" v-bind="[$attrs, $props]" :disabled="disabled || loading" v-on="$listeners">
        <bm-overlay :show="hasIcon && loading" variant="transparent">
            <div :class="loading ? 'invisible' : 'visible'">
                <slot name="icon">
                    <bm-icon v-if="icon" :icon="icon" />
                </slot>
            </div>
            <template #overlay>
                <bm-spinner :size="0.12" thick />
            </template>
        </bm-overlay>
        <bm-overlay :show="!hasIcon && loading" :opacity="0">
            <span class="slot-wrapper"><slot /></span>
            <template #overlay>
                <bm-spinner :size="0.12" thick />
            </template>
        </bm-overlay>
    </b-button>
</template>

<script>
import { BButton } from "bootstrap-vue";
import BmIcon from "../BmIcon";
import BmSpinner from "../BmSpinner";
import BmOverlay from "../BmOverlay";

export default {
    name: "BmButton",
    components: { BButton, BmIcon, BmSpinner, BmOverlay },
    props: {
        // those props are only passed to BButton, they are set here to not be recognized as an attribute
        ...BButton.options.props,
        variant: {
            type: String,
            default: "fill",
            validator: function (value) {
                return [
                    "fill-accent",
                    "fill",
                    "fill-danger",
                    "outline-accent",
                    "outline",
                    "outline-danger",
                    "text-accent",
                    "text",
                    "outline-on-fill-primary",
                    "text-on-fill-primary",
                    "link"
                ].includes(value);
            }
        },
        icon: {
            type: String,
            default: null
        },
        size: {
            type: String,
            default: "md",
            validator: function (value) {
                return ["sm", "md", "lg"].includes(value);
            }
        },
        loading: { type: Boolean, default: false }
    },
    computed: {
        hasIcon() {
            return this.icon || this.$scopedSlots.icon;
        }
    }
};
</script>

<style lang="scss">
@import "../../css/mixins/_buttons";

@include set-button-icon-sizes(
    ".bm-button",
    (
        "sm": "xs",
        "md": "sm",
        "lg": "md"
    )
);
</style>
