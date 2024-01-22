<template>
    <b-button
        v-if="!extension || !extensions.length"
        class="bm-button"
        v-bind="childProps"
        :disabled="disabled || loading"
        v-on="$listeners"
    >
        <bm-spinner v-if="loading" thick />
        <slot v-else name="icon">
            <bm-icon v-if="icon" :icon="icon" />
        </slot>
        <span class="slot-wrapper"><slot /></span>
    </b-button>
    <bm-dropdown v-else class="bm-button" v-bind="childProps" :disabled="disabled || loading" split v-on="$listeners">
        <template #button-content>
            <slot name="icon">
                <bm-icon v-if="icon" :icon="icon" />
            </slot>
            <span class="slot-wrapper"><slot /></span>
        </template>
        <v-nodes :vnodes="extensions" />
    </bm-dropdown>
</template>

<script>
import { computed } from "vue";
import { BButton } from "bootstrap-vue";
import { useExtensions } from "@bluemind/extensions.vue";
import VNodes from "../VNodes";
import BmIcon from "../BmIcon";
import BmDropdown from "../dropdown/BmDropdown";
import BmSpinner from "../BmSpinner";

export default {
    name: "BmButton",
    components: { BButton, BmDropdown, BmIcon, BmSpinner, VNodes },
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
        loading: { type: Boolean, default: false },
        extension: {
            type: String,
            default: undefined
        }
    },
    setup(props) {
        const { renderWebAppExtensions } = useExtensions();
        const extensions = renderWebAppExtensions(props.extension);
        return { extensions };
    },
    computed: {
        childProps() {
            const { extension, ...childProps } = this.$props;
            return childProps;
        },
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
