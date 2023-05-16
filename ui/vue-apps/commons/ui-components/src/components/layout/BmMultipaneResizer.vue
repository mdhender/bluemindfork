<template>
    <multipane-resizer class="bm-multipane-resizer" v-bind="[$attrs, $props]" v-on="$listeners">
        <svg viewBox="0 0 2 14" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
                d="M2 1C2 1.55228 1.55228 2 1 2C0.447715 2 0 1.55228 0 1C0 0.447715 0.447715 0 1 0C1.55228 0 2 0.447715 2 1Z M2 4C2 4.55228 1.55228 5 1 5C0.447715 5 0 4.55228 0 4C0 3.44772 0.447715 3 1 3C1.55228 3 2 3.44772 2 4Z M2 7C2 7.55228 1.55228 8 1 8C0.447715 8 0 7.55228 0 7C0 6.44772 0.447715 6 1 6C1.55228 6 2 6.44772 2 7Z M2 10C2 10.5523 1.55228 11 1 11C0.447715 11 0 10.5523 0 10C0 9.44771 0.447715 9 1 9C1.55228 9 2 9.44771 2 10Z M2 13C2 13.5523 1.55228 14 1 14C0.447715 14 0 13.5523 0 13C0 12.4477 0.447715 12 1 12C1.55228 12 2 12.4477 2 13Z"
            />
        </svg>
    </multipane-resizer>
</template>

<script>
import { MultipaneResizer } from "@bluemind/vue-multipane";

export default {
    name: "BmMultipaneResizer",
    components: { MultipaneResizer },
    props: {
        ...MultipaneResizer.props
    }
};
</script>

<style lang="scss">
@import "../../css/_variables.scss";

.bm-multipane-resizer {
    flex: none;

    background-color: $surface;
    margin: 0 !important;
    top: 0 !important;
    bottom: 0 !important;
    left: 0 !important;
    right: 0 !important;

    &::before {
        content: "";
        background-color: $separator-color;
        position: absolute;
        top: 0;
        bottom: 0;
        left: 0;
        right: 0;
    }

    display: flex !important;
    align-items: center;
    justify-content: center;

    svg {
        width: base-px-to-rem(2);
        height: base-px-to-rem(14);
        fill: $neutral-fg-lo2;
        z-index: 1;
    }

    &:hover {
        &::before {
            background-color: $resizer-hover-color;
        }
        svg {
            fill: $neutral-fg-lo1;
        }
    }
}

$hover-border-color: $neutral-fg-lo3;
$active-border-color: $neutral-fg-lo2;

.layout-v > .bm-multipane-resizer {
    width: $separator-thickness !important;
    &:hover::before {
        border-left: 1px solid $hover-border-color;
        border-right: 1px solid $hover-border-color;
    }
    &:active::before {
        border-left: 1px solid $active-border-color;
        border-right: 1px solid $active-border-color;
    }
}

.layout-h > .bm-multipane-resizer {
    height: $separator-thickness !important;
    svg {
        transform: rotate(90deg);
    }
    &:hover::before {
        border-top: 1px solid $hover-border-color;
        border-bottom: 1px solid $hover-border-color;
    }
    &:active::before {
        border-top: 1px solid $active-border-color;
        border-bottom: 1px solid $active-border-color;
    }
}
</style>
