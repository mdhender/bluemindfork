<template>
    <div class="conversation-list-item-middle regular text-truncate">
        <div class="from-or-to-row">
            <div :title="fromOrTo" class="from-or-to text-truncate">
                <bm-extension id="webapp.mail" path="list.conversation.prefix" :conversation="conversation" />
                <span v-if="isDraft" class="text-danger font-weight-normal">
                    [<span class="font-italic">{{ $t("common.folder.draft") }}</span
                    >]
                </span>
                {{ fromOrTo }}
            </div>
            <div v-if="CONVERSATION_LIST_IS_SEARCH_MODE" class="d-flex slide">
                <mail-folder-icon variant="caption" :mailbox="mailboxes[folder.mailboxRef.key]" :folder="folder">
                    {{ folder.name }}
                </mail-folder-icon>
            </div>
            <div v-else class="widgets">
                <component :is="widget" v-for="widget in widgets" :key="widget.template" />
            </div>
        </div>
        <div class="subject-row">
            <div class="subject-and-count">
                <div :title="displayedSubject" class="text-truncate">
                    {{ displayedSubject }}
                </div>
                <span v-if="conversation && conversationSize > 1 && conversation.unreadCount > 0">
                    ({{ conversation.unreadCount }})
                </span>
            </div>
            <div v-if="!isMessageListStyleFull" class="displayed-date">
                <span class="d-none d-lg-block">
                    {{ displayedDate }}
                </span>
                <span class="d-block d-lg-none">
                    {{ smallerDisplayedDate }}
                </span>
            </div>
        </div>
        <div v-if="isMessageListStyleFull" class="preview-row">
            <div class="displayed-preview text-truncate" :title="displayedPreview">
                {{ displayedPreview }}
            </div>
            <div class="displayed-date">
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
import { mapGetters, mapState } from "vuex";
import { BmExtension } from "@bluemind/extensions.vue";
import { BmIcon } from "@bluemind/styleguide";
import { DateComparator } from "@bluemind/date";
import { Flag } from "@bluemind/email";
import { folderUtils } from "@bluemind/mail";
import MailFolderIcon from "../MailFolderIcon";
import {
    CONVERSATIONS_ACTIVATED,
    MY_DRAFTS,
    MY_SENT,
    CONVERSATION_LIST_IS_SEARCH_MODE,
    CONVERSATION_IS_SELECTED
} from "~/getters";

const { isDraftFolder } = folderUtils;

const FLAG_COMPONENT = {
    [Flag.FLAGGED]: {
        components: { BmIcon },
        template:
            '<bm-icon :aria-label="$t(\'mail.list.flagged.aria\')" aria-hidden="false" size="xs" class="text-warning" icon="flag-fill"/>',
        order: 3
    },
    [Flag.FORWARDED]: {
        components: { BmIcon },
        template:
            '<bm-icon :aria-label="$t(\'mail.list.forwarded.aria\')" aria-hidden="false" size="xs" icon="forward"/>',
        order: 1
    },
    [Flag.ANSWERED]: {
        components: { BmIcon },
        template: '<bm-icon :aria-label="$t(\'mail.list.replied.aria\')" aria-hidden="false" size="xs" icon="reply"/>',
        order: 2
    }
};

export default {
    name: "ConversationListItemMiddle",
    components: { BmExtension, BmIcon, MailFolderIcon },
    props: {
        conversation: {
            type: Object,
            required: true
        },
        isImportant: {
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
        isMessageListStyleFull() {
            return this.$store.state.settings.mail_message_list_style === "full";
        },
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
                const recipients = this.conversation.to?.length
                    ? this.conversation.to
                    : this.conversation.cc?.length
                    ? this.conversation.cc
                    : this.conversation.bcc?.length
                    ? this.conversation.bcc
                    : [];
                return recipients.map(to => (to.dn ? to.dn : to.address)).join(", ");
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

        /**
         * @return true if the sole message in non-conversation mode is in Drafts or at least one of the conversation's
         *  messages is in Drafts.
         */
        isDraft() {
            const firstMessage = this.messages[this.conversation.messages[0]];
            return (
                isDraftFolder(this.folders[firstMessage.folderRef.key].path) ||
                (this.$store.getters[`mail/${CONVERSATIONS_ACTIVATED}`] && this.isConversationWithDraft)
            );
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/type";
@import "~@bluemind/styleguide/css/variables";

.conversation-list-item-full,
.conversation-list-item-compact {
    .conversation-list-item-middle {
        gap: 0;
        .from-or-to-row {
            height: base-px-to-rem(20);
        }
        .subject-row,
        .preview-row {
            height: base-px-to-rem(16);
            @extend %caption;
        }
    }
}

.conversation-list-item-normal {
    .conversation-list-item-middle {
        gap: base-px-to-rem(1);
        .from-or-to-row {
            height: base-px-to-rem(24);
        }
    }
}

.conversation-list-item-middle {
    display: flex;
    flex-direction: column;

    .from-or-to-row {
        display: flex;
        align-items: center;
        gap: $sp-3;
        color: $neutral-fg-hi1;

        .from-or-to {
            flex: 1;
        }

        .widgets {
            display: flex;
            align-items: center;
            gap: $sp-3;
            height: base-px-to-rem(20);
        }
    }

    .subject-row {
        display: flex;
        align-items: center;
        gap: $sp-2;

        .subject-and-count {
            flex: 1;
            min-width: 0;
            color: $neutral-fg;
            display: flex;
        }
    }

    .preview-row {
        display: flex;
        align-items: center;
        gap: $sp-3;

        .displayed-preview {
            flex: 1;
            color: $neutral-fg-hi1;
        }
    }

    .displayed-date {
        flex: 0;
        color: $neutral-fg;
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
    .bm-extension-list-conversation-prefix {
        display: inline;
    }
}

.conversation-list-item.not-seen {
    .from-or-to,
    .subject-and-count {
        font-weight: $font-weight-bold;
    }
}
</style>
