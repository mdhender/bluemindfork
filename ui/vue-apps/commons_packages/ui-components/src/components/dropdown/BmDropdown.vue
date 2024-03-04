<template>
    <bv-dropdown
        ref="b_dropdown"
        v-bind="[$attrs, childProps]"
        :dropright="isSubMenu"
        class="bm-dropdown"
        :variant="variant_"
        :size="isSubMenu ? 'lg' : size"
        :class="{
            'dropdown-text': variant_.startsWith('text'),
            'dropdown-on-fill-primary': variant_.endsWith('on-fill-primary'),
            'dropdown-split': split,
            'dropdown-sub-menu': isSubMenu
        }"
        v-on="$listeners"
        @click="onClick"
        @mouseenter.native="expandSub"
    >
        <template slot="button-content">
            <slot name="button-content">
                <slot name="icon">
                    <bm-icon v-if="icon" :icon="icon" />
                </slot>
                <span v-if="$scopedSlots['button-content']" class="dropdown-button-content">
                    <slot name="button-content" />
                </span>
                <span v-if="text" class="dropdown-button-text">{{ text }}</span>
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
    name: "BmDropdown",
    components: { BvDropdown, BmIcon, VNodes },
    mixins: [BmDropdownMixin],
    inject: {
        getBvDropdown: { default: () => () => null }
    },
    inheritAttrs: false,
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
        },
        extension: {
            type: String,
            default: undefined
        },
        extensionId: {
            type: String,
            default: undefined
        }
    },
    setup() {
        const { renderWebAppExtensions } = useExtensions();
        return { renderWebAppExtensions };
    },
    computed: {
        isSubMenu() {
            return !!this.getBvDropdown();
        },
        $subSplitEl() {
            return this.split && this.isSubMenu && this.$el.getElementsByClassName("dropdown-toggle-split")?.[0];
        },
        childProps() {
            const { extension, ...props } = this.$props;
            return props;
        },
        variant_() {
            return this.isSubMenu ? (this.split ? "text" : "text-thin") : this.variant;
        },
        extensions() {
            return this.renderWebAppExtensions(this.extension, this.extensionId, this.$attrs);
        }
    },
    mounted() {
        this.$subSplitEl?.addEventListener?.("mouseenter", this.show);
    },
    destroyed() {
        this.$subSplitEl?.removeEventListener?.("mouseenter", this.show);
    },
    methods: {
        expandSub() {
            if (this.isSubMenu && !this.split) {
                this.show();
            }
        },
        onClick(event) {
            // Close all parents dropdowns when clicking on the button part of a splitted dropdown
            let parent = this.getBvDropdown();
            while (parent) {
                parent.hide(event);
                parent = parent.$parent.getBvDropdown?.();
            }
        }
    }
};
</script>

<style lang="scss">
@import "../../css/utils/typography";
@import "../../css/utils/variables";
@import "../../css/utils/buttons";
@import "../../css/utils/text";

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

    &.b-dropdown.dropdown-sub-menu {
        width: 100%;
        height: $dropdown-item-height;

        > .btn {
            .dropdown-button-text {
                @include text-overflow;
            }
            @include bm-button-variant(
                $normal-text: $neutral-fg,
                $hovered-text: $neutral-fg-hi1,
                $disabled-text: $neutral-fg-disabled,
                $hovered-bg: $neutral-bg-lo1,
                $focused-stroke: $neutral-fg,
                $focused-hovered-stroke: $neutral-fg-hi1,
                $important: true
            );
            @include regular;
            outline-offset: -1px;

            gap: 0;
            $icon-size: map-get($icon-sizes, "md");
            padding-left: $icon-size + 2 * $sp-4;
            > .bm-icon {
                position: absolute;
                left: base-px-to-rem(8);
                width: $icon-size !important;
                height: $icon-size !important;
            }

            flex: 1;
            justify-content: start;
            > span {
                flex: 1;
                text-align: left;
            }
            &.dropdown-toggle-split {
                flex: none;
            }
        }
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
