<template>
    <div class="conversation-list-item-middle d-flex flex-column text-truncate">
        <div class="d-flex flex-row">
            <div :title="fromOrTo" class="mail-conversation-list-item-sender h3 text-dark text-truncate flex-fill">
                <span v-if="isDraft" class="text-danger font-weight-normal">
                    [<span class="font-italic">{{ $t("common.folder.draft") }}</span
                    >]
                </span>
                {{ fromOrTo }}
            </div>
            <div v-if="CONVERSATION_LIST_IS_SEARCH_MODE && !mouseIn" class="d-flex slide">
                <mail-folder-icon
                    class="text-secondary text-truncate"
                    :shared="isFolderOfMailshare(folder)"
                    :folder="folder"
                >
                    <i class="font-weight-bold">{{ folder.name }}</i>
                </mail-folder-icon>
            </div>
            <div v-else-if="!mouseIn" class="d-flex justify-content-end">
                <component :is="widget" v-for="widget in widgets" :key="widget.template" class="ml-2" />
            </div>
        </div>
        <div class="d-flex flex-row">
            <div class="d-flex flex-column flex-fill overflow-hidden">
                <div class="d-flex text-secondary">
                    <div :title="displayedSubject" class="mail-conversation-list-item-subject text-truncate">
                        {{ displayedSubject }}
                    </div>
                    <strong v-if="conversation && conversationSize > 1 && conversation.unreadCount > 0" class="pl-1">
                        ({{ conversation.unreadCount }})
                    </strong>
                </div>
                <div
                    :title="displayedPreview"
                    class="mail-conversation-list-item-preview text-dark text-condensed text-truncate"
                >
                    {{ displayedPreview }}
                </div>
            </div>
            <div v-show="!mouseIn" class="mail-conversation-list-item-date text-secondary align-self-end">
                <span class="d-none d-lg-block">
                    {{ displayedDate }}
                </span>
                <span class="d-block d-lg-none">
                    {{ smallerDisplayedDate }}
                </span>
            </div>
        </div>
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/styleguide";
import { DateComparator } from "@bluemind/date";
import { Flag } from "@bluemind/email";
import { mapGetters, mapState } from "vuex";
import MailFolderIcon from "../MailFolderIcon";
import { MailboxType } from "~/model/mailbox";
import { MY_DRAFTS, MY_SENT, CONVERSATION_LIST_IS_SEARCH_MODE, CONVERSATION_IS_SELECTED } from "~/getters";
import { isDraftFolder } from "~/model/folder";

const FLAG_COMPONENT = {
    [Flag.FLAGGED]: {
        components: { BmIcon },
        template:
            '<bm-icon :aria-label="$t(\'mail.list.flagged.aria\')" aria-hidden="false" class="text-warning" icon="flag-fill"/>',
        order: 3
    },
    [Flag.FORWARDED]: {
        components: { BmIcon },
        template: '<bm-icon :aria-label="$t(\'mail.list.forwarded.aria\')" aria-hidden="false" icon="forward"/>',
        order: 1
    },
    [Flag.ANSWERED]: {
        components: { BmIcon },
        template: '<bm-icon :aria-label="$t(\'mail.list.replied.aria\')" aria-hidden="false" icon="reply"/>',
        order: 2
    }
};

export default {
    name: "ConversationListItemMiddle",
    components: { BmIcon, MailFolderIcon },
    props: {
        conversation: {
            type: Object,
            required: true
        },
        isImportant: {
            type: Boolean,
            required: true
        },
        mouseIn: {
            type: Boolean,
            required: true
        },
        conversationSize: {
            type: Number,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", {
            MY_DRAFTS,
            MY_SENT,
            CONVERSATION_LIST_IS_SEARCH_MODE,
            CONVERSATION_IS_SELECTED
        }),
        ...mapState("mail", ["activeFolder", "folders", "mailboxes"]),
        ...mapState("mail", { messages: ({ conversations }) => conversations.messages }),
        ...mapState("session", { settings: ({ settings }) => settings.remote }),
        displayedDate: function () {
            const date = this.conversation.date;
            const today = new Date();
            if (DateComparator.isSameDay(date, today)) {
                return this.$d(date, "short_time");
            } else if (DateComparator.isSameYear(date, today)) {
                return this.$d(date, "relative_date");
            }
            return this.$d(date, "short_date");
        },
        smallerDisplayedDate: function () {
            return this.displayedDate.substring(this.displayedDate.indexOf(" ") + 1);
        },
        widgets() {
            return this.conversation.flags
                .map(flag => FLAG_COMPONENT[flag])
                .filter(widget => !!widget)
                .sort((a, b) => a.order - b.order);
        },
        folder() {
            return this.folders[this.conversation.folderRef.key];
        },
        fromOrTo() {
            const folder = this.conversation.folderRef.key;
            const isSentOrDraftBox = [this.MY_DRAFTS.key, this.MY_SENT.key].includes(folder);
            if (isSentOrDraftBox) {
                return this.conversation.to.map(to => (to.dn ? to.dn : to.address)).join(", ");
            } else {
                return this.conversation.from.dn ? this.conversation.from.dn : this.conversation.from.address;
            }
        },
        displayedSubject() {
            const subject = this.conversation.subject;
            if (!subject || subject.trim() === "") {
                return this.$t("mail.viewer.no.subject");
            } else {
                return subject;
            }
        },
        displayedPreview() {
            const preview = this.conversation.preview;
            if (!preview || preview.trim() === "") {
                return this.$t("mail.viewer.no.preview");
            } else {
                return preview;
            }
        },
        isConversationWithDraft() {
            return (
                this.conversation.messages.length > 1 &&
                this.conversation.messages.some(key =>
                    isDraftFolder(this.folders[this.messages[key].folderRef.key].path)
                )
            );
        },
        conversationsActivated() {
            return this.settings.mail_thread === "true" && this.folders[this.activeFolder].allowConversations;
        },

        /**
         * @return true if the sole message in non-conversation mode is in Drafts or at least one of the conversation's
         *  messages is in Drafts.
         */
        isDraft() {
            const firstMessage = this.messages[this.conversation.messages[0]];
            return (
                isDraftFolder(this.folders[firstMessage.folderRef.key].path) ||
                (this.conversationsActivated && this.isConversationWithDraft)
            );
        }
    },
    methods: {
        isFolderOfMailshare(folder) {
            return this.mailboxes[folder.mailboxRef.key].type === MailboxType.MAILSHARE;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.conversation-list-item-middle {
    .custom-control-label::after,
    .custom-control-label::before {
        top: 0.2rem !important;
    }

    .fade-out-leave-active {
        transition: opacity 0s linear 0.15s;
    }

    .fade-out-enter,
    .fade-out-leave-to {
        opacity: 0;
        position: absolute;
        right: $sp-3;
    }
}
</style>
