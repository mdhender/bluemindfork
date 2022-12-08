<template>
    <div class="bm-notice d-flex px-2">
        <div class="arrow-up" />
        <span class="text-center w-100 text-condensed py-1">{{ text }}</span>
    </div>
</template>

<script>
export default {
    name: "BmNotice",
    props: {
        text: { type: String, default: "" }
    }
};
</script>
<style lang="scss">
@import "../css/_variables.scss";

.bm-notice {
    margin: $tooltip-arrow-height 0 0;
    background-color: $fill-danger-bg;

    .arrow-up {
        position: relative;
        display: block;
        width: $tooltip-arrow-width;
        height: $tooltip-arrow-height;
        bottom: $tooltip-arrow-height;
        left: 50%;

        &::before {
            position: absolute;
            content: "";
            border-color: transparent;
            border-style: solid;
            bottom: 0;
            border-width: 0 calc($tooltip-arrow-width / 2) $tooltip-arrow-height;
            border-bottom-color: $danger-fg;
        }
    }

    span {
        color: $fill-danger-fg;
    }
}
</style>
