<template>
    <bv-dropdown
        ref="b_dropdown"
        v-bind="[$attrs, $props]"
        class="bm-captioned-icon-dropdown"
        :class="{
            'dropdown-split': split
        }"
        variant="captioned-icon"
        v-on="$listeners"
    >
        <template slot="button-content">
            <slot name="icon">
                <bm-icon :icon="icon" />
            </slot>
            <span class="caption">{{ caption }}</span>
        </template>
        <slot />
        <v-nodes v-if="extension && extensions.length" :vnodes="extensions" />
    </bv-dropdown>
</template>

<script>
import { useExtensions } from "@bluemind/extensions.vue";
import { BvDropdown } from "./BDropdown";
import BmDropdownMixin from "./mixins/BmDropdownMixin";
import BmIcon from "../BmIcon";
import VNodes from "../VNodes";

export default {
    name: "BmCaptionedIconDropdown",
    components: { BvDropdown, BmIcon, VNodes },
    mixins: [BmDropdownMixin],
    props: {
        icon: {
            type: String,
            default: null
        },
        caption: {
            type: String,
            required: true
        },
        split: {
            type: Boolean,
            default: false
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
    }
};
</script>

<style lang="scss">
@import "../../css/utils/variables";

.bm-captioned-icon-dropdown.dropdown-split {
    .btn {
        &:not(.dropdown-toggle-split) {
            padding-right: base-px-to-rem(2);
        }
        &.dropdown-toggle-split {
            width: base-px-to-rem(24);
            padding-left: base-px-to-rem(2);
        }
    }
}

.bm-captioned-icon-dropdown .btn .bm-icon {
    $size: map-get($icon-sizes, "lg");
    width: $size !important;
    height: $size !important;
}
</style>
