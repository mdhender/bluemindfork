<template>
    <bm-button-toolbar
        key-nav
        class="mail-viewer-toolbar float-right mail-viewer-mobile-actions bg-white position-sticky"
    >
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
            :aria-label="$t('common.forward')"
            :title="$t('common.forward')"
            @click="forward"
        >
            <bm-icon icon="forward" size="2x" />
            <span class="d-lg-none">{{ $t("common.forward") }}</span>
        </bm-button>
    </bm-button-toolbar>
</template>

<script>
import { mapGetters, mapState } from "vuex";

import { BmButton, BmButtonToolbar, BmIcon } from "@bluemind/styleguide";

import { MessageCreationModes } from "~model/message";
import { draftPath } from "../../model/draft";
import { MY_DRAFTS } from "~getters";
import MessagePathParam from "../../router/MessagePathParam";
export default {
    name: "MailViewerToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS }),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail", ["messages"]),
        message() {
            return this.messages[this.currentMessageKey];
        }
    },
    methods: {
        reply() {
            this.goTo(MessageCreationModes.REPLY);
        },
        replyAll() {
            this.goTo(MessageCreationModes.REPLY_ALL);
        },
        forward() {
            this.goTo(MessageCreationModes.FORWARD);
        },
        goTo(action) {
            const messagepath = draftPath(this.MY_DRAFTS);
            const message = MessagePathParam.build("", this.messages[this.currentMessageKey]);
            this.$router.navigate({ name: "mail:message", params: { messagepath }, query: { action, message } });
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
