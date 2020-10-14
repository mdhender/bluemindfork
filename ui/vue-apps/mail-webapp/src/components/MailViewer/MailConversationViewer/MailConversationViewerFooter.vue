<template>
    <div class="mail-conversation-viewer-footer py-3">
        <bm-button variant="primary" :aria-label="$t('mail.content.reply.aria')" @click="reply()">
            <div class="d-flex align-items-center">
                <bm-icon class="pr-1" icon="reply" size="2x" />
                {{ $t("mail.content.reply.aria") }}
            </div>
        </bm-button>
        <span class="pl-3" />
        <bm-button variant="primary" :aria-label="$t('mail.content.reply_all.aria')" @click="replyAll()">
            <div class="d-flex align-items-center">
                <bm-icon class="pr-1" icon="reply-all" size="2x" />
                {{ $t("mail.content.reply_all.aria") }}
            </div>
        </bm-button>
    </div>
</template>
<script>
import { BmButton, BmIcon } from "@bluemind/styleguide";
import { ComposerInitMixin } from "~/mixins";
import { MessageCreationModes } from "~/model/message";
import { draftPath } from "~/model/draft";
import MessagePathParam from "~/router/MessagePathParam";
import { MY_DRAFTS } from "~/getters";
import { mapGetters } from "vuex";

export default {
    name: "MailConversationViewerFooter",
    components: { BmButton, BmIcon },
    mixins: [ComposerInitMixin],
    props: {
        lastNonDraft: {
            required: true,
            type: Object
        }
    },
    data() {
        return { MessageCreationModes };
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS })
    },
    methods: {
        reply() {
            this.goTo(MessageCreationModes.REPLY);
        },
        replyAll() {
            this.goTo(MessageCreationModes.REPLY_ALL);
        },
        goTo(action) {
            const messagepath = draftPath(this.MY_DRAFTS);
            const message = MessagePathParam.build("", this.lastNonDraft);
            this.$router.navigate({ name: "mail:message", params: { messagepath }, query: { action, message } });
        }
    }
};
</script>
<style lang="scss">
.mail-conversation-viewer-footer {
    padding-left: 5.5rem;
}
</style>
