<template>
    <b-button v-bind="[$props]" class="bm-button-close" :class="{ 'on-fill-primary': onFillPrimary }" v-on="$listeners">
        <bm-icon icon="cross-thin" />
    </b-button>
</template>

<script>
import { BButton } from "bootstrap-vue";
import BmIcon from "../BmIcon";

export default {
    name: "BmButtonClose",
    components: { BButton, BmIcon },
    props: {
        size: {
            type: String,
            default: "md",
            validator(value) {
                return ["xs", "sm", "md", "lg"].includes(value);
            }
        },
        disabled: {
            type: Boolean,
            default: false
        },
        onFillPrimary: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "../../css/utils/variables";
@import "../../css/utils/buttons";

.bm-button-close {
    flex: none;

    @include bm-icon-button-compact-variant("neutral");

    &.on-fill-primary {
        @include bm-icon-button-compact-variant("fill-primary");
    }

    @each $name, $value in $btn-close-sizes {
        &.btn-#{$name} {
            height: $value;
            width: $value;
        }
    }
}

@include set-button-icon-sizes(
    ".bm-button-close",
    (
        "xs": "xs",
        "sm": "xs",
        "md": "md",
        "lg": "lg"
    )
);
</style>
