<template>
    <div class="mail-conversation-viewer-hidden-items">
        <div class="conversation-viewer-row d-flex">
            <div v-if="isUserPrefChronologicalOrder || index > 0" class="vertical-line" />
            <div class="col spacer" />
        </div>
        <div class="conversation-viewer-row d-flex align-items-center item-count-and-expand-button">
            <bm-avatar :count="count" />
            <div class="col pl-5">
                <bm-button variant="link" @click="$emit('do-show-hidden-messages')">
                    {{ $t("mail.conversation.show.middle.messages", { count }) }}
                </bm-button>
            </div>
        </div>
        <div class="conversation-viewer-row d-flex">
            <div :class="{ 'vertical-line': !isLast }" />
            <div class="col spacer" />
        </div>
    </div>
</template>
<script>
import { BmButton, BmAvatar } from "@bluemind/ui-components";

export default {
    name: "MailConversationViewerHiddenItems",
    components: { BmButton, BmAvatar },
    props: {
        count: {
            type: Number,
            required: true
        },
        conversationSize: {
            type: Number,
            required: true
        },
        index: {
            type: Number,
            required: true
        },
        isUserPrefChronologicalOrder: {
            type: Boolean,
            required: false,
            default: true
        }
    },
    computed: {
        isLast() {
            return this.index + this.count === this.conversationSize;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-conversation-viewer-hidden-items {
    background-color: $neutral-bg;

    .item-count-and-expand-button {
        height: base-px-to-rem(42);
    }
}
</style>
