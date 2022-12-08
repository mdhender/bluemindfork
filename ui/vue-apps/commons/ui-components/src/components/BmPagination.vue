<template>
    <b-pagination
        v-if="totalRows > perPage"
        v-bind="$props"
        class="bm-pagination"
        :hide-goto-end-buttons="hideGotoEndButtons || pagesCount < 6"
        v-on="$listeners"
    >
        <template #prev-text><bm-icon icon="chevron-left" /></template>
        <template #next-text><bm-icon icon="chevron-right" /></template>
        <template #first-text><bm-icon icon="caret-first" /></template>
        <template #last-text><bm-icon icon="caret-last" /></template>
        <slot />
    </b-pagination>
</template>

<script>
import { BPagination } from "bootstrap-vue";
import BmIcon from "./BmIcon";

export default {
    name: "BmPagination",
    components: { BmIcon },
    extends: BPagination,
    props: {
        hideGotoEndButtons: {
            type: Boolean,
            default: false
        },
        nextClass: {
            type: String,
            default: "next"
        },
        prevClass: {
            type: String,
            default: "prev"
        }
    },
    computed: {
        pagesCount() {
            return Math.ceil(this.totalRows / this.perPage);
        }
    }
};
</script>

<style lang="scss">
@import "../css/_mixins";
@import "../css/_variables";

.bm-pagination .page-item {
    .page-link {
        font-weight: $font-weight-bold;
        width: base-px-to-rem(32);
        height: base-px-to-rem(26);
        padding: 0;
        background-color: transparent;
        border-color: $neutral-fg-lo3;
        box-shadow: unset;
        font-size: base-px-to-rem(12);
        color: $neutral-fg;
        &:focus {
            @include default-focus($neutral-fg-hi1);
            z-index: 4;
        }
    }
    button.page-link {
        padding-top: base-px-to-rem(2);
    }
    span.page-link {
        padding-top: base-px-to-rem(4);
        padding-left: base-px-to-rem(5);
    }

    &.active .page-link {
        background-color: $neutral-bg-hi1;
        font-size: base-px-to-rem(16);
        color: $neutral-fg-hi1;
    }
    &.disabled .page-link {
        color: $neutral-fg-disabled;
    }

    &:first-child,
    &:last-child,
    &.prev,
    &.next {
        .page-link {
            width: base-px-to-rem(26);
        }
    }
    &[role="separator"] {
        .page-link {
            width: base-px-to-rem(26);
            padding-left: base-px-to-rem(8);
        }
    }
}
</style>
