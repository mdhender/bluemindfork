<template>
    <mail-conversation-viewer-item class="mail-conversation-viewer-message" v-bind="$props" v-on="$listeners">
        <template slot="head">
            <bm-extension id="webapp.mail" path="viewer.header" :message="message" />
            <div class="conversation-viewer-message-head d-flex flex-fill justify-content-between align-items-start">
                <div class="d-flex align-self-center overflow-hidden no-wrap flex-fill">
                    <mail-viewer-from :message="message" no-avatar />
                    <mail-folder-icon
                        v-if="folder.key !== conversation.folderRef.key"
                        class="flex-fill"
                        variant="caption"
                        :mailbox="mailboxes[folder.mailboxRef.key]"
                        :folder="folder"
                        @click.native.stop="$emit('collapse')"
                    />
                </div>
                <div class="d-lg-none d-flex align-items-center h-100 text-nowrap">
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
                    class="d-none d-lg-flex justify-content-end align-items-center text-neutral text-nowrap h-100 pr-6"
                >
                    <mail-conversation-viewer-flags class="pr-2" :message="message" />
                    <div class="align-items-end pr-6" @click.stop="$emit('collapse')">
                        {{ $d(message.date, "full_date_time_short") }}
                    </div>
                    <mail-viewer-toolbar
                        v-if="isMessageExpanded && conversation"
                        class="flex-nowrap align-items-start"
                        size="sm"
                        :message="message"
                        :conversation="conversation"
                    />
                </div>
            </div>
        </template>
        <template slot="content">
            <div v-if="!isMessageExpanded" class="d-flex flex-fill align-items-center pb-2 pr-6">
                <message-icon class="mr-3" :message="message" />
                <div class="text-truncate">{{ message.preview }}</div>
            </div>
            <div v-else class="d-flex flex-fill pb-2 pr-6">
                <body-viewer
                    v-if="MESSAGE_IS_LOADED(message.key)"
                    class="flex-fill"
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
import { Contact } from "@bluemind/business-components";
import { BmExtension } from "@bluemind/extensions.vue";
import MailConversationViewerItem from "./MailConversationViewerItem";
import MailViewerContentLoading from "../MailViewerContentLoading";
import MailViewerFrom from "../MailViewerFrom";
import MailViewerToolbar from "../MailViewerToolbar";
import MailViewerToolbarForMobile from "../MailViewerToolbarForMobile";
import MailConversationViewerFlags from "./MailConversationViewerFlags";
import MailConversationViewerItemMixin from "./MailConversationViewerItemMixin";
import BodyViewer from "../BodyViewer";
import { MESSAGE_IS_LOADED } from "~/getters";
import MailFolderIcon from "../../MailFolderIcon";
import MessageIcon from "../../MessageIcon/MessageIcon";

export default {
    name: "MailConversationViewerMessage",
    components: {
        BmExtension,
        BodyViewer,
        MailConversationViewerFlags,
        MailConversationViewerItem,
        MailFolderIcon,
        MailViewerContentLoading,
        MailViewerFrom,
        MailViewerToolbar,
        MailViewerToolbarForMobile,
        MessageIcon
    },
    mixins: [MailConversationViewerItemMixin],
    data() {
        return { Contact };
    },
    computed: {
        ...mapGetters("mail", { MESSAGE_IS_LOADED }),
        ...mapState("mail", ["folders", "mailboxes"]),
        folder() {
            return this.folders[this.message.folderRef.key];
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables.scss";

.mail-conversation-viewer-message {
    .conversation-viewer-message-head {
        height: $input-height;
        .contact {
            flex-shrink: 10;
        }
        .mail-folder-icon {
            margin-left: $sp-3;
            margin-top: base-px-to-rem(2);
        }
    }
    .click-to-collapse-zone {
        cursor: pointer;
    }
}
</style>
