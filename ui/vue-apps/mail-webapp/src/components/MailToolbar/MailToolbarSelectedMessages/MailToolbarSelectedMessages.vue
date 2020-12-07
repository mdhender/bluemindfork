<template>
    <div class="mail-toolbar-consult-message">
        <bm-button
            v-show="displayMarkAsRead"
            variant="inline-light"
            class="unread btn-lg-simple-dark"
            :title="$tc('mail.actions.mark_read.aria', selection.length || 1)"
            :aria-label="$tc('mail.actions.mark_read.aria', selection.length || 1)"
            @click="MARK_AS_READ"
        >
            <bm-icon icon="read" size="2x" />
            <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_read", selection.length || 1) }}</span>
        </bm-button>
        <bm-button
            v-show="displayMarkAsUnread"
            variant="inline-light"
            class="read btn-lg-simple-dark"
            :title="$tc('mail.actions.mark_unread.aria', selection.length || 1)"
            :aria-label="$tc('mail.actions.mark_unread.aria', selection.length || 1)"
            @click="MARK_AS_UNREAD"
        >
            <bm-icon icon="unread" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.mark_unread", selection.length || 1) }}</span>
        </bm-button>
        <mail-toolbar-selected-messages-move-action
            v-show="!MULTIPLE_MESSAGE_SELECTED && !selectionHasReadOnlyFolders"
        />
        <bm-button
            v-show="!selectionHasReadOnlyFolders"
            variant="inline-light"
            class="btn-lg-simple-dark"
            :title="$tc('mail.actions.remove.aria')"
            :aria-label="$tc('mail.actions.remove.aria')"
            @click.exact="MOVE_MESSAGES_TO_TRASH(selected)"
            @click.shift.exact="REMOVE_MESSAGES(selected)"
        >
            <bm-icon icon="trash" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.remove") }}</span>
        </bm-button>
        <bm-button
            v-show="displayMarkAsFlagged"
            variant="inline-light"
            class="flagged btn-lg-simple-dark"
            :title="$tc('mail.actions.mark_flagged.aria', selection.length)"
            :aria-label="$tc('mail.actions.mark_flagged.aria', selection.length)"
            @click="MARK_AS_FLAGGED"
        >
            <bm-icon icon="flag-outline" size="2x" />
            <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_flagged") }}</span>
        </bm-button>
        <bm-button
            v-show="displayMarkAsUnflagged"
            variant="inline-light"
            class="unflagged btn-lg-simple-dark"
            :title="$tc('mail.actions.mark_unflagged.aria', selection.length)"
            :aria-label="$tc('mail.actions.mark_unflagged.aria', selection.length)"
            @click="MARK_AS_UNFLAGGED"
        >
            <bm-icon icon="flag-fill" size="2x" class="text-warning" />
            <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_as_unflagged") }}</span>
        </bm-button>
        <mail-toolbar-selected-messages-other-actions v-if="!selectionHasReadOnlyFolders" />
    </div>
</template>

<script>
import { mapActions, mapGetters, mapState } from "vuex";
import { BmButton, BmIcon } from "@bluemind/styleguide";
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
import {
    MARK_FOLDER_AS_READ,
    MARK_MESSAGES_AS_READ,
    MARK_MESSAGES_AS_UNREAD,
    MARK_MESSAGES_AS_FLAGGED,
    MARK_MESSAGES_AS_UNFLAGGED,
    MARK_MESSAGE_AS_READ,
    MARK_MESSAGE_AS_UNREAD,
    MARK_MESSAGE_AS_FLAGGED,
    MARK_MESSAGE_AS_UNFLAGGED
} from "~actions";
import RemoveMixin from "../../../store/mixins/RemoveMixin";

export default {
    name: "MailToolbarSelectedMessages",
    components: {
        BmButton,
        BmIcon,
        MailToolbarSelectedMessagesMoveAction,
        MailToolbarSelectedMessagesOtherActions
    },
    mixins: [RemoveMixin],
    computed: {
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
        ...mapState("mail", ["folders", "activeFolder", "messages", "selection", "mailboxes"]),
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
        selected() {
            return this.MULTIPLE_MESSAGE_SELECTED ? this.selection.map(key => this.messages[key]) : [this.message];
        },
        selectionHasReadOnlyFolders() {
            return this.selected.some(({ folderRef }) => !this.folders[folderRef.key].writable);
        }
    },
    methods: {
        ...mapActions("mail", {
            MARK_FOLDER_AS_READ,
            MARK_MESSAGES_AS_READ,
            MARK_MESSAGES_AS_UNREAD,
            MARK_MESSAGES_AS_FLAGGED,
            MARK_MESSAGES_AS_UNFLAGGED,
            MARK_MESSAGE_AS_READ,
            MARK_MESSAGE_AS_UNREAD,
            MARK_MESSAGE_AS_FLAGGED,
            MARK_MESSAGE_AS_UNFLAGGED
        }),
        MARK_AS_READ() {
            if (this.MULTIPLE_MESSAGE_SELECTED) {
                if (
                    this.ALL_MESSAGES_ARE_SELECTED &&
                    !this.MESSAGE_LIST_FILTERED &&
                    !this.MESSAGE_LIST_IS_SEARCH_MODE
                ) {
                    const folder = this.folders[this.activeFolder];
                    const mailbox = this.mailboxes[folder.mailboxRef.key];
                    return this.MARK_FOLDER_AS_READ({ folder, mailbox });
                } else {
                    return this.MARK_MESSAGES_AS_READ(this.selected);
                }
            } else {
                return this.MARK_MESSAGE_AS_READ(this.selected);
            }
        },
        MARK_AS_UNREAD() {
            if (this.MULTIPLE_MESSAGE_SELECTED) {
                return this.MARK_MESSAGES_AS_UNREAD(this.selected);
            } else {
                return this.MARK_MESSAGE_AS_UNREAD(this.selected);
            }
        },
        MARK_AS_FLAGGED() {
            if (this.MULTIPLE_MESSAGE_SELECTED) {
                return this.MARK_MESSAGES_AS_FLAGGED(this.selected);
            } else {
                return this.MARK_MESSAGE_AS_FLAGGED(this.selected);
            }
        },
        MARK_AS_UNFLAGGED() {
            if (this.MULTIPLE_MESSAGE_SELECTED) {
                return this.MARK_MESSAGES_AS_UNFLAGGED(this.selected);
            } else {
                return this.MARK_MESSAGE_AS_UNFLAGGED(this.selected);
            }
        }
    }
};
</script>
