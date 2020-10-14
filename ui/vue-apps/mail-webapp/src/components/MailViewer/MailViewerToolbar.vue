<template>
    <bm-button-toolbar key-nav class="mail-viewer-toolbar mail-viewer-mobile-actions bg-white float-right">
        <bm-button
            variant="simple-primary"
            :aria-label="$t('mail.content.reply.aria')"
            :title="$t('mail.content.reply.aria')"
            @click="reply"
        >
            <bm-icon icon="reply" size="2x" />
            <span class="d-lg-none">{{ $t("mail.content.reply.aria") }}</span>
        </bm-button>
        <bm-button
            variant="simple-primary"
            :aria-label="$t('mail.content.reply_all.aria')"
            :title="$t('mail.content.reply_all.aria')"
            @click="replyAll"
        >
            <bm-icon icon="reply-all" size="2x" />
            <span class="d-lg-none">{{ $t("mail.content.reply_all.aria") }}</span>
        </bm-button>
        <bm-button
            variant="simple-primary"
            :aria-label="$t('mail.content.forward.aria')"
            :title="$t('mail.content.forward.aria')"
            @click="forward"
        >
            <bm-icon icon="forward" size="2x" />
            <span class="d-lg-none">{{ $t("mail.content.forward.aria") }}</span>
        </bm-button>
        <mail-viewer-toolbar-other-actions v-if="showOtherActions" :message="message" :conversation="conversation" />
    </bm-button-toolbar>
</template>

<script>
import { mapGetters } from "vuex";
import { BmButton, BmButtonToolbar, BmIcon } from "@bluemind/styleguide";

import { MessageCreationModes } from "~/model/message";
import { draftPath } from "~/model/draft";
import { MY_DRAFTS } from "~/getters";
import MessagePathParam from "~/router/MessagePathParam";
import MailViewerToolbarOtherActions from "./MailViewerToolbarOtherActions";

export default {
    name: "MailViewerToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon,
        MailViewerToolbarOtherActions
    },
    props: {
        message: {
            type: Object,
            required: false,
            default: null
        },
        conversation: {
            type: Object,
            required: false,
            default: null
        },
        showOtherActions: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS })
    },
    methods: {
        reply() {
            this.goTo(MessageCreationModes.REPLY, this.conversation);
        },
        replyAll() {
            this.goTo(MessageCreationModes.REPLY_ALL, this.conversation);
        },
        forward() {
            this.goTo(MessageCreationModes.FORWARD);
        },
        goTo(action, conversation) {
            if (conversation) {
                this.$router.navigate({
                    name: "v:mail:conversation",
                    params: { conversation, action, related: this.message }
                });
            } else {
                const messagepath = draftPath(this.MY_DRAFTS);
                const message = MessagePathParam.build("", this.message);
                this.$router.navigate({ name: "mail:message", params: { messagepath }, query: { action, message } });
            }
        }
    }
};
</script>
<style lang="scss" scoped>
@import "~@bluemind/styleguide/css/_variables";
@media (max-width: map-get($grid-breakpoints, "lg")) {
    .mail-viewer-mobile-actions {
        bottom: 0;
        box-shadow: 0 -0.125rem 0.125rem rgba($dark, 0.25);
        justify-content: space-evenly;
    }
}
</style>
