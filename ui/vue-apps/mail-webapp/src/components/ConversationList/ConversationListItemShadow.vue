<template>
    <div class="mail-conversation-list-item-shadow">
        <conversation-list-item-shadow-single v-if="count <= 1" :conversation="conversation" />
        <conversation-list-item-shadow-multiple v-else :count="count" />
    </div>
</template>
<script>
import ConversationListItemShadowSingle from "./ConversationListItemShadowSingle";
import ConversationListItemShadowMultiple from "./ConversationListItemShadowMultiple";

export default {
    name: "ConversationListItemShadow",
    components: { ConversationListItemShadowSingle, ConversationListItemShadowMultiple },
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
@import "~@bluemind/ui-components/src/css/variables";
@import "~@bluemind/ui-components/src/css/mixins";

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
