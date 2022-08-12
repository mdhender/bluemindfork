<template>
    <bm-button-toolbar key-nav class="mail-conversation-viewer-footer py-3">
        <mail-open-in-popup-with-shift v-slot="action" :href="replyRoute(lastNonDraft)">
            <bm-button
                variant="contained-accent"
                :title="action.label($t('mail.content.reply.aria'))"
                :icon="action.icon('reply')"
                @click="action.execute(() => reply(conversation, lastNonDraft))"
            >
                {{ $t("mail.content.reply.aria") }}
            </bm-button>
        </mail-open-in-popup-with-shift>
        <span class="pl-3" />
        <mail-open-in-popup-with-shift v-slot="action" :href="replyAllRoute(lastNonDraft)">
            <bm-button
                variant="contained-accent"
                :title="action.label($t('mail.content.reply_all.aria'))"
                :icon="action.icon('reply-all')"
                @click="action.execute(() => replyAll(conversation, lastNonDraft))"
            >
                {{ $t("mail.content.reply_all.aria") }}
            </bm-button>
        </mail-open-in-popup-with-shift>
    </bm-button-toolbar>
</template>

<script>
import { mapState } from "vuex";
import { BmButtonToolbar, BmButton } from "@bluemind/styleguide";
import { ReplyAndForwardRoutesMixin } from "~/mixins";
import MailOpenInPopupWithShift from "../../MailOpenInPopupWithShift";

export default {
    name: "MailConversationViewerFooter",
    components: { BmButtonToolbar, BmButton, MailOpenInPopupWithShift },
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
