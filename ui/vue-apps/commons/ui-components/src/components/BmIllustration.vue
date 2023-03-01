<template>
    <!-- eslint-disable vue/no-v-html -->
    <div
        role="img"
        class="bm-illustration"
        :class="{ [`illustration-${size}`]: true, 'illustration-over-background': overBackground }"
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
                return ["xs", "sm", "md", "lg", "xl"].includes(value);
            }
        },
        overBackground: {
            type: Boolean,
            default: false
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
@import "../css/variables";

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
        #bg {
            fill: $surface;
        }
    }

    &.illustration-xs {
        &,
        & > svg {
            width: $illustration-width-xs;
            height: $illustration-height-xs;
        }
    }

    &.illustration-sm {
        &,
        & > svg {
            width: $illustration-width-sm;
            height: $illustration-height-sm;
        }
    }

    &.illustration-md {
        &,
        & > svg {
            width: $illustration-width;
            height: $illustration-height;
        }
    }

    &.illustration-lg {
        &,
        & > svg {
            width: $illustration-width-lg;
            height: $illustration-height-lg;
        }
    }

    &.illustration-xl {
        &,
        & > svg {
            width: $illustration-width-xl;
            height: $illustration-height-xl;
        }
    }
}
</style>
