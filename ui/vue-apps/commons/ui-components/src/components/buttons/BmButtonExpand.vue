<template>
    <bm-icon-button
        class="bm-button-expand"
        :class="{ expanded: expanded }"
        variant="compact"
        v-bind="[$attrs, $props]"
        :size="size"
        :icon="expanded ? 'chevron' : 'chevron-right'"
        v-on="$listeners"
    />
</template>

<script>
import BmIconButton from "./BmIconButton";

export default {
    name: "BmButtonExpand",
    components: { BmIconButton },
    props: {
        size: {
            type: String,
            default: "md",
            validator: function (value) {
                return ["sm", "md"].includes(value);
            }
        },
        expanded: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables";

.bm-button-expand {
    $icon-size: map-get($icon-sizes, "xs");

    .bm-icon {
        width: $icon-size !important;
        height: $icon-size !important;
    }

    &.expanded:not(:disabled):not(.disabled) {
        color: $neutral-fg-hi1;
    }
}
</style>
