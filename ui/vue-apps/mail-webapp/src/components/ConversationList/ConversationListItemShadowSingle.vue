<template>
    <bm-row class="py-2 no-gutters align-items-center">
        <bm-col cols="1" class="text-right bm-drag-handle">
            <bm-icon icon="6dots-v" />
        </bm-col>
        <bm-col cols="2" class="pl-1">
            <bm-avatar :alt="from" />
        </bm-col>
        <bm-col cols="9" class="pr-1 text-overflow font-weight-bold">
            {{ conversation.subject }}
        </bm-col>
    </bm-row>
</template>
<script>
import { BmRow, BmCol, BmAvatar, BmIcon } from "@bluemind/ui-components";

export default {
    name: "ConversationListItemShadowSingle",
    components: {
        BmRow,
        BmCol,
        BmAvatar,
        BmIcon
    },
    props: {
        conversation: {
            type: Object,
            required: true
        }
    },
    computed: {
        from() {
            return this.conversation.from.dn ? this.conversation.from.dn : this.conversation.from.address;
        }
    }
};
</script>
