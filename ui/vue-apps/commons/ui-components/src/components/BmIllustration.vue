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
            width: 100px;
            height: 100px;
        }
    }

    &.illustration-sm {
        &,
        & > svg {
            width: 210px;
            height: 180px;
        }
    }

    &.illustration-md {
        &,
        & > svg {
            width: 350px;
            height: 300px;
        }
    }

    &.illustration-lg {
        &,
        & > svg {
            width: 525px;
            height: 450px;
        }
    }

    &.illustration-xl {
        &,
        & > svg {
            width: 700px;
            height: 600px;
        }
    }
}
</style>
