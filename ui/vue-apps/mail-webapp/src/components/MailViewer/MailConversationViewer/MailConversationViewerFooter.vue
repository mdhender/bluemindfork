<template>
    <bm-toolbar class="mail-conversation-viewer-footer">
        <div class="mail-conversation-viewer-footer-shadow">
            <mail-open-in-popup-with-shift v-slot="action" :href="replyRoute(lastNonDraft)">
                <bm-toolbar-button
                    variant="fill-accent"
                    :title="action.label($t('mail.content.reply.aria'))"
                    :icon="action.icon('arrow-left-broken')"
                    @click="action.execute(() => reply(lastNonDraft, conversation))"
                >
                    {{ $t("mail.content.reply.aria") }}
                </bm-toolbar-button>
            </mail-open-in-popup-with-shift>
            <mail-open-in-popup-with-shift v-slot="action" :href="replyAllRoute(lastNonDraft)">
                <bm-toolbar-button
                    variant="fill-accent"
                    :title="action.label($t('mail.content.reply_all.aria'))"
                    :icon="action.icon('arrows-left-broken')"
                    @click="action.execute(() => replyAll(lastNonDraft, conversation))"
                >
                    {{ $t("mail.content.reply_all.aria") }}
                </bm-toolbar-button>
            </mail-open-in-popup-with-shift>
        </div>
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
    .mail-conversation-viewer-footer-shadow {
        display: flex;
        width: 100%;
        height: 100%;
        margin-left: $sp-1;
        padding-top: $sp-4;
        padding-left: $conversation-padding-left;
        @include from-lg {
            padding-left: $conversation-padding-left-lg;
        }
        padding-bottom: $sp-4;
        gap: $sp-6;
        box-shadow: $box-shadow-sm;
    }
    background-color: $surface-hi1;
    bottom: 0;
    position: fixed !important;
    width: 100%;
    z-index: $zindex-sticky;
    overflow-x: clip;
}
</style>
