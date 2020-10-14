<template>
    <div class="mail-toolbar-selected-conversations">
        <template v-if="!selectionHasReadOnlyFolders">
            <bm-button
                v-show="showMarkAsRead"
                variant="inline-light"
                class="unread btn-lg-simple-dark"
                :title="$tc('mail.actions.mark_read.aria', SELECTION_KEYS.length || 1)"
                :aria-label="$tc('mail.actions.mark_read.aria', SELECTION_KEYS.length || 1)"
                @click="markAsRead()"
            >
                <bm-icon icon="read" size="2x" />
                <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_read", SELECTION_KEYS.length || 1) }}</span>
            </bm-button>
            <bm-button
                v-show="showMarkAsUnread"
                variant="inline-light"
                class="read btn-lg-simple-dark"
                :title="$tc('mail.actions.mark_unread.aria', SELECTION_KEYS.length || 1)"
                :aria-label="$tc('mail.actions.mark_unread.aria', SELECTION_KEYS.length || 1)"
                @click="markAsUnread()"
            >
                <bm-icon icon="unread" size="2x" />
                <span class="d-none d-lg-block">{{ $tc("mail.actions.mark_unread", SELECTION_KEYS.length || 1) }}</span>
            </bm-button>
            <mail-toolbar-selected-conversations-move-action />
            <bm-button
                variant="inline-light"
                class="btn-lg-simple-dark"
                :title="$tc('mail.actions.remove.aria')"
                :aria-label="$tc('mail.actions.remove.aria')"
                @click.exact="moveToTrash()"
                @click.shift.exact="remove()"
            >
                <bm-icon icon="trash" size="2x" />
                <span class="d-none d-lg-block">{{ $tc("mail.actions.remove") }}</span>
            </bm-button>
            <bm-button
                v-show="showMarkAsFlagged"
                variant="inline-light"
                class="flagged btn-lg-simple-dark"
                :title="$tc('mail.actions.mark_flagged.aria', SELECTION_KEYS.length)"
                :aria-label="$tc('mail.actions.mark_flagged.aria', SELECTION_KEYS.length)"
                @click="markAsFlagged()"
            >
                <bm-icon icon="flag-outline" size="2x" />
                <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_flagged") }}</span>
            </bm-button>
            <bm-button
                v-show="showMarkAsUnflagged"
                variant="inline-light"
                class="unflagged btn-lg-simple-dark"
                :title="$tc('mail.actions.mark_unflagged.aria', SELECTION_KEYS.length)"
                :aria-label="$tc('mail.actions.mark_unflagged.aria', SELECTION_KEYS.length)"
                @click="markAsUnflagged()"
            >
                <bm-icon icon="flag-fill" size="2x" class="text-warning" />
                <span class="d-none d-lg-block"> {{ $tc("mail.actions.mark_as_unflagged") }}</span>
            </bm-button>
            <mail-toolbar-selected-conversations-other-actions />
        </template>
    </div>
</template>

<script>
import { BmButton, BmIcon } from "@bluemind/styleguide";
import MailToolbarSelectedConversationsMoveAction from "./MailToolbarSelectedConversationsMoveAction";
import MailToolbarSelectedConversationsOtherActions from "./MailToolbarSelectedConversationsOtherActions";
import { mapGetters } from "vuex";
import { FlagMixin, RemoveMixin, SelectionMixin } from "~/mixins";
import { SELECTION_KEYS } from "~/getters";

export default {
    name: "MailToolbarSelectedConversations",
    components: {
        BmButton,
        BmIcon,
        MailToolbarSelectedConversationsMoveAction,
        MailToolbarSelectedConversationsOtherActions
    },
    mixins: [FlagMixin, RemoveMixin, SelectionMixin],
    computed: {
        ...mapGetters("mail", { SELECTION_KEYS })
    }
};
</script>
