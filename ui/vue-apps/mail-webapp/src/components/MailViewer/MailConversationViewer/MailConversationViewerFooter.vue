<template>
    <bm-button-toolbar key-nav class="mail-conversation-viewer-footer py-3">
        <bm-button
            variant="primary"
            :aria-label="$t('mail.content.reply.aria')"
            @click="reply(conversation, lastNonDraft)"
        >
            <div class="d-flex align-items-center">
                <bm-icon class="pr-1" icon="reply" size="2x" />
                {{ $t("mail.content.reply.aria") }}
            </div>
        </bm-button>
        <span class="pl-3" />
        <bm-button
            variant="primary"
            :aria-label="$t('mail.content.reply_all.aria')"
            @click="replyAll(conversation, lastNonDraft)"
        >
            <div class="d-flex align-items-center">
                <bm-icon class="pr-1" icon="reply-all" size="2x" />
                {{ $t("mail.content.reply_all.aria") }}
            </div>
        </bm-button>
    </bm-button-toolbar>
</template>

<script>
import { mapState } from "vuex";
import { BmButtonToolbar, BmButton, BmIcon } from "@bluemind/styleguide";
import { ReplyAndForwardRoutesMixin } from "~/mixins";

export default {
    name: "MailConversationViewerFooter",
    components: { BmButtonToolbar, BmButton, BmIcon },
    mixins: [ReplyAndForwardRoutesMixin],
    props: {
        lastNonDraft: {
            required: true,
            type: Object
        },
        conversationKey: {
            required: true,
            type: Number
        }
    },
    computed: {
        ...mapState("mail", { conversationByKey: ({ conversations }) => conversations.conversationByKey }),
        conversation() {
            return this.conversationByKey[this.conversationKey];
        }
    }
};
</script>

<style lang="scss">
.mail-conversation-viewer-footer {
    padding-left: 5.5rem;
}
</style>
