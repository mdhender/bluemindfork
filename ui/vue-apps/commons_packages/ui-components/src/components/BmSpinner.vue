<template>
    <div class="bm-spinner">
        <svg
            xmlns="http://www.w3.org/2000/svg"
            :class="`spinner-${size}`"
            xmlns:xlink="http://www.w3.org/1999/xlink"
            viewBox="0 0 250 250"
            class="d-none"
        >
            <defs>
                <path
                    id="a"
                    fill="none"
                    stroke-linecap="round"
                    :stroke-width="strokeWidth"
                    d="M 125,25 212,75 212,175 125,225 38,175 38,75 125,25"
                />

                <path
                    id="c"
                    fill="none"
                    stroke-linecap="round"
                    :stroke-width="strokeWidth"
                    d="M 125,50 190,87.5 190,162.5 125,200 60,162.5 60,87.5 125,50"
                />
                <path
                    id="d"
                    fill="none"
                    stroke-linecap="round"
                    :stroke-width="strokeWidth"
                    d="M 125,75 168.5,100 168.5,150 125,175 81.5,150 81.5,100 125,75"
                />
            </defs>
        </svg>
        <svg :class="`spinner-${size}`" class="spinner d-inline-block" viewBox="0 0 250 250">
            <use xlink:href="#a" fill-opacity="0" stroke="#00AAEB" class="v4" />
            <use xlink:href="#a" fill-opacity="0" stroke="#0A2A86" class="v3" />
        </svg>
    </div>
</template>

<script>
export default {
    name: "BmSpinner",
    props: {
        size: {
            type: String,
            default: "5xl",
            validator: function (value) {
                return ["xs", "sm", "md", "lg", "xl", "2xl", "3xl", "4xl", "5xl"].includes(value);
            }
        },
        thick: { type: Boolean, default: false }
    },
    computed: {
        strokeWidth() {
            return this.thick ? 20 : 10;
        }
    }
};
</script>

<style lang="scss">
@import "../css/utils/variables.scss";

@each $name, $value in $icon-sizes {
    .bm-spinner > .spinner-#{$name} {
        width: $value;
        height: $value;
    }
}

.bm-spinner {
    .v3 {
        stroke-dasharray: 200 610;
        stroke-dashoffset: 1015;
        animation: draw 2s ease-in-out infinite -1s;
    }

    .v4 {
        stroke-dasharray: 350 610;
        stroke-dashoffset: 1310;
        animation: draw2 2s cubic-bezier(0.42, 0, 1, 0.31) infinite -1s;
    }

    @keyframes draw2 {
        60% {
            stroke-dashoffset: 350;
        }
        to {
            stroke-dashoffset: 350;
        }
    }

    @keyframes draw {
        80% {
            stroke-dashoffset: 210;
        }
        to {
            stroke-dashoffset: 205;
        }
    }
}
</style>
