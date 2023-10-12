<template>
    <b-breadcrumb-item
        class="bm-breadcrumb-item"
        :class="{ 'item-active': active, disabled, link: isLink, 'interaction-ready': isInteractionReady }"
        :active="!isLink"
        v-bind="childProps"
        v-on="$listeners"
    >
        <slot>
            <slot name="icon">
                <bm-icon v-if="icon" :icon="icon" />
            </slot>
            <div v-if="text" class="text-truncate">
                {{ text }}
            </div>
        </slot>
    </b-breadcrumb-item>
</template>

<script>
import omit from "lodash.omit";
import { BBreadcrumbItem } from "bootstrap-vue";

import BmIcon from "./BmIcon";

export default {
    name: "BmBreadcrumbItem",
    components: { BBreadcrumbItem, BmIcon },
    props: {
        ...BBreadcrumbItem.options.props,

        interactive: {
            type: Boolean,
            default: true
        },
        icon: {
            type: [Object, Array, String],
            default: undefined
        }
    },
    computed: {
        childProps() {
            return omit(this.$props, "interactive", "icon");
        },
        isLink() {
            return this.interactive && !this.active;
        },
        isInteractionReady() {
            return this.isLink && !this.disabled;
        }
    }
};
</script>

<style lang="scss">
@import "../css/utils/focus";
@import "../css/utils/responsiveness";
@import "../css/utils/text";
@import "../css/utils/typography";
@import "../css/utils/variables";

.bm-breadcrumb-item {
    flex: 1 0 0;
    min-width: 2 * $sp-2 + map-get($icon-sizes, "md");
    @include from-lg {
        min-width: 2 * $sp-3 + map-get($icon-sizes, "md");
    }
    max-width: max-content;

    &.item-active {
        flex: 0 1 auto;
        @include text-overflow;
    }

    display: flex;

    > a,
    > span {
        display: flex;
        gap: $sp-3;
        padding: 0 $sp-2 base-px-to-rem(3);
        @include from-lg {
            padding-left: $sp-3;
            padding-right: $sp-3;
        }
        align-items: end;
        height: $breadcrumb-height;
        border-radius: $breadcrumb-border-radius;

        @include regular;
        @include text-overflow;
    }

    &.item-active > span {
        @include bold;
    }

    &.interaction-ready {
        cursor: pointer;
    }

    &.link .main {
        text-decoration: underline;
    }

    &.disabled > a {
        cursor: unset;
    }
}

@mixin define-colors($variant, $active-hi: true) {
    > a,
    > span {
        color: var(--#{$variant}-fg);
    }

    &.interaction-ready {
        &.hover > a,
        > a:hover {
            color: var(--#{$variant}-fg-hi1);
        }

        &.focus > a,
        > a:focus-visible {
            outline: 1px $outline-style var(--#{$variant}-fg);
            outline-offset: 0;
        }

        &.focus.hover > a,
        > a:focus-visible:hover {
            outline-color: var(--#{$variant}-fg-hi1);
        }
    }

    &.item-active > span {
        color: if($active-hi, var(--#{$variant}-fg-hi1), var(--#{$variant}-fg));
    }

    &.disabled {
        > a,
        > span {
            color: var(--#{$variant}-fg-disabled);
        }
    }
}

.bm-breadcrumb-item {
    @include define-colors("neutral");
}

.bm-breadcrumb.on-fill-primary .bm-breadcrumb-item {
    @include define-colors("fill-primary", $active-hi: false);
}
</style>
