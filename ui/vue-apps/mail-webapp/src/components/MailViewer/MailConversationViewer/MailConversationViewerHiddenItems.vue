<template>
    <div class="mail-conversation-viewer-hidden-items bg-light">
        <template v-if="index === 1">
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
                        @click="$emit('do-show-middle-messages')"
                    >
                        {{ $t("mail.conversation.show.middle.messages", { count }) }}
                    </bm-button>
                </div>
            </div>
            <div class="pl-5 row">
                <div class="col-1 vertical-line" />
                <div class="col spacer" />
            </div>
        </template>
    </div>
</template>
<script>
import { BmButton } from "@bluemind/styleguide";
import ConversationAvatar from "../../MailConversationList/ConversationAvatar";

export default {
    name: "MailConversationViewerHiddenItems",
    components: { BmButton, ConversationAvatar },
    props: {
        index: {
            type: Number,
            required: true
        },
        conversation: {
            type: Array,
            required: true
        },
        count: {
            type: Number,
            required: true
        }
    }
};
</script>
