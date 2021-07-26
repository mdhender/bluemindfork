<template>
    <div class="mail-conversation-viewer-hidden-items bg-light">
        <div class="pl-5 row">
            <div class="col-1 vertical-line" />
            <div class="col spacer" />
        </div>
        <div class="pl-5 row">
            <div class="col-1 text-center">
                <conversation-avatar :text="count" />
            </div>
            <div class="col pl-3 align-self-center">
                <bm-button
                    class="font-weight-bold text-secondary pl-0"
                    variant="link"
                    @click="$emit('do-show-hidden-messages')"
                >
                    {{ $t("mail.conversation.show.middle.messages", { count }) }}
                </bm-button>
            </div>
        </div>
        <div class="pl-5 row">
            <div class="col-1" :class="{ 'vertical-line': !isLast }" />
            <div class="col spacer" />
        </div>
    </div>
</template>
<script>
import { BmButton } from "@bluemind/styleguide";
import ConversationAvatar from "../../MailConversationList/ConversationAvatar";

export default {
    name: "MailConversationViewerHiddenItems",
    components: { BmButton, ConversationAvatar },
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
        }
    },
    computed: {
        isLast() {
            return this.index + this.count === this.conversationSize;
        }
    }
};
</script>
