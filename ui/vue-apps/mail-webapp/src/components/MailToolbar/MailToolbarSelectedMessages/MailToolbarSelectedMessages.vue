<template>
    <div class="mail-toolbar-consult-message">
        <bm-button
            v-show="displayMarkAsRead"
            v-bm-tooltip.bottom
            variant="simple-dark"
            class="unread"
            :title="$tc('mail.actions.mark_read.aria', selection.length || 1)"
            :aria-label="$tc('mail.actions.mark_read.aria', selection.length || 1)"
            @click="doMarkAsRead"
        >
            <bm-icon icon="read" size="2x" />
            <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_read", selection.length || 1) }}</span>
        </bm-button>
        <bm-button
            v-show="displayMarkAsUnread"
            v-bm-tooltip.bottom
            variant="simple-dark"
            class="read"
            :title="$tc('mail.actions.mark_unread.aria', selection.length || 1)"
            :aria-label="$tc('mail.actions.mark_unread.aria', selection.length || 1)"
            @click="doMarkAsUnread"
        >
            <bm-icon icon="unread" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.mark_unread", selection.length || 1) }}</span>
        </bm-button>
        <mail-toolbar-selected-messages-move-action
            v-show="!MULTIPLE_MESSAGE_SELECTED && !selectionHasReadOnlyFolders"
        />
        <bm-button
            v-show="!selectionHasReadOnlyFolders"
            v-bm-tooltip.bottom
            variant="simple-dark"
            :title="$tc('mail.actions.remove.aria')"
            :aria-label="$tc('mail.actions.remove.aria')"
            @click.exact="remove"
            @click.shift.exact="purge"
        >
            <bm-icon icon="trash" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.remove") }}</span>
        </bm-button>
        <bm-button
            v-show="displayMarkAsFlagged"
            v-bm-tooltip.bottom
            variant="simple-dark"
            class="flagged"
            :title="$tc('mail.actions.mark_flagged.aria', selection.length)"
            :aria-label="$tc('mail.actions.mark_flagged.aria', selection.length)"
            @click="doMarkAsFlagged"
        >
            <bm-icon icon="flag-outline" size="2x" />
            <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_flagged") }}</span>
        </bm-button>
        <bm-button
            v-show="displayMarkAsUnflagged"
            v-bm-tooltip.bottom
            variant="simple-dark"
            class="unflagged"
            :title="$tc('mail.actions.mark_unflagged.aria', selection.length)"
            :aria-label="$tc('mail.actions.mark_unflagged.aria', selection.length)"
            @click="doMarkAsUnflagged"
        >
            <bm-icon icon="flag-fill" size="2x" class="text-warning" />
            <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_as_unflagged") }}</span>
        </bm-button>
        <mail-toolbar-selected-messages-other-actions v-if="!selectionHasReadOnlyFolders" />
    </div>
</template>

<script>
import { mapActions, mapGetters, mapState } from "vuex";
import { BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { ItemUri } from "@bluemind/item-uri";
import { Flag } from "@bluemind/email";
import MailToolbarSelectedMessagesMoveAction from "./MailToolbarSelectedMessagesMoveAction";
import MailToolbarSelectedMessagesOtherActions from "./MailToolbarSelectedMessagesOtherActions";
import {
    ALL_MESSAGES_ARE_SELECTED,
    ALL_SELECTED_MESSAGES_ARE_FLAGGED,
    ALL_SELECTED_MESSAGES_ARE_READ,
    ALL_SELECTED_MESSAGES_ARE_UNFLAGGED,
    ALL_SELECTED_MESSAGES_ARE_UNREAD,
    MESSAGE_LIST_FILTERED,
    MESSAGE_LIST_IS_SEARCH_MODE,
    MULTIPLE_MESSAGE_SELECTED,
    MY_TRASH
} from "~getters";

export default {
    name: "MailToolbarSelectedMessages",
    components: {
        BmButton,
        BmIcon,
        MailToolbarSelectedMessagesMoveAction,
        MailToolbarSelectedMessagesOtherActions
    },
    directives: { BmTooltip },
    computed: {
        ...mapGetters("mail-webapp", ["nextMessageKey"]),
        ...mapGetters("mail", {
            ALL_MESSAGES_ARE_SELECTED,
            ALL_SELECTED_MESSAGES_ARE_FLAGGED,
            ALL_SELECTED_MESSAGES_ARE_READ,
            ALL_SELECTED_MESSAGES_ARE_UNFLAGGED,
            ALL_SELECTED_MESSAGES_ARE_UNREAD,
            MULTIPLE_MESSAGE_SELECTED,
            MY_TRASH,
            MESSAGE_LIST_FILTERED,
            MESSAGE_LIST_IS_SEARCH_MODE
        }),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail", ["folders", "activeFolder", "messages", "selection"]),
        message() {
            return this.messages[this.currentMessageKey];
        },
        displayMarkAsRead() {
            if (this.MULTIPLE_MESSAGE_SELECTED) {
                return !this.ALL_SELECTED_MESSAGES_ARE_READ;
            } else {
                return !this.message.flags.includes(Flag.SEEN);
            }
        },
        displayMarkAsUnread() {
            if (this.MULTIPLE_MESSAGE_SELECTED) {
                return !this.ALL_SELECTED_MESSAGES_ARE_UNREAD;
            } else {
                return this.message.flags.includes(Flag.SEEN);
            }
        },
        displayMarkAsFlagged() {
            if (this.selectionHasReadOnlyFolders) {
                return false;
            } else if (this.MULTIPLE_MESSAGE_SELECTED) {
                return !this.ALL_SELECTED_MESSAGES_ARE_FLAGGED;
            } else {
                return !this.message.flags.includes(Flag.FLAGGED);
            }
        },
        displayMarkAsUnflagged() {
            if (this.selectionHasReadOnlyFolders) {
                return false;
            } else if (this.MULTIPLE_MESSAGE_SELECTED) {
                return !this.ALL_SELECTED_MESSAGES_ARE_UNFLAGGED;
            } else {
                return this.message.flags.includes(Flag.FLAGGED);
            }
        },
        selectionHasReadOnlyFolders() {
            const selection = this.selection.length ? this.selection : [this.currentMessageKey];
            return selection.some(messageKey => !this.folders[ItemUri.container(messageKey)].writable);
        }
    },
    methods: {
        ...mapActions("mail-webapp", {
            markAsUnread: "markAsUnread",
            markFolderAsRead: "markFolderAsRead",
            markMessagesAsFlagged: "markAsFlagged",
            markMessagesAsRead: "markAsRead",
            markMessagesAsUnflagged: "markAsUnflagged"
        }),
        async purge() {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$tc("mail.actions.purge.modal.content", this.selection.length || 1, {
                    subject: this.message && this.message.subject
                }),
                {
                    title: this.$tc("mail.actions.purge.modal.title", this.selection.length || 1),
                    okTitle: this.$t("common.delete"),
                    cancelVariant: "outline-secondary",
                    cancelTitle: this.$t("common.cancel"),
                    centered: true,
                    hideHeaderClose: false
                }
            );
            if (confirm) {
                // do this before followed async operations
                const nextMessageKey = this.nextMessageKey;
                this.$store.dispatch(
                    "mail-webapp/purge",
                    this.selection.length > 1 ? this.selection : this.currentMessageKey
                );
                if (!this.MULTIPLE_MESSAGE_SELECTED) {
                    this.$router.navigate({ name: "v:mail:message", params: { message: nextMessageKey } });
                }
            }
        },
        remove() {
            if (this.activeFolder === this.MY_TRASH.key) {
                this.purge();
            } else {
                // do this before followed async operations
                const nextMessageKey = this.nextMessageKey;
                this.$store.dispatch(
                    "mail-webapp/remove",
                    this.selection.length > 1 ? this.selection : this.currentMessageKey
                );
                if (!this.MULTIPLE_MESSAGE_SELECTED) {
                    this.$router.navigate({ name: "v:mail:message", params: { message: nextMessageKey } });
                }
            }
        },
        selectedKeys() {
            return this.MULTIPLE_MESSAGE_SELECTED ? this.selection : [this.currentMessageKey];
        },
        doMarkAsRead() {
            const areAllMessagesInFolderSelected =
                this.ALL_MESSAGES_ARE_SELECTED && !this.MESSAGE_LIST_FILTERED && !this.MESSAGE_LIST_IS_SEARCH_MODE;
            areAllMessagesInFolderSelected
                ? this.markFolderAsRead(this.activeFolder)
                : this.markMessagesAsRead(this.selectedKeys());
        },
        doMarkAsUnread() {
            if (this.MULTIPLE_MESSAGE_SELECTED) {
                this.markAsUnread(this.selection);
            } else {
                this.markAsUnread([this.currentMessageKey]);
            }
        },
        doMarkAsFlagged() {
            this.markMessagesAsFlagged(this.selectedKeys());
        },
        doMarkAsUnflagged() {
            this.markMessagesAsUnflagged(this.selectedKeys());
        }
    }
};
</script>
