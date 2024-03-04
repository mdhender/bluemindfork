<template>
    <b-button
        v-if="!extension || !extensions.length"
        v-bind="childProps"
        class="bm-icon-button"
        :class="classes"
        :variant="'icon-' + variant"
        v-on="$listeners"
    >
        <slot>
            <bm-icon :icon="icon" />
        </slot>
    </b-button>
    <bm-icon-dropdown
        v-else
        class="bm-icon-button"
        :class="classes"
        v-bind="[$attrs, childProps]"
        :variant="variant"
        split
        v-on="$listeners"
    >
        <v-nodes :vnodes="extensions" />
    </bm-icon-dropdown>
</template>

<script>
import { computed, useAttrs } from "vue";
import { BButton } from "bootstrap-vue";
import { useExtensions } from "@bluemind/extensions.vue";
import BmIcon from "../BmIcon";
import BmIconDropdown from "../dropdown/BmIconDropdown";
import VNodes from "../VNodes";

export default {
    name: "BmIconButton",
    components: { BButton, BmIconDropdown, BmIcon, VNodes },
    props: {
        ...BButton.options.props,
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
        },
        extension: {
            type: String,
            default: undefined
        },
        extensionId: {
            type: String,
            default: undefined
        }
    },
    setup() {
        const { renderWebAppExtensions } = useExtensions();
        return { renderWebAppExtensions };
    },
    computed: {
        regular() {
            return this.variant.startsWith("regular");
        },
        compact() {
            return this.variant.startsWith("compact");
        },
        classes() {
            return {
                regular: this.regular,
                compact: this.compact,
                accent: this.variant === "regular-accent",
                danger: this.variant === "compact-danger",
                "on-fill-primary": this.variant.endsWith("on-fill-primary")
            };
        },
        extensions() {
            return this.renderWebAppExtensions(this.extension, this.extensionId, this.$attrs);
        },
        childProps() {
            const { extension, ...props } = this.$props;
            return props;
        }
    }
};
</script>

<style lang="scss">
@import "../../css/utils/variables";
@import "../../css/utils/buttons";

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
