<template>
    <button class="bm-sort-control" :class="{ 'in-table': inTable }" v-on="$listeners">
        <div class="sort-arrows">
            <div class="caret-wrapper">
                <bm-icon class="caret caret-up" size="xs" icon="caret-up" :class="{ active: value === 'asc' }" />
            </div>
            <div class="caret-wrapper">
                <bm-icon class="caret caret-down" size="xs" icon="caret-down" :class="{ active: value === 'desc' }" />
            </div>
        </div>
        <slot />
    </button>
</template>

<script>
import BmIcon from "./BmIcon";

export default {
    name: "BmSortControl",
    components: { BmIcon },
    props: {
        value: {
            type: String,
            validator: function (value) {
                return ["asc", "desc", null].includes(value);
            },
            default: null
        },
        inTable: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "../css/_mixins.scss";
@import "../css/_type.scss";
@import "../css/_variables";

.bm-sort-control {
    background: none;
    border: none;
    outline: none;

    &:focus {
        @include default-focus($neutral-fg, true);
    }

    display: flex;
    align-items: center;
    color: $primary-fg;
    gap: $sp-2 + $sp-3;
    height: base-px-to-rem(24);
    padding: 0 $sp-4;
    @extend %bold;

    .sort-arrows {
        display: inline-flex;
        flex-direction: column;
        align-items: center;

        .caret-wrapper {
            width: base-px-to-rem(8);
            height: base-px-to-rem(9);
            display: flex;
            justify-content: center;
            position: relative;
        }

        .bm-icon.caret-down {
            position: absolute;
            top: base-px-to-rem(-3);
        }

        .bm-icon.caret {
            color: $primary-fg-disabled;
            &.active {
                color: $primary-fg-hi1;
            }
        }
    }

    &.in-table {
        color: $neutral-fg;
        gap: $sp-3;
        padding: 0;
        @extend %caption-bold;

        .sort-arrows {
            .bm-icon.caret {
                color: $neutral-fg-disabled;
                &.active {
                    color: $neutral-fg-hi1;
                }
            }
        }
    }
}
</style>
