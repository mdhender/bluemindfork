<template>
    <bm-icon-button
        class="mark-as-flagged-icon-button"
        :icon="isMarkAsFlaggedPressed ? 'flag-fill' : 'flag-outline'"
        :title="ariaText"
        :aria-label="ariaText"
        v-bind="[$attrs, $props]"
        v-on="$listeners"
        @click.prevent.stop="action"
    />
</template>

<script>
import { BmIconButton } from "@bluemind/styleguide";
import { MarkAsFlaggedMixin } from "~/mixins";

export default {
    name: "MarkAsFlaggedIconButton",
    components: { BmIconButton },
    mixins: [MarkAsFlaggedMixin]
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mark-as-flagged-icon-button {
    .fa-flag-fill {
        color: $warning-fg;
    }
}
</style>
