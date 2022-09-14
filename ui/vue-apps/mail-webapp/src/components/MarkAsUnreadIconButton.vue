<template>
    <bm-icon-button
        class="mark-as-unread-icon-button"
        :title="ariaText"
        :aria-label="ariaText"
        v-bind="[$attrs, $props]"
        v-on="$listeners"
        @click.prevent.stop="action"
    >
        <bm-unread-fill-icon v-if="isMarkAsUnreadPressed" />
        <bm-icon v-else icon="unread" />
    </bm-icon-button>
</template>

<script>
import { BmIconButton, BmIcon, BmUnreadFillIcon } from "@bluemind/styleguide";
import { MarkAsUnreadMixin } from "~/mixins";

export default {
    name: "MarkAsUnreadIconButton",
    components: { BmIconButton, BmIcon, BmUnreadFillIcon },
    mixins: [MarkAsUnreadMixin]
};
</script>
