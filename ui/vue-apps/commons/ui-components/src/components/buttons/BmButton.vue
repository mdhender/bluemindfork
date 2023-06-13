<template>
    <b-button class="bm-button" v-bind="[$attrs, $props]" :disabled="disabled || loading" v-on="$listeners">
        <bm-spinner v-if="loading" thick />
        <slot v-else name="icon">
            <bm-icon v-if="icon" :icon="icon" />
        </slot>
        <span class="slot-wrapper"><slot /></span>
    </b-button>
</template>

<script>
import { BButton } from "bootstrap-vue";
import BmIcon from "../BmIcon";
import BmSpinner from "../BmSpinner";

export default {
    name: "BmButton",
    components: { BButton, BmIcon, BmSpinner },
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
@import "../../css/utils/buttons";

@include set-button-icon-sizes(
    ".bm-button",
    (
        "sm": "xs",
        "md": "sm",
        "lg": "md"
    )
);
</style>
