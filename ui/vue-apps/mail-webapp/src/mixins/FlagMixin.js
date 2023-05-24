import {
    MARK_FOLDER_AS_READ,
    MARK_CONVERSATIONS_AS_FLAGGED,
    MARK_CONVERSATIONS_AS_READ,
    MARK_CONVERSATIONS_AS_UNFLAGGED,
    MARK_CONVERSATIONS_AS_UNREAD
} from "~/actions";
import { mapActions, mapGetters, mapState } from "vuex";
import { Flag } from "@bluemind/email";
import SelectionMixin from "./SelectionMixin";
import {
    ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED,
    ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED,
    ALL_SELECTED_CONVERSATIONS_ARE_READ,
    ALL_SELECTED_CONVERSATIONS_ARE_UNREAD,
    ALL_CONVERSATIONS_ARE_SELECTED,
    CONVERSATIONS_ACTIVATED,
    CONVERSATION_LIST_FILTERED,
    CONVERSATION_LIST_IS_SEARCH_MODE,
    CURRENT_CONVERSATION_METADATA,
    CURRENT_MAILBOX,
    SEVERAL_CONVERSATIONS_SELECTED
} from "~/getters";

export default {
    mixins: [SelectionMixin],
    props: {
        conversation: {
            type: Object,
            default: undefined
        }
    },
    computed: {
        ...mapState("mail", {
            $_FlagMixin_folders: state => state.folders,
            $_FlagMixin_activeFolder: state => state.activeFolder
        }),
        ...mapGetters("mail", {
            $_FlagMixin_ALL_CONVERSATIONS_ARE_SELECTED: ALL_CONVERSATIONS_ARE_SELECTED,
            $_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED: ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED,
            $_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_READ: ALL_SELECTED_CONVERSATIONS_ARE_READ,
            $_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED: ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED,
            $_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_UNREAD: ALL_SELECTED_CONVERSATIONS_ARE_UNREAD,
            $_FlagMixin_CONVERSATION_LIST_FILTERED: CONVERSATION_LIST_FILTERED,
            $_FlagMixin_CONVERSATION_LIST_IS_SEARCH_MODE: CONVERSATION_LIST_IS_SEARCH_MODE,
            $_FlagMixin_CONVERSATIONS_ACTIVATED: CONVERSATIONS_ACTIVATED,
            $_FlagMixin_CURRENT_CONVERSATION_METADATA: CURRENT_CONVERSATION_METADATA,
            $_FlagMixin_CURRENT_MAILBOX: CURRENT_MAILBOX,
            $_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED: SEVERAL_CONVERSATIONS_SELECTED
        }),
        showMarkAsFlaggedInMain() {
            if (this.conversation) {
                return !this.conversation.flags.includes(Flag.FLAGGED);
            } else if (this.$_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED) {
                return this.$_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED;
            } else if (this.$_FlagMixin_CURRENT_CONVERSATION_METADATA) {
                return !this.$_FlagMixin_CURRENT_CONVERSATION_METADATA.flags.includes(Flag.FLAGGED);
            }
            return false;
        },
        showMarkAsFlaggedInOthers() {
            if (this.conversation) {
                return false;
            } else if (this.$_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED) {
                return (
                    !this.$_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED &&
                    !this.$_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED
                ); // mixed or indeterminate state
            }
            return false;
        },
        showMarkAsUnflaggedInMain() {
            if (this.conversation) {
                return this.conversation.flags.includes(Flag.FLAGGED);
            } else if (this.$_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED) {
                return this.$_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED;
            } else if (this.$_FlagMixin_CURRENT_CONVERSATION_METADATA) {
                return this.$_FlagMixin_CURRENT_CONVERSATION_METADATA.flags.includes(Flag.FLAGGED);
            }
            return false;
        },
        showMarkAsUnflaggedInOthers() {
            return this.showMarkAsFlaggedInOthers;
        }
    },
    methods: {
        ...mapActions("mail", {
            $_FlagMixin_markAsFlagged: MARK_CONVERSATIONS_AS_FLAGGED,
            $_FlagMixin_markAsRead: MARK_CONVERSATIONS_AS_READ,
            $_FlagMixin_markAsUnflagged: MARK_CONVERSATIONS_AS_UNFLAGGED,
            $_FlagMixin_markAsUnread: MARK_CONVERSATIONS_AS_UNREAD,
            $_FlagMixin_markFolderAsRead: MARK_FOLDER_AS_READ
        }),
        showMarkAsReadInMain(isTemplate = false) {
            if (isTemplate) {
                return false;
            } else if (this.conversation) {
                return !this.conversation.flags.includes(Flag.SEEN);
            } else if (this.$_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED) {
                return !this.$_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_READ;
            } else if (this.$_FlagMixin_CURRENT_CONVERSATION_METADATA) {
                return !this.$_FlagMixin_CURRENT_CONVERSATION_METADATA.flags.includes(Flag.SEEN);
            }
            return false;
        },
        showMarkAsReadInOthers(isTemplate = false) {
            if (isTemplate) {
                return this.showMarkAsReadInMain(false); // "|| this.showMarkAsReadInOthers(false)" is implied
            }
            return false;
        },
        showMarkAsUnreadInMain(isTemplate) {
            if (isTemplate) {
                return false;
            } else if (this.conversation) {
                return this.conversation.flags.includes(Flag.SEEN);
            } else if (this.$_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED) {
                return this.$_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_READ;
            } else if (this.$_FlagMixin_CURRENT_CONVERSATION_METADATA) {
                return this.$_FlagMixin_CURRENT_CONVERSATION_METADATA.flags.includes(Flag.SEEN);
            }
            return false;
        },
        showMarkAsUnreadInOthers(isTemplate) {
            if (isTemplate) {
                return this.showMarkAsUnreadInMain(false) || this.showMarkAsUnreadInOthers(false);
            } else if (this.conversation) {
                return false;
            } else if (this.$_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED) {
                return (
                    !this.$_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_READ &&
                    !this.$_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_UNREAD
                ); // mixed or indeterminate state
            }
            return false;
        },
        markAsRead(conversations) {
            if (
                !conversations &&
                this.$_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED &&
                this.$_FlagMixin_ALL_CONVERSATIONS_ARE_SELECTED &&
                !this.$_FlagMixin_CONVERSATION_LIST_FILTERED &&
                !this.$_FlagMixin_CONVERSATION_LIST_IS_SEARCH_MODE
            ) {
                return this.$_FlagMixin_markFolderAsRead({
                    folder: this.$_FlagMixin_folders[this.$_FlagMixin_activeFolder],
                    mailbox: this.$_FlagMixin_CURRENT_MAILBOX
                });
            } else {
                conversations = conversations || this.selected;
                return this.$_FlagMixin_markAsRead({
                    conversations,
                    conversationsActivated: this.$_FlagMixin_CONVERSATIONS_ACTIVATED,
                    mailbox: this.$_FlagMixin_CURRENT_MAILBOX
                });
            }
        },
        markAsUnread(conversations = this.selected) {
            return this.$_FlagMixin_markAsUnread({
                conversations,
                conversationsActivated: this.$_FlagMixin_CONVERSATIONS_ACTIVATED,
                mailbox: this.$_FlagMixin_CURRENT_MAILBOX
            });
        },
        markAsFlagged(conversations = this.selected) {
            return this.$_FlagMixin_markAsFlagged({
                conversations,
                conversationsActivated: this.$_FlagMixin_CONVERSATIONS_ACTIVATED,
                mailbox: this.$_FlagMixin_CURRENT_MAILBOX
            });
        },
        markAsUnflagged(conversations = this.selected) {
            return this.$_FlagMixin_markAsUnflagged({
                conversations,
                conversationsActivated: this.$_FlagMixin_CONVERSATIONS_ACTIVATED,
                mailbox: this.$_FlagMixin_CURRENT_MAILBOX
            });
        }
    }
};
