<template>
    <bm-button class="bm-toggleable-button" :pressed="pressed" v-bind="[$attrs, $props]" v-on="$listeners">
        <slot />
    </bm-button>
</template>

<script>
import BmButton from "./BmButton";

export default {
    name: "BmToggleableButton",
    components: { BmButton },
    props: {
        variant: {
            type: String,
            default: "outline",
            validator: function (value) {
                return ["outline", "text"].includes(value);
            }
        },
        pressed: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "../../css/mixins/_buttons";

.bm-toggleable-button {
    &.active {
        &.btn-outline {
            @include bm-button-fill-variant("fill-neutral");
        }
        &.btn-text {
            @include bm-button-text-variant("neutral");
        }
    }
}
</style>
