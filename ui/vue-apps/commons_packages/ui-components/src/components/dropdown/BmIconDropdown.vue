<template>
    <bv-dropdown
        ref="b_dropdown"
        v-bind="[$attrs, childProps]"
        class="bm-icon-dropdown"
        :class="{
            regular,
            compact,
            'dropdown-split': split,
            'dropdown-no-caret': noCaret
        }"
        :variant="'icon-' + variant"
        v-on="$listeners"
    >
        <template slot="button-content">
            <slot name="icon">
                <bm-icon :icon="icon" />
            </slot>
        </template>
        <slot />
        <v-nodes v-if="extension && extensions.length" :vnodes="extensions" />
    </bv-dropdown>
</template>

<script>
import { useExtensions } from "@bluemind/extensions.vue";
import BmDropdownMixin from "./mixins/BmDropdownMixin";
import BmIcon from "../BmIcon";
import { BvDropdown } from "./BDropdown";
import VNodes from "../VNodes";

export default {
    name: "BmIconDropdown",
    components: { BvDropdown, BmIcon, VNodes },
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
            default: null
        },
        size: {
            type: String,
            default: "md",
            validator: function (value) {
                return ["sm", "md", "lg"].includes(value);
            }
        },
        split: {
            type: Boolean,
            default: false
        },
        noCaret: {
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
    },
    computed: {
        regular() {
            return this.variant.startsWith("regular");
        },
        compact() {
            return this.variant.startsWith("compact");
        },
        childProps() {
            const { extension, ...props } = this.$props;
            return props;
        }
    }
};
</script>

<style lang="scss">
@import "../../css/utils/variables";
@import "../../css/utils/buttons";

.bm-icon-dropdown.b-dropdown:not(.dropdown-no-caret) {
    .btn {
        justify-content: flex-start;
    }

    &.regular > .btn-sm {
        padding-left: base-px-to-rem(9);
        gap: base-px-to-rem(2);
        width: base-px-to-rem(52);
    }
    &.regular.dropdown-split > .btn-sm {
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
    &.regular.dropdown-split > .btn-md {
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
    &.regular.dropdown-split > .btn-lg {
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
    &.compact.dropdown-split > .btn-sm {
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
    &.compact.dropdown-split > .btn-md {
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
    &.compact.dropdown-split > .btn-lg {
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
