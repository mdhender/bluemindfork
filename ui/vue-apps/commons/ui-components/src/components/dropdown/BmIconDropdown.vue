<template>
    <b-dropdown
        ref="b_dropdown"
        v-bind="[$attrs, $props]"
        class="bm-icon-dropdown"
        :class="{
            regular: regular,
            compact: compact
        }"
        :variant="'icon-' + variant"
        v-on="$listeners"
    >
        <template slot="button-content">
            <bm-icon :icon="icon" />
        </template>
        <slot />
    </b-dropdown>
</template>

<script>
import { BDropdown } from "bootstrap-vue";
import BmDropdownMixin from "./mixins/BmDropdownMixin";
import BmIcon from "../BmIcon";

export default {
    name: "BmIconDropdown",
    components: { BDropdown, BmIcon },
    mixins: [BmDropdownMixin],
    props: {
        variant: {
            type: String,
            default: "regular",
            validator: function (value) {
                return [
                    "regular-accent",
                    "regular",
                    "compact",
                    "compact-danger",
                    "regular-on-fill-primary",
                    "compact-on-fill-primary"
                ].includes(value);
            }
        },
        icon: {
            type: String,
            required: true
        },
        size: {
            type: String,
            default: "md",
            validator: function (value) {
                return ["sm", "md", "lg"].includes(value);
            }
        }
    },
    computed: {
        regular() {
            return this.variant.startsWith("regular");
        },
        compact() {
            return this.variant.startsWith("compact");
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables";
@import "../../css/mixins/_buttons";

.bm-icon-dropdown.b-dropdown:not([no-caret]) {
    .btn {
        justify-content: flex-start;
    }

    &.regular > .btn-sm {
        padding-left: base-px-to-rem(9);
        gap: base-px-to-rem(2);
        width: base-px-to-rem(52);
    }
    &.regular[split] > .btn-sm {
        &:not(.dropdown-toggle-split) {
            width: base-px-to-rem(30);
        }
        &.dropdown-toggle-split {
            width: base-px-to-rem(22);
            padding-left: base-px-to-rem(1);
        }
    }

    &.regular > .btn-md {
        padding-left: base-px-to-rem(11);
        gap: base-px-to-rem(4);
        width: base-px-to-rem(64);
    }
    &.regular[split] > .btn-md {
        &:not(.dropdown-toggle-split) {
            width: base-px-to-rem(37);
        }
        &.dropdown-toggle-split {
            width: base-px-to-rem(27);
            padding-left: base-px-to-rem(2);
        }
    }

    &.regular > .btn-lg {
        padding-left: base-px-to-rem(16);
        gap: base-px-to-rem(8);
        width: base-px-to-rem(76);
    }
    &.regular[split] > .btn-lg {
        &:not(.dropdown-toggle-split) {
            width: base-px-to-rem(44);
        }
        &.dropdown-toggle-split {
            width: base-px-to-rem(32);
            padding-left: base-px-to-rem(4);
        }
    }

    &.compact > .btn-sm {
        padding-left: base-px-to-rem(4);
        gap: base-px-to-rem(2);
        width: base-px-to-rem(38);
    }
    &.compact[split] > .btn-sm {
        &:not(.dropdown-toggle-split) {
            width: base-px-to-rem(21);
        }
        &.dropdown-toggle-split {
            width: base-px-to-rem(17);
            justify-content: flex-start;
            padding-left: base-px-to-rem(1);
        }
    }

    &.compact > .btn-md {
        padding-left: base-px-to-rem(8);
        gap: base-px-to-rem(4);
        width: base-px-to-rem(46);
    }
    &.compact[split] > .btn-md {
        &:not(.dropdown-toggle-split) {
            width: base-px-to-rem(26);
        }
        &.dropdown-toggle-split {
            width: base-px-to-rem(20);
            padding-left: base-px-to-rem(2);
        }
    }

    &.compact > .btn-lg {
        padding-left: base-px-to-rem(8);
        gap: base-px-to-rem(6);
        width: base-px-to-rem(54);
    }
    &.compact[split] > .btn-lg {
        &:not(.dropdown-toggle-split) {
            width: base-px-to-rem(31);
        }
        &.dropdown-toggle-split {
            width: base-px-to-rem(23);
            padding-left: base-px-to-rem(3);
        }
    }
}

@include set-button-icon-sizes(
    ".bm-icon-dropdown.regular .btn",
    (
        "sm": "lg",
        "md": "xl",
        "lg": "xl"
    )
);
@include set-button-icon-sizes(
    ".bm-icon-dropdown.compact .btn",
    (
        "sm": "md",
        "md": "md",
        "lg": "lg"
    )
);
</style>
