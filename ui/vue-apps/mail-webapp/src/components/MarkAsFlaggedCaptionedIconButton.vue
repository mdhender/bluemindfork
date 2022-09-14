<template>
    <bm-toggleable-captioned-icon-button
        class="mark-as-flagged-captioned-icon-button"
        :pressed="isMarkAsFlaggedPressed"
        :caption="$t('mail.state.flagged')"
        :icon="isMarkAsFlaggedPressed ? 'flag-fill' : 'flag-outline'"
        :title="ariaText"
        :aria-label="ariaText"
        v-bind="[$attrs, $props]"
        v-on="$listeners"
        @click.prevent.stop="action"
    />
</template>

<script>
import { BmToggleableCaptionedIconButton } from "@bluemind/styleguide";
import { MarkAsFlaggedMixin } from "~/mixins";

export default {
    name: "MarkAsFlaggedCaptionedIconButton",
    components: { BmToggleableCaptionedIconButton },
    mixins: [MarkAsFlaggedMixin]
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mark-as-flagged-captioned-icon-button {
    .fa-flag-fill {
        color: $warning-fg;
    }
}
</style>
