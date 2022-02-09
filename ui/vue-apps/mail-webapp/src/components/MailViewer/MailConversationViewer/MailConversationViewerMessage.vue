<template>
    <mail-conversation-viewer-item class="mail-conversation-viewer-message" v-bind="$props" v-on="$listeners">
        <bm-extension id="webapp.mail" path="viewer.header" :message="message" />
        <template slot="head">
            <div class="col pl-3 align-self-center">
                <bm-contact :contact="message.from" variant="no-avatar" />
                <mail-folder-icon
                    v-if="folder.key !== conversation.folderRef.key"
                    :shared="shared"
                    :folder="folder"
                    class="text-secondary font-italic pl-2"
                />
            </div>
            <div class="d-lg-none d-flex align-items-center">
                <mail-conversation-viewer-flags
                    class="z-index-250"
                    :class="{ 'pr-3': !isMessageExpanded }"
                    :message="message"
                />
                {{ $d(message.date, "full_date_time_short") }}
                <mail-viewer-toolbar-for-mobile
                    v-if="isMessageExpanded"
                    :message="message"
                    :conversation="conversation"
                    @shown="$emit('darken', true)"
                    @hidden="$emit('darken', false)"
                />
            </div>
            <div
                class="col d-none d-lg-flex justify-content-end align-items-center text-secondary"
                :class="{ 'pr-3': !isMessageExpanded }"
            >
                <mail-conversation-viewer-flags class="pr-2" :message="message" />
                {{ $d(message.date, "full_date_time_short") }}
                <mail-viewer-toolbar
                    v-if="isMessageExpanded && conversation"
                    :message="message"
                    :conversation="conversation"
                />
            </div>
        </template>
        <template slot="content">
            <div v-if="!isMessageExpanded" class="col pl-3 pb-2 pr-3 text-truncate">{{ message.preview }}...</div>
            <div v-else class="col pl-3 pb-2 pr-3">
                <body-viewer
                    v-if="MESSAGE_IS_LOADED(message.key)"
                    :message="message"
                    @remote-content="$emit('remote-content', message)"
                />
                <mail-viewer-content-loading v-else />
            </div>
        </template>
    </mail-conversation-viewer-item>
</template>
<script>
import { mapGetters, mapState } from "vuex";
import { BmExtension } from "@bluemind/extensions";
import { BmContact } from "@bluemind/styleguide";
import MailConversationViewerItem from "./MailConversationViewerItem";
import MailViewerContentLoading from "../../MailViewer/MailViewerContentLoading";
import MailViewerToolbar from "../MailViewerToolbar";
import MailViewerToolbarForMobile from "../MailViewerToolbarForMobile";
import MailConversationViewerFlags from "./MailConversationViewerFlags";
import MailConversationViewerItemMixin from "./MailConversationViewerItemMixin";
import BodyViewer from "../BodyViewer";
import { MESSAGE_IS_LOADED } from "~/getters";
import MailFolderIcon from "../../MailFolderIcon";
import { MailboxType } from "~/model/mailbox";

export default {
    name: "MailConversationViewerMessage",
    components: {
        BmContact,
        BmExtension,
        BodyViewer,
        MailFolderIcon,
        MailConversationViewerFlags,
        MailConversationViewerItem,
        MailViewerContentLoading,
        MailViewerToolbar,
        MailViewerToolbarForMobile
    },
    mixins: [MailConversationViewerItemMixin],
    computed: {
        ...mapGetters("mail", { MESSAGE_IS_LOADED }),
        ...mapState("mail", ["folders", "mailboxes"]),
        folder() {
            return this.folders[this.message.folderRef.key];
        },
        shared() {
            return this.mailboxes[this.folder.mailboxRef.key].type === MailboxType.MAILSHARE;
        }
    }
};
</script>

<style>
.mail-conversation-viewer-message .click-to-collapse-zone {
    cursor: pointer;
}
</style>
