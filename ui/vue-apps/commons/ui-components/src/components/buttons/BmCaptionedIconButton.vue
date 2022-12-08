<template>
    <b-button v-bind="[$attrs, $props]" class="bm-captioned-icon-button" variant="captioned-icon" v-on="$listeners">
        <slot>
            <bm-icon :icon="icon" />
        </slot>
        <span class="caption">{{ caption }}</span>
    </b-button>
</template>

<script>
import { BButton } from "bootstrap-vue";
import BmIcon from "../BmIcon";

export default {
    name: "BmCaptionedIconButton",
    components: { BButton, BmIcon },
    props: {
        icon: {
            type: String,
            default: null
        },
        caption: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables";
@import "../../css/mixins/_buttons";

.btn-captioned-icon {
    @include bm-button-text-variant("neutral");

    padding: base-px-to-rem(4) base-px-to-rem(10);
    gap: base-px-to-rem(2);
    flex-direction: column;
    white-space: nowrap;
}

.bm-captioned-icon-button .bm-icon {
    $size: map-get($icon-sizes, "lg");
    width: $size !important;
    height: $size !important;
}
</style>
