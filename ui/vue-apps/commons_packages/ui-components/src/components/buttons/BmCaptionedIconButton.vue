<template>
    <b-button
        v-if="!extension || !extensions.length"
        v-bind="[$attrs, $props]"
        class="bm-captioned-icon-button"
        variant="captioned-icon"
        v-on="$listeners"
    >
        <slot>
            <bm-icon :icon="icon" />
        </slot>
        <span class="caption">{{ caption }}</span>
    </b-button>
    <bm-captioned-icon-dropdown
        v-else
        class="bm-captioned-icon-button"
        v-bind="[$attrs, childProps]"
        split
        v-on="$listeners"
    >
        <v-nodes :vnodes="extensions" />
    </bm-captioned-icon-dropdown>
</template>

<script>
import { BButton } from "bootstrap-vue";
import { useExtensions } from "@bluemind/extensions.vue";
import BmIcon from "../BmIcon";
import BmCaptionedIconDropdown from "../dropdown/BmCaptionedIconDropdown";
import VNodes from "../VNodes";

export default {
    name: "BmCaptionedIconButton",
    components: { BButton, BmIcon, BmCaptionedIconDropdown, VNodes },
    props: {
        icon: {
            type: String,
            default: null
        },
        caption: {
            type: String,
            required: true
        },
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
        }
    }
};
</script>

<style lang="scss">
@import "../../css/utils/variables";
@import "../../css/utils/buttons";

.btn-captioned-icon {
    @include bm-button-text-variant("neutral");

    padding: base-px-to-rem(4) base-px-to-rem(10);
    gap: base-px-to-rem(2);
    flex-direction: column;
    white-space: nowrap;
}

.bm-captioned-icon-button .bm-icon {
    $size: map-get($icon-sizes, "lg");
    width: $size !important;
    height: $size !important;
}
</style>
