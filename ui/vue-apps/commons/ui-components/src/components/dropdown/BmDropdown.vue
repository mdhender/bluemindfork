<template>
    <b-dropdown
        ref="b_dropdown"
        v-bind="[$attrs, $props]"
        class="bm-dropdown"
        :class="{
            'dropdown-text': variant.startsWith('text'),
            'dropdown-on-fill-primary': variant.endsWith('on-fill-primary'),
            'dropdown-split': split
        }"
        v-on="$listeners"
    >
        <template slot="button-content">
            <slot name="button-content">
                <slot name="icon">
                    <bm-icon v-if="icon" :icon="icon" />
                </slot>
                <span>{{ text }}</span>
            </slot>
        </template>
        <slot />
    </b-dropdown>
</template>

<script>
import { BDropdown } from "bootstrap-vue";
import BmDropdownMixin from "./mixins/BmDropdownMixin";
import BmIcon from "../BmIcon";

export default {
    name: "BmDropdown",
    components: { BDropdown, BmIcon },
    mixins: [BmDropdownMixin],
    props: {
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
                    "text",
                    "text-thin",
                    "outline-on-fill-primary",
                    "text-on-fill-primary",
                    "text-thin-on-fill-primary"
                ].includes(value);
            }
        },
        text: {
            type: String,
            default: null
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
        }
    }
};
</script>

<style lang="scss">
@import "../../css/utils/typography";
@import "../../css/utils/variables";
@import "../../css/utils/buttons";

.bm-dropdown {
    & > .btn-text-thin {
        @include bm-button-text-variant("neutral");
        @include regular;
    }
    & > .btn-text-thin-on-fill-primary {
        @include bm-button-text-variant("fill-primary");
        @include regular;
    }
    &.dropdown-text > .btn {
        @include bm-button-all-sizes("fill");
    }

    &.b-dropdown.dropdown-split {
        & > .btn:not(.dropdown-toggle-split) {
            border-right: none;
            &:after {
                content: "";
                height: calc(100% + 6px);
                border-right-style: solid;
                border-right-width: 1px;
            }

            // Size
            &.btn {
                padding-right: 0;
                &:before {
                    width: calc(100% - 1px);
                }
            }
            &.btn-sm:after {
                padding-right: base-px-to-rem(8) - $btn-icon-label-gap;
                height: calc(100% + #{base-px-to-rem(2)});
            }
            &.btn-md:after {
                padding-right: base-px-to-rem(10) - $btn-icon-label-gap;
                height: calc(100% + #{base-px-to-rem(6)});
            }
            &.btn-lg:after {
                padding-right: base-px-to-rem(12) - $btn-icon-label-gap;
                height: calc(100% + #{base-px-to-rem(12)});
            }

            // Color
            &.btn-fill-accent:after {
                border-color: $fill-secondary-fg-lo2;
            }
            &.btn-fill:after {
                border-color: $fill-neutral-fg-lo2;
            }
            &.btn-fill-danger:after {
                border-color: $fill-danger-fg-lo2;
            }
            &.btn-outline-accent:after {
                border-color: $secondary-fg-lo2;
            }
            &.btn-outline:after {
                border-color: $neutral-fg-lo3;
            }
            &.btn-outline-danger:after {
                border-color: $danger-fg-lo2;
            }
            &.btn-text:after,
            &.btn-text-thin:after {
                border-color: $neutral-fg-lo3;
            }
            &.btn-outline-on-fill-primary:after {
                border-color: $fill-primary-fg-lo2;
            }
            &.btn-text-on-fill-primary:after,
            &.btn-text-thin-on-fill-primary:after {
                border-color: $fill-primary-fg-lo2;
            }
        }

        & > .btn.dropdown-toggle-split {
            border-left: none;

            // Size
            &.btn-sm {
                $padding-x: base-px-to-rem(12) - $caret-width;
                padding-left: $padding-x;
                padding-right: $padding-x;
            }
            &.btn-md {
                $padding-x: base-px-to-rem(15) - $caret-width;
                padding-left: $padding-x;
                padding-right: $padding-x;
            }
            &.btn-lg {
                $padding-x: base-px-to-rem(18) - $caret-width;
                padding-left: $padding-x;
                padding-right: $padding-x;
            }
        }
    }
}

@include set-button-icon-sizes(
    ".bm-dropdown .btn",
    (
        "sm": "xs",
        "md": "sm",
        "lg": "md"
    )
);
</style>
