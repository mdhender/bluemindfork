<template>
    <mail-conversation-viewer-item
        class="mail-conversation-viewer-draft draft"
        v-bind="$props"
        :is-draft="true"
        v-on="$listeners"
    >
        <template slot="head">
            <div class="pl-3 align-self-center z-index-110">
                <span class="text-danger">
                    [<span class="font-italic">{{ $t("common.folder.draft") }}</span
                    >]
                </span>
                <span class="text-alternate-light d-none d-lg-inline-block">{{ formattedDraftSaveDate }}</span>
            </div>
            <div class="col d-flex justify-content-end align-items-center text-secondary">
                <mail-viewer-draft-toolbar-for-mobile
                    class="d-lg-none"
                    :conversation="conversation"
                    :message="message"
                    @shown="$emit('darken', true)"
                    @hidden="$emit('darken', false)"
                    @edit="$emit('darken', false)"
                />
                <mail-viewer-draft-toolbar class="d-none d-lg-flex" :conversation="conversation" :message="message" />
            </div>
        </template>
        <template slot="subhead">
            <div class="row pl-5 d-lg-none">
                <div
                    class="col-1 vertical-line vertical-line-after-avatar"
                    :class="{ 'vertical-line-transparent': index === conversation.messages.length - 1 }"
                />
                <span class="pl-3 text-alternate-light">{{ formattedDraftSaveDate }}</span>
            </div>
        </template>
        <template slot="to">
            <span>{{ $t("common.to") }}</span>
            {{ message.to.map(to => to.dn || to.address).join(", ") }}
        </template>
        <template slot="content">
            <div v-if="!isMessageExpanded" class="col pl-3 pb-2 pr-3 text-truncate">{{ message.preview }}...</div>
            <div v-else class="col pl-3 pb-2 pr-3">
                <mail-attachments-block v-if="message.attachments.length > 0" :message="message" />
                <body-viewer :message="message" collapse-quoted />
            </div>
        </template>
        <template slot="bottom">
            <span class="col pl-3 pb-2 text-danger font-italic">{{ $t("mail.draft.not.sent") }}</span>
        </template>
    </mail-conversation-viewer-item>
</template>
<script>
import MailConversationViewerItem from "./MailConversationViewerItem";
import MailViewerDraftToolbar from "../MailViewerDraftToolbar";
import MailViewerDraftToolbarForMobile from "../MailViewerDraftToolbarForMobile";
import MailConversationViewerItemMixin from "./MailConversationViewerItemMixin";
import { FormattedDateMixin } from "~/mixins";
import BodyViewer from "../BodyViewer.vue";
import MailAttachmentsBlock from "../../MailAttachment/MailAttachmentsBlock";

export default {
    name: "MailConversationViewerDraft",
    components: {
        BodyViewer,
        MailAttachmentsBlock,
        MailConversationViewerItem,
        MailViewerDraftToolbar,
        MailViewerDraftToolbarForMobile
    },
    mixins: [FormattedDateMixin, MailConversationViewerItemMixin],
    computed: {
        formattedDraftSaveDate() {
            const formatted = this.formatDraftSaveDate(this.message);
            return formatted.time ? this.$t("mail.save.date.time", formatted) : this.$t("mail.save.date", formatted);
        }
    }
};
</script>

<style>
.mail-conversation-viewer-draft .click-to-collapse-zone {
    cursor: pointer;
}
</style>
