<template>
    <bm-row
        class="mail-conversation-list-item-shadow-multiple py-2 no-gutters align-items-center"
        :class="{ 'lot-of': count > 2 }"
    >
        <bm-col cols="1" class="text-right bm-drag-handle">
            <bm-icon icon="6dots-v" />
        </bm-col>
        <bm-col class="p-1 text-overflow font-weight-bold">
            {{ $tc("mail.messages.multiple.drag_shadow", count, { count }) }}
        </bm-col>
    </bm-row>
</template>
<script>
import { BmRow, BmCol, BmIcon } from "@bluemind/styleguide";

export default {
    name: "MailConversationListItemShadowMultiple",
    components: {
        BmRow,
        BmCol,
        BmIcon
    },
    props: {
        count: {
            type: Number,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.mail-conversation-list-item-shadow-multiple::before,
.mail-conversation-list-item-shadow-multiple::after {
    content: "";
    position: absolute;
    height: 100%;
    width: 100%;
    background-color: $surface-bg;
    outline: 1px solid $drag-color !important;
}

.mail-conversation-list-item-shadow-multiple.lot-of::before {
    left: -8px;
    top: -8px;
    z-index: -2;
}

.mail-conversation-list-item-shadow-multiple::after {
    top: -4px;
    left: -4px;
    z-index: -1;
}
</style>
