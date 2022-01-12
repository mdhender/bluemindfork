<template>
    <bm-button-toolbar key-nav class="mail-viewer-toolbar bg-surface">
        <bm-button
            variant="simple-primary"
            :aria-label="$t('mail.content.reply.aria')"
            :title="$t('mail.content.reply.aria')"
            @click="reply(conversation, message)"
        >
            <bm-icon icon="reply" size="2x" />
            <span class="d-lg-none">{{ $t("mail.content.reply.aria") }}</span>
        </bm-button>
        <bm-button
            variant="simple-primary"
            :aria-label="$t('mail.content.reply_all.aria')"
            :title="$t('mail.content.reply_all.aria')"
            @click="replyAll(conversation, message)"
        >
            <bm-icon icon="reply-all" size="2x" />
            <span class="d-lg-none">{{ $t("mail.content.reply_all.aria") }}</span>
        </bm-button>
        <bm-button
            variant="simple-primary"
            :aria-label="$t('common.forward')"
            :title="$t('common.forward')"
            @click="forward(message)"
        >
            <bm-icon icon="forward" size="2x" />
            <span class="d-lg-none">{{ $t("common.forward") }}</span>
        </bm-button>
        <mail-viewer-toolbar-other-actions v-if="!isFolderReadOnly" :message="message" :conversation="conversation" />
    </bm-button-toolbar>
</template>

<script>
import { BmButton, BmButtonToolbar, BmIcon } from "@bluemind/styleguide";
import { mapState } from "vuex";
import { ReplyAndForwardRoutesMixin } from "~/mixins";
import MailViewerToolbarOtherActions from "./MailViewerToolbarOtherActions";

export default {
    name: "MailViewerToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon,
        MailViewerToolbarOtherActions
    },
    mixins: [ReplyAndForwardRoutesMixin],
    props: {
        message: {
            type: Object,
            required: true
        },
        conversation: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", { folders: "folders" }),
        isFolderReadOnly() {
            return !this.folders[this.message.folderRef.key].writable;
        }
    }
};
</script>

<style lang="scss" scoped>
@import "~@bluemind/styleguide/css/_variables";
@media (max-width: map-get($grid-breakpoints, "lg")) {
    .mail-viewer-toolbar {
        bottom: 0;
        box-shadow: 0 -0.125rem 0.125rem rgba($dark, 0.25);
    }
}
</style>
