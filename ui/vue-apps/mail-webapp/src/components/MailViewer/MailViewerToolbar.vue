<template>
    <bm-button-toolbar
        key-nav
        class="mail-viewer-toolbar float-right mail-viewer-mobile-actions bg-white position-sticky"
    >
        <bm-button
            variant="simple-primary"
            :aria-label="$t('mail.content.reply.aria')"
            :title="$t('mail.content.reply.aria')"
            @click="initReplyOrForward(MessageCreationModes.REPLY, message)"
        >
            <bm-icon icon="reply" size="2x" />
            <span class="d-lg-none">{{ $t("mail.content.reply.aria") }}</span>
        </bm-button>
        <bm-button
            variant="simple-primary"
            :aria-label="$t('mail.content.reply_all.aria')"
            :title="$t('mail.content.reply_all.aria')"
            @click="initReplyOrForward(MessageCreationModes.REPLY_ALL, message)"
        >
            <bm-icon icon="reply-all" size="2x" />
            <span class="d-lg-none">{{ $t("mail.content.reply_all.aria") }}</span>
        </bm-button>
        <bm-button
            variant="simple-primary"
            :aria-label="$t('mail.content.forward.aria')"
            :title="$t('mail.content.forward.aria')"
            @click="initReplyOrForward(MessageCreationModes.FORWARD, message)"
        >
            <bm-icon icon="forward" size="2x" />
            <span class="d-lg-none">{{ $t("mail.content.forward.aria") }}</span>
        </bm-button>
    </bm-button-toolbar>
</template>

<script>
import { mapState } from "vuex";

import { BmButton, BmButtonToolbar, BmIcon } from "@bluemind/styleguide";

import { MessageCreationModes } from "~model/message";
import { ComposerInitMixin } from "~mixins";

export default {
    name: "MailViewerToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon
    },
    mixins: [ComposerInitMixin],
    data() {
        return {
            MessageCreationModes
        };
    },
    computed: {
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail", ["messages"]),
        message() {
            return this.messages[this.currentMessageKey];
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
