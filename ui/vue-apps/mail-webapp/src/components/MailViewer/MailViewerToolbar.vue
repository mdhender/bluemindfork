<template>
    <bm-button-toolbar key-nav class="mail-viewer-toolbar bg-surface">
        <mail-open-in-popup-with-shift v-slot="action" :href="replyRoute(message)">
            <bm-button
                variant="simple-secondary"
                :title="action.label($t('mail.content.reply.aria'))"
                @click="action.execute(() => reply(conversation, message))"
            >
                <bm-icon icon="reply" size="2x" />
                <span class="d-lg-none">{{ $t("mail.content.reply.aria") }}</span>
            </bm-button>
        </mail-open-in-popup-with-shift>
        <mail-open-in-popup-with-shift v-slot="action" :href="replyAllRoute(message)">
            <bm-button
                variant="simple-secondary"
                :title="action.label($t('mail.content.reply_all.aria'))"
                @click="action.execute(() => replyAll(conversation, message))"
            >
                <bm-icon icon="reply-all" size="2x" />
                <span class="d-lg-none">{{ $t("mail.content.reply_all.aria") }}</span>
            </bm-button>
        </mail-open-in-popup-with-shift>
        <mail-open-in-popup-with-shift v-slot="action" :href="forwardRoute(message)">
            <bm-button
                variant="simple-secondary"
                :title="action.label($t('common.forward'))"
                @click="action.execute(() => forward(message))"
            >
                <bm-icon icon="forward" size="2x" />
                <span class="d-lg-none">{{ $t("common.forward") }}</span>
            </bm-button>
        </mail-open-in-popup-with-shift>
        <mail-viewer-toolbar-other-actions v-if="!isFolderReadOnly" :message="message" :conversation="conversation" />
    </bm-button-toolbar>
</template>

<script>
import { BmButton, BmButtonToolbar, BmIcon } from "@bluemind/styleguide";
import { mapState } from "vuex";
import { ReplyAndForwardRoutesMixin } from "~/mixins";
import MailViewerToolbarOtherActions from "./MailViewerToolbarOtherActions";
import MailOpenInPopupWithShift from "../MailOpenInPopupWithShift";

export default {
    name: "MailViewerToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon,
        MailViewerToolbarOtherActions,
        MailOpenInPopupWithShift
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
        box-shadow: 0 -0.125rem 0.125rem rgba($highest, 0.25);
    }
}
</style>
