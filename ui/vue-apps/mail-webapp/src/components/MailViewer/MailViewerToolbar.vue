<template>
    <bm-button-toolbar key-nav class="mail-viewer-toolbar bg-surface">
        <mail-open-in-popup-with-shift v-slot="action" :href="replyRoute(message)">
            <bm-icon-button
                variant="regular-accent"
                :size="size"
                :title="action.label($t('mail.content.reply.aria'))"
                icon="reply"
                @click="action.execute(() => reply(conversation, message))"
            />
        </mail-open-in-popup-with-shift>
        <mail-open-in-popup-with-shift v-slot="action" :href="replyAllRoute(message)">
            <bm-icon-button
                variant="regular-accent"
                :size="size"
                :title="action.label($t('mail.content.reply_all.aria'))"
                icon="reply-all"
                @click="action.execute(() => replyAll(conversation, message))"
            />
        </mail-open-in-popup-with-shift>
        <mail-open-in-popup-with-shift v-slot="action" :href="forwardRoute(message)">
            <bm-icon-button
                variant="regular-accent"
                :size="size"
                :title="action.label($t('common.forward'))"
                icon="forward"
                @click="action.execute(() => forward(message))"
            />
        </mail-open-in-popup-with-shift>
        <mail-viewer-toolbar-other-actions
            v-if="!isFolderReadOnly"
            :size="size"
            :message="message"
            :conversation="conversation"
        />
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar, BmIconButton } from "@bluemind/styleguide";
import { mapState } from "vuex";
import { ReplyAndForwardRoutesMixin } from "~/mixins";
import MailViewerToolbarOtherActions from "./MailViewerToolbarOtherActions";
import MailOpenInPopupWithShift from "../MailOpenInPopupWithShift";

export default {
    name: "MailViewerToolbar",
    components: {
        BmButtonToolbar,
        BmIconButton,
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
        },
        size: {
            type: String,
            default: "md",
            validator: function (value) {
                return ["sm", "md"].includes(value);
            }
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
        $nb-buttons: 4;
        max-width: $nb-buttons * ($icon-btn-width-regular + $sp-6);
    }
}
</style>
