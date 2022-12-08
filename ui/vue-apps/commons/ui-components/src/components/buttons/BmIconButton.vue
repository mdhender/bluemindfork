<template>
    <b-button
        v-bind="[$attrs, $props]"
        class="bm-icon-button"
        :class="{
            regular: regular,
            compact: compact,
            accent: variant === 'regular-accent',
            danger: variant === 'compact-danger',
            'on-fill-primary': variant.endsWith('on-fill-primary')
        }"
        :variant="'icon-' + variant"
        v-on="$listeners"
    >
        <slot>
            <bm-icon :icon="icon" />
        </slot>
    </b-button>
</template>

<script>
import { BButton } from "bootstrap-vue";
import BmIcon from "../BmIcon";

export default {
    name: "BmIconButton",
    components: { BButton, BmIcon },
    props: {
        variant: {
            type: String,
            default: "regular",
            validator: function (value) {
                return [
                    "regular-accent",
                    "regular",
                    "compact",
                    "compact-danger",
                    "regular-on-fill-primary",
                    "compact-on-fill-primary"
                ].includes(value);
            }
        },
        icon: {
            type: String,
            default: null
        },
        size: {
            type: String,
            default: "md",
            validator: function (value) {
                return ["sm", "md", "lg"].includes(value);
            }
        }
    },
    computed: {
        regular() {
            return this.variant.startsWith("regular");
        },
        compact() {
            return this.variant.startsWith("compact");
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables";
@import "../../css/mixins/_buttons";

@mixin regular-sizes {
    height: $icon-btn-height;
    width: $icon-btn-width-regular;
    flex: none;

    &.btn-sm {
        height: $icon-btn-height-sm;
        width: $icon-btn-width-regular-sm;
    }

    &.btn-lg {
        height: $icon-btn-height-lg;
        width: $icon-btn-width-regular-lg;
    }
}

@mixin compact-sizes {
    height: $icon-btn-height;
    width: $icon-btn-width-compact;
    flex: none;

    &.btn-sm {
        height: $icon-btn-height-sm;
        width: $icon-btn-width-compact-sm;
    }

    &.btn-lg {
        height: $icon-btn-height-lg;
        width: $icon-btn-width-compact-lg;
    }
}

.btn-icon-regular-accent {
    @include bm-icon-button-regular-variant("secondary");
    @include regular-sizes();
}
.btn-icon-regular {
    @include bm-icon-button-regular-variant("neutral");
    @include regular-sizes();
}
.btn-icon-regular-on-fill-primary {
    @include bm-icon-button-regular-variant("fill-primary");
    @include regular-sizes();
}
.btn-icon-compact {
    @include bm-icon-button-compact-variant("neutral");
    @include compact-sizes();
}
.btn-icon-compact-on-fill-primary {
    @include bm-icon-button-compact-variant("fill-primary");
    @include compact-sizes();
}
.btn-icon-compact-danger {
    @include bm-icon-button-compact-variant("danger");
    @include compact-sizes();
}

@include set-button-icon-sizes(
    ".bm-icon-button.regular",
    (
        "sm": "lg",
        "md": "xl",
        "lg": "xl"
    )
);
@include set-button-icon-sizes(
    ".bm-icon-button.compact",
    (
        "sm": "md",
        "md": "md",
        "lg": "lg"
    )
);
</style>
