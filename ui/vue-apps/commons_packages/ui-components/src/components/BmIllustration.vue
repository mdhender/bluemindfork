<template>
    <!-- eslint-disable vue/no-v-html -->
    <div
        role="img"
        class="bm-illustration"
        :class="{
            [`illustration-${size}`]: true,
            [`illustration-from-lg-${sizeLg}`]: Boolean(sizeLg),
            'illustration-over-background': overBackground,
            'illustration-from-lg-over-background': overBackgroundLg === undefined ? overBackground : overBackgroundLg
        }"
        v-html="svgData"
    />
</template>

<script>
export default {
    name: "BmIllustration",
    props: {
        value: {
            type: String,
            required: true
        },
        size: {
            type: String,
            default: "md",
            validator(value) {
                return ["xxs", "xs", "sm", "md", "lg", "xl"].includes(value);
            }
        },
        sizeLg: {
            type: String,
            default: undefined,
            validator(value) {
                return ["xxs", "xs", "sm", "md", "lg", "xl", undefined].includes(value);
            }
        },
        overBackground: {
            type: Boolean,
            default: false
        },
        overBackgroundLg: {
            type: Boolean,
            default: undefined
        }
    },
    computed: {
        svgData() {
            return require(`../illustrations/${this.value}.svg`);
        }
    }
};
</script>

<style lang="scss">
@use "sass:map";

@import "../css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.bm-illustration {
    overflow: hidden;
    display: flex;
    justify-content: center;
    align-items: center;
    & > svg {
        flex: none;
    }

    #fg {
        fill: $neutral-fg-hi1;
    }
    #bg {
        fill: $backdrop;
    }
    &.illustration-over-background {
        @include until-lg {
            #bg {
                fill: $surface;
            }
        }
    }
    &.illustration-from-lg-over-background {
        @include from-lg {
            #bg {
                fill: $surface;
            }
        }
    }

    $illustration-sizes: "xxs", "xs", "s", "sm", "md", "lg", "xl";

    @each $size in $illustration-sizes {
        &.illustration-#{$size} {
            &,
            & > svg {
                width: map-get($illustration-widths, $size);
                height: map-get($illustration-heights, $size);
            }
        }
        &.illustration-from-lg-#{$size} {
            @include from-lg {
                &,
                & > svg {
                    width: map-get($illustration-widths, $size);
                    height: map-get($illustration-heights, $size);
                }
            }
        }
    }
}
</style>
