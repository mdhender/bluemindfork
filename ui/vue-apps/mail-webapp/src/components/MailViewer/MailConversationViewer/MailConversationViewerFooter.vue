<template>
    <bm-button-toolbar key-nav class="mail-conversation-viewer-footer py-3">
        <mail-open-in-popup-with-shift v-slot="action" :href="replyRoute(lastNonDraft)">
            <bm-button
                variant="secondary"
                :title="action.label($t('mail.content.reply.aria'))"
                @click="action.execute(() => reply(conversation, lastNonDraft))"
            >
                <div class="d-flex align-items-center">
                    <bm-icon class="pr-1" :icon="action.icon('reply')" size="sm" />
                    {{ $t("mail.content.reply.aria") }}
                </div>
            </bm-button>
        </mail-open-in-popup-with-shift>
        <span class="pl-3" />
        <mail-open-in-popup-with-shift v-slot="action" :href="replyAllRoute(lastNonDraft)">
            <bm-button
                variant="secondary"
                :title="action.label($t('mail.content.reply_all.aria'))"
                @click="action.execute(() => replyAll(conversation, lastNonDraft))"
            >
                <div class="d-flex align-items-center">
                    <bm-icon class="pr-1" :icon="action.icon('reply-all')" size="sm" />
                    {{ $t("mail.content.reply_all.aria") }}
                </div>
            </bm-button>
        </mail-open-in-popup-with-shift>
    </bm-button-toolbar>
</template>

<script>
import { mapState } from "vuex";
import { BmButtonToolbar, BmButton, BmIcon } from "@bluemind/styleguide";
import { ReplyAndForwardRoutesMixin } from "~/mixins";
import MailOpenInPopupWithShift from "../../MailOpenInPopupWithShift";

export default {
    name: "MailConversationViewerFooter",
    components: { BmButtonToolbar, BmButton, BmIcon, MailOpenInPopupWithShift },
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
