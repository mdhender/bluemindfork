<template>
    <mail-conversation-viewer-item class="mail-conversation-viewer-message" v-bind="$props" v-on="$listeners">
        <template slot="head">
            <div class="col pl-3">
                <span class="h3 font-weight-bold align-self-center">
                    {{ message.from ? message.from.dn || message.from.address : "" }}
                </span>
                <mail-folder-icon
                    v-if="folder.key !== conversation.folderRef.key"
                    :shared="shared"
                    :folder="folder"
                    class="text-secondary font-italic font-size-sm pl-2"
                />
            </div>
            <div class="d-lg-none d-flex align-items-center">
                <mail-conversation-viewer-flags
                    class="z-index-250"
                    :class="{ 'pr-3': !expandedMessages[index] }"
                    :message="message"
                />
                <mail-viewer-toolbar-for-mobile
                    v-if="expandedMessages[index]"
                    :message="message"
                    :conversation="conversation"
                    @shown="$emit('darken', true)"
                    @hidden="$emit('darken', false)"
                />
            </div>
            <div
                class="col d-none d-lg-flex justify-content-end align-items-center text-secondary"
                :class="{ 'pr-3': !expandedMessages[index] }"
            >
                <mail-conversation-viewer-flags class="pr-2" :message="message" />
                {{ $d(message.date, "full_date_time_short") }}
                <mail-viewer-toolbar
                    v-if="expandedMessages[index]"
                    :message="message"
                    :conversation="conversation"
                    show-other-actions
                />
            </div>
        </template>
        <template slot="content">
            <div v-if="!expandedMessages[index]" class="col pl-3 pb-2 pr-3 text-truncate">{{ message.preview }}...</div>
            <div v-else class="col pl-3 pb-2 pr-3">
                <mail-attachments-block
                    v-if="message.attachments && message.attachments.length > 0"
                    :message="message"
                />
                <body-viewer v-if="MESSAGE_IS_LOADED(message.key)" :message="message" />
                <mail-viewer-content-loading v-else />
            </div>
        </template>
        <template slot="to">
            {{ message.to ? message.to.map(to => to.dn || to.address).join(", ") : "" }}
        </template>
    </mail-conversation-viewer-item>
</template>
<script>
import { mapGetters, mapState } from "vuex";
import MailConversationViewerItem from "./MailConversationViewerItem";
import MailViewerContentLoading from "../../MailViewer/MailViewerContentLoading";
import MailViewerToolbar from "../MailViewerToolbar";
import MailViewerToolbarForMobile from "../MailViewerToolbarForMobile";
import MailConversationViewerFlags from "./MailConversationViewerFlags";
import MailConversationViewerItemMixin from "./MailConversationViewerItemMixin";
import BodyViewer from "../BodyViewer.vue";
import MailAttachmentsBlock from "../../MailAttachment/MailAttachmentsBlock";
import { MESSAGE_IS_LOADED } from "~/getters";
import MailFolderIcon from "../../MailFolderIcon";
import { MailboxType } from "~/model/mailbox";

export default {
    name: "MailConversationViewerMessage",
    components: {
        BodyViewer,
        MailAttachmentsBlock,
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
