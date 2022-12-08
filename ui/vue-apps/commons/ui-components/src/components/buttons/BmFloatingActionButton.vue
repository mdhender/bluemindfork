<template>
    <div class="bm-floating-action-button">
        <b-button v-bm-clipping="'hexagon'" v-bind="[$attrs, $props]" variant="fill-accent" v-on="$listeners">
            <bm-icon :icon="icon" size="xl" />
        </b-button>
    </div>
</template>

<script>
import { BButton } from "bootstrap-vue";
import BmIcon from "../BmIcon";
import BmClipping from "../../directives/BmClipping";

export default {
    name: "BmFloatingActionButton",
    components: { BButton, BmIcon },
    directives: { BmClipping },
    props: {
        icon: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "../../css/_variables";

$clip-path-width: 100px;
$clip-path-height: 120px;
$fab-width: 50px;
$fab-height: $fab-width * math.div($clip-path-height, $clip-path-width);

.bm-floating-action-button {
    display: inline-block;
    width: $fab-width;
    height: $fab-height;
    position: relative;
    filter: drop-shadow($box-shadow);

    .btn {
        width: $clip-path-width;
        height: $clip-path-height;
        transform: scale(math.div($fab-width, $clip-path-width));
        transform-origin: top left;
        .bm-icon {
            transform: scale(math.div($clip-path-width, $fab-width));
        }
    }
}
</style>
