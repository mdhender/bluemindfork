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
            $_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED: SEVERAL_CONVERSATIONS_SELECTED,
            $_FlagMixin_CURRENT_CONVERSATION_METADATA: CURRENT_CONVERSATION_METADATA,
            $_FlagMixin_CONVERSATION_LIST_IS_SEARCH_MODE: CONVERSATION_LIST_IS_SEARCH_MODE,
            $_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED: ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED,
            $_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED: ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED,
            $_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_READ: ALL_SELECTED_CONVERSATIONS_ARE_READ,
            $_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_UNREAD: ALL_SELECTED_CONVERSATIONS_ARE_UNREAD,
            $_FlagMixin_ALL_CONVERSATIONS_ARE_SELECTED: ALL_CONVERSATIONS_ARE_SELECTED,
            $_FlagMixin_CURRENT_MAILBOX: CURRENT_MAILBOX
        }),
        showMarkAsRead() {
            if (this.conversation) {
                return !this.conversation.flags.includes(Flag.SEEN);
            } else if (this.$_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED) {
                return !this.$_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_READ;
            } else if (this.$_FlagMixin_CURRENT_CONVERSATION_METADATA) {
                return !this.$_FlagMixin_CURRENT_CONVERSATION_METADATA.flags.includes(Flag.SEEN);
            }
            return false;
        },
        showMarkAsUnread() {
            if (this.conversation) {
                return this.conversation.flags.includes(Flag.SEEN);
            } else if (this.$_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED) {
                return !this.$_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_UNREAD;
            } else if (this.$_FlagMixin_CURRENT_CONVERSATION_METADATA) {
                return this.$_FlagMixin_CURRENT_CONVERSATION_METADATA.flags.includes(Flag.SEEN);
            }
            return false;
        },
        showMarkAsFlagged() {
            if (this.conversation) {
                return !this.conversation.flags.includes(Flag.FLAGGED);
            } else if (this.$_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED) {
                return !this.$_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED;
            } else if (this.$_FlagMixin_CURRENT_CONVERSATION_METADATA) {
                return !this.$_FlagMixin_CURRENT_CONVERSATION_METADATA.flags.includes(Flag.FLAGGED);
            }
            return false;
        },
        showMarkAsUnflagged() {
            if (this.conversation) {
                return this.conversation.flags.includes(Flag.FLAGGED);
            } else if (this.$_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED) {
                return !this.$_FlagMixin_ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED;
            } else if (this.$_FlagMixin_CURRENT_CONVERSATION_METADATA) {
                return this.$_FlagMixin_CURRENT_CONVERSATION_METADATA.flags.includes(Flag.FLAGGED);
            }
            return false;
        }
        // $_FlagMixin_loopLimit() {
        //     return Math.min(this.selected.length, CHECK_FLAG_ITERATION_LIMIT);
        // }
    },
    methods: {
        ...mapActions("mail", {
            $_FlagMixin_markAsFlagged: MARK_CONVERSATIONS_AS_FLAGGED,
            $_FlagMixin_markAsRead: MARK_CONVERSATIONS_AS_READ,
            $_FlagMixin_markAsUnflagged: MARK_CONVERSATIONS_AS_UNFLAGGED,
            $_FlagMixin_markAsUnread: MARK_CONVERSATIONS_AS_UNREAD,
            $_FlagMixin_markFolderAsRead: MARK_FOLDER_AS_READ
        }),
        markAsRead(conversations) {
            if (
                !conversations &&
                this.$_FlagMixin_SEVERAL_CONVERSATIONS_SELECTED &&
                this.$_FlagMixin_ALL_CONVERSATIONS_ARE_SELECTED &&
                !this.$_FlagMixin_CONVERSATION_LIST_FILTERED &&
                !this.$_FlagMixin_CONVERSATION_LIST_IS_SEARCH_MODE
            ) {
                const folder = this.$_FlagMixin_folders[this.$_FlagMixin_activeFolder];
                return this.$_FlagMixin_markFolderAsRead({ folder, mailbox: this.$_FlagMixin_CURRENT_MAILBOX });
            } else {
                conversations = conversations || this.selected;
                return this.$_FlagMixin_markAsRead({ conversations });
            }
        },
        markAsUnread(conversations = this.selected) {
            return this.$_FlagMixin_markAsUnread({ conversations });
        },
        markAsFlagged(conversations = this.selected) {
            return this.$_FlagMixin_markAsFlagged({ conversations });
        },
        markAsUnflagged(conversations = this.selected) {
            return this.$_FlagMixin_markAsUnflagged({ conversations });
        }
    }
};
