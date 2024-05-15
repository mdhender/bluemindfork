<template>
    <bm-toolbar
        v-if="!CONVERSATION_LIST_DELETED_FILTER_ENABLED"
        key-nav
        class="mail-viewer-toolbar bg-surface"
        menu-icon-variant="regular-accent"
    >
        <mail-open-in-popup-with-shift v-slot="action" :href="replyRoute(message)">
            <bm-toolbar-icon-button
                variant="regular-accent"
                :size="size"
                :title="action.label($t('mail.content.reply.aria'))"
                icon="arrow-left-broken"
                @click.stop="action.execute(() => reply(message, conversation))"
            />
        </mail-open-in-popup-with-shift>
        <mail-open-in-popup-with-shift v-slot="action" :href="replyAllRoute(message)">
            <bm-toolbar-icon-button
                variant="regular-accent"
                :size="size"
                :title="action.label($t('mail.content.reply_all.aria'))"
                icon="arrows-left-broken"
                @click.stop="action.execute(() => replyAll(message, conversation))"
            />
        </mail-open-in-popup-with-shift>
        <forward-event-button v-if="isEventRequest" :message="message" :size="size">
            <mail-open-in-popup-with-shift v-slot="action" :href="forwardRoute(message)">
                <bm-dropdown-item
                    :icon="action.icon('forward')"
                    :title="action.label($t('mail.content.forward.message'))"
                    @click.stop="action.execute(() => forward(message))"
                >
                    {{ $t("mail.content.forward.message") }}
                </bm-dropdown-item>
            </mail-open-in-popup-with-shift>
        </forward-event-button>
        <mail-open-in-popup-with-shift v-else v-slot="action" :href="forwardRoute(message)">
            <bm-toolbar-icon-button
                variant="regular-accent"
                :size="size"
                :title="action.label($t('common.forward'))"
                icon="arrow-right"
                @click.stop="action.execute(() => forward(message))"
            />
        </mail-open-in-popup-with-shift>

        <template #menu>
            <mail-viewer-toolbar-other-actions
                v-if="!isFolderReadOnly"
                :size="size"
                :message="message"
                :conversation="conversation"
        /></template>
    </bm-toolbar>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { messageUtils, partUtils } from "@bluemind/mail";
import { BmToolbar, BmToolbarIconButton, BmDropdownItem } from "@bluemind/ui-components";
import { ReplyAndForwardRoutesMixin } from "~/mixins";
import { CONVERSATION_LIST_DELETED_FILTER_ENABLED } from "~/getters";
import MailViewerToolbarOtherActions from "./MailViewerToolbarOtherActions";
import { useComposerInit } from "~/composables/composer/ComposerInit";
import ForwardEventButton from "../../calendar/components/ForwardEventButton";
import MailOpenInPopupWithShift from "../MailOpenInPopupWithShift";

const { MessageHeader } = messageUtils;
const { hasCalendarPart } = partUtils;

export default {
    name: "MailViewerToolbar",
    components: {
        BmToolbar,
        BmToolbarIconButton,
        ForwardEventButton,
        BmDropdownItem,
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
        ...mapGetters("mail", { CONVERSATION_LIST_DELETED_FILTER_ENABLED }),
        isFolderReadOnly() {
            return !this.folders[this.message.folderRef.key].writable;
        },
        isEventRequest() {
            return (
                this.message.headers.some(({ name }) => name === MessageHeader.X_BM_EVENT) &&
                hasCalendarPart(this.message.structure)
            );
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
