<template>
    <bm-toolbar class="mail-conversation-viewer-footer">
        <mail-open-in-popup-with-shift v-slot="action" :href="replyRoute(lastNonDraft)">
            <bm-toolbar-button
                variant="fill-accent"
                :title="action.label($t('mail.content.reply.aria'))"
                :icon="action.icon('reply')"
                @click="action.execute(() => reply(lastNonDraft, conversation))"
            >
                {{ $t("mail.content.reply.aria") }}
            </bm-toolbar-button>
        </mail-open-in-popup-with-shift>
        <mail-open-in-popup-with-shift v-slot="action" :href="replyAllRoute(lastNonDraft)">
            <bm-toolbar-button
                variant="fill-accent"
                :title="action.label($t('mail.content.reply_all.aria'))"
                :icon="action.icon('reply-all')"
                @click="action.execute(() => replyAll(lastNonDraft, conversation))"
            >
                {{ $t("mail.content.reply_all.aria") }}
            </bm-toolbar-button>
        </mail-open-in-popup-with-shift>
    </bm-toolbar>
</template>

<script>
import { mapState } from "vuex";
import { BmToolbar, BmToolbarButton } from "@bluemind/ui-components";
import { ReplyAndForwardRoutesMixin } from "~/mixins";
import MailOpenInPopupWithShift from "../../MailOpenInPopupWithShift";
import { useComposerInit } from "~/composables/composer/ComposerInit";

export default {
    name: "MailConversationViewerFooter",
    components: { BmToolbar, BmToolbarButton, MailOpenInPopupWithShift },
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
    setup() {
        const { initRelatedMessage } = useComposerInit(); // required by reply and replayAll from ReplyAndForwardRoutesMixin
        return { initRelatedMessage };
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
@import "~@bluemind/ui-components/src/css/utils/responsiveness.scss";
@import "~@bluemind/ui-components/src/css/utils/variables.scss";
@import "../variables.scss";

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
