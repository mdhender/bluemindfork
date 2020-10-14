<template>
    <div class="mail-conversation-list-item-shadow">
        <mail-conversation-list-item-shadow-single v-if="count <= 1" :conversation="conversation" />
        <mail-conversation-list-item-shadow-multiple v-else :count="count" />
    </div>
</template>
<script>
import MailConversationListItemShadowSingle from "./MailConversationListItemShadowSingle";
import MailConversationListItemShadowMultiple from "./MailConversationListItemShadowMultiple";

export default {
    name: "MailConversationListItemShadow",
    components: { MailConversationListItemShadowSingle, MailConversationListItemShadowMultiple },
    props: {
        conversation: {
            type: Object,
            required: true
        },
        count: {
            type: Number,
            required: true
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
@import "~@bluemind/styleguide/css/mixins";

$shadow-width: 240px;

.mail-conversation-list-item-shadow {
    width: $shadow-width;
    background-color: $surface-bg;
}

/** @see dragAndDrop/plugins/tooltip.js */
.bm-drag-tooltip {
    max-width: $shadow-width * 2;
    @include text-overflow;
}
</style>
