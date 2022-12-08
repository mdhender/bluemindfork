<template>
    <b-dropdown
        ref="b_dropdown"
        v-bind="[$attrs, $props]"
        class="bm-captioned-icon-dropdown"
        variant="captioned-icon"
        v-on="$listeners"
    >
        <template slot="button-content">
            <bm-icon :icon="icon" />
            <span class="caption">{{ caption }}</span>
        </template>
        <slot />
    </b-dropdown>
</template>

<script>
import { BDropdown } from "bootstrap-vue";
import BmDropdownMixin from "./mixins/BmDropdownMixin";
import BmIcon from "../BmIcon";

export default {
    name: "BmCaptionedIconDropdown",
    components: { BDropdown, BmIcon },
    mixins: [BmDropdownMixin],
    props: {
        icon: {
            type: String,
            required: true
        },
        caption: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables";

.bm-captioned-icon-dropdown.b-dropdown[split] {
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
