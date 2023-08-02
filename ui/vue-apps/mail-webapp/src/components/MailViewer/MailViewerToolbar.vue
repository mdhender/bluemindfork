<template>
    <bm-button-toolbar key-nav class="mail-viewer-toolbar bg-surface">
        <mail-open-in-popup-with-shift v-slot="action" :href="replyRoute(message)">
            <bm-icon-button
                variant="regular-accent"
                :size="size"
                :title="action.label($t('mail.content.reply.aria'))"
                icon="reply"
                @click.stop="action.execute(() => reply(message, conversation))"
            />
        </mail-open-in-popup-with-shift>
        <mail-open-in-popup-with-shift v-slot="action" :href="replyAllRoute(message)">
            <bm-icon-button
                variant="regular-accent"
                :size="size"
                :title="action.label($t('mail.content.reply_all.aria'))"
                icon="reply-all"
                @click.stop="action.execute(() => replyAll(message, conversation))"
            />
        </mail-open-in-popup-with-shift>
        <mail-open-in-popup-with-shift v-slot="action" :href="forwardRoute(message)">
            <bm-icon-button
                variant="regular-accent"
                :size="size"
                :title="action.label($t('common.forward'))"
                icon="forward"
                @click.stop="action.execute(() => forward(message))"
            />
        </mail-open-in-popup-with-shift>
        <mail-open-in-popup-with-shift v-if="isEventRequest" v-slot="action" :href="forwardEventRoute(message)">
            <bm-icon-button
                variant="regular-accent"
                :size="size"
                :title="action.label($t('event.forward'))"
                icon="forward"
                @click.stop="action.execute(() => forwardEvent(message))"
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
import { BmButtonToolbar, BmIconButton } from "@bluemind/ui-components";
import { mapState } from "vuex";
import { ReplyAndForwardRoutesMixin } from "~/mixins";
import MailViewerToolbarOtherActions from "./MailViewerToolbarOtherActions";
import MailOpenInPopupWithShift from "../MailOpenInPopupWithShift";
import { messageUtils } from "@bluemind/mail";
const { MessageHeader } = messageUtils;
import { useComposerInit } from "~/composables/composer/ComposerInit";

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
            default: undefined
        },
        size: {
            type: String,
            default: "md",
            validator: function (value) {
                return ["sm", "md"].includes(value);
            }
        }
    },
    setup() {
        const { initRelatedMessage } = useComposerInit(); // required by reply and replayAll from ReplyAndForwardRoutesMixin
        return { initRelatedMessage };
    },
    computed: {
        ...mapState("mail", { folders: "folders" }),
        isFolderReadOnly() {
            return !this.folders[this.message.folderRef.key].writable;
        },
        isEventRequest() {
            return this.message.headers.some(({ name }) => name === MessageHeader.X_BM_EVENT);
        }
    }
};
</script>

<style lang="scss" scoped>
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";
.mail-viewer-toolbar {
    bottom: 0;
    $nb-buttons: 4;
    @include until-lg {
        max-width: $nb-buttons * ($icon-btn-width-regular + $sp-6);
    }
}
</style>
