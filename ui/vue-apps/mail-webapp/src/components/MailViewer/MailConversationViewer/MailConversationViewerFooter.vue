<template>
    <bm-button-toolbar key-nav class="mail-conversation-viewer-footer">
        <mail-open-in-popup-with-shift v-slot="action" :href="replyRoute(lastNonDraft)">
            <bm-button
                variant="fill-accent"
                :title="action.label($t('mail.content.reply.aria'))"
                :icon="action.icon('reply')"
                @click="action.execute(() => reply(conversation, lastNonDraft))"
            >
                {{ $t("mail.content.reply.aria") }}
            </bm-button>
        </mail-open-in-popup-with-shift>
        <mail-open-in-popup-with-shift v-slot="action" :href="replyAllRoute(lastNonDraft)">
            <bm-button
                variant="fill-accent"
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
@import "~@bluemind/styleguide/css/mixins/_responsiveness.scss";
@import "~@bluemind/styleguide/css/_variables.scss";
@import "../_variables.scss";

.mail-conversation-viewer-footer {
    padding-top: $sp-4;
    padding-left: $conversation-main-padding-left;
    @include from-lg {
        padding-left: $conversation-main-padding-left-lg + $sp-7;
    }
    padding-bottom: $sp-7;
    gap: $sp-6;
}
</style>
