<template>
    <div
        ref="root"
        role="img"
        class="bm-illustration"
        :class="{ [`illustration-${size}`]: true, 'illustration-over-background': overBackground }"
        :title="alt"
    />
</template>

<script>
export default {
    name: "BmIllustration",
    props: {
        src: {
            type: String,
            required: true
        },
        alt: {
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
    watch: {
        src: {
            handler: function (src) {
                fetch(src)
                    .then(r => {
                        return r.text();
                    })
                    .then(text => {
                        this.$refs.root.innerHTML = text;
                    });
            },
            immediate: true
        }
    }
};
</script>

<style lang="scss">
@import "../css/variables";

.bm-illustration {
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

    &.illustration-xs > svg {
        width: 100px;
        height: 100px;
    }

    &.illustration-sm > svg {
        width: 210px;
        height: 180px;
    }

    &.illustration-md > svg {
        width: 350px;
        height: 300px;
    }

    &.illustration-lg > svg {
        width: 525px;
        height: 450px;
    }

    &.illustration-xl > svg {
        width: 700px;
        height: 600px;
    }
}
</style>
