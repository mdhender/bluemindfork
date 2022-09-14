<template>
    <bm-toggleable-captioned-icon-button
        class="mark-as-unread-captioned-icon-button"
        :pressed="isMarkAsUnreadPressed"
        :caption="$t('mail.state.unread')"
        :title="ariaText"
        :aria-label="ariaText"
        v-bind="[$attrs, $props]"
        v-on="$listeners"
        @click.prevent.stop="action"
    >
        <bm-unread-fill-icon v-if="isMarkAsUnreadPressed" />
        <bm-icon v-else icon="unread" />
    </bm-toggleable-captioned-icon-button>
</template>

<script>
import { BmToggleableCaptionedIconButton, BmIcon, BmUnreadFillIcon } from "@bluemind/styleguide";
import { MarkAsUnreadMixin } from "~/mixins";

export default {
    name: "MarkAsUnreadCaptionedIconButton",
    components: { BmToggleableCaptionedIconButton, BmIcon, BmUnreadFillIcon },
    mixins: [MarkAsUnreadMixin]
};
</script>
