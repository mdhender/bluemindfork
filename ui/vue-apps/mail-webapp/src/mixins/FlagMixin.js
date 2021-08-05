import {
    MARK_FOLDER_AS_READ,
    MARK_CONVERSATIONS_AS_FLAGGED,
    MARK_CONVERSATIONS_AS_READ,
    MARK_CONVERSATIONS_AS_UNFLAGGED,
    MARK_CONVERSATIONS_AS_UNREAD
} from "~/actions";
import { mapActions, mapGetters } from "vuex";
import { Flag } from "@bluemind/email";
import SelectionMixin from "./SelectionMixin";
import { SEVERAL_CONVERSATIONS_SELECTED } from "~/getters";

export default {
    mixins: [SelectionMixin],
    props: {
        conversation: {
            type: Object,
            default: undefined
        }
    },
    computed: {
        ...mapGetters("mail", { SEVERAL_CONVERSATIONS_SELECTED }),
        showMarkAsRead() {
            return this.conversation
                ? !this.conversation.flags.includes(Flag.SEEN)
                : this.$_FlagMixin_atLeastOneSelectedHasNotFlag(Flag.SEEN);
        },
        showMarkAsUnread() {
            return this.conversation
                ? this.conversation.flags.includes(Flag.SEEN)
                : this.$_FlagMixin_atLeastOneSelectedHasFlag(Flag.SEEN);
        },
        showMarkAsFlagged() {
            return this.conversation
                ? !this.conversation.flags.includes(Flag.FLAGGED)
                : this.$_FlagMixin_atLeastOneSelectedHasNotFlag(Flag.FLAGGED);
        },
        showMarkAsUnflagged() {
            return this.conversation
                ? this.conversation.flags.includes(Flag.FLAGGED)
                : this.$_FlagMixin_atLeastOneSelectedHasFlag(Flag.FLAGGED);
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
        markAsRead(conversations) {
            if (
                !conversations &&
                this.SEVERAL_CONVERSATIONS_SELECTED &&
                this.ALL_CONVERSATIONS_ARE_SELECTED &&
                !this.CONVERSATION_LIST_FILTERED &&
                !this.CONVERSATION_LIST_IS_SEARCH_MODE
            ) {
                const folder = this.folders[this.activeFolder];
                const mailbox = this.mailboxes[folder.mailboxRef.key];
                return this.$_FlagMixin_markFolderAsRead({ folder, mailbox });
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
        },
        $_FlagMixin_atLeastOneSelectedHasFlag(flag) {
            return this.selected.some(c => c.flags.includes(flag));
        },
        $_FlagMixin_atLeastOneSelectedHasNotFlag(flag) {
            return this.selected.some(c => !c.flags.includes(flag));
        }
    }
};
