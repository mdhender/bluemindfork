<template>
    <div class="mail-toolbar-selected-conversations">
        <template v-if="ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE">
            <bm-button
                v-show="showMarkAsRead"
                variant="inline-light"
                class="unread btn-lg-simple-dark"
                :title="markAsReadAriaText()"
                :aria-label="markAsReadAriaText()"
                @click="markAsRead()"
            >
                <bm-icon icon="read" size="2x" />
                <span class="d-none d-lg-block">{{ markAsReadText }}</span>
            </bm-button>
            <bm-button
                v-show="showMarkAsUnread"
                variant="inline-light"
                class="read btn-lg-simple-dark"
                :title="markAsUnreadAriaText()"
                :aria-label="markAsUnreadAriaText()"
                @click="markAsUnread()"
            >
                <bm-icon icon="unread" size="2x" />
                <span class="d-none d-lg-block">{{ markAsUnreadText }}</span>
            </bm-button>
            <mail-toolbar-selected-conversations-move-action />
            <bm-button
                variant="inline-light"
                class="btn-lg-simple-dark"
                :title="removeAriaText()"
                :aria-label="removeAriaText()"
                @click.exact="moveToTrash()"
                @click.shift.exact="remove()"
            >
                <bm-icon icon="trash" size="2x" />
                <span class="d-none d-lg-block">{{ removeText }}</span>
            </bm-button>
            <bm-button
                v-show="showMarkAsFlagged"
                variant="inline-light"
                class="flagged btn-lg-simple-dark"
                :title="markAsFlaggedAriaText()"
                :aria-label="markAsFlaggedAriaText()"
                @click="markAsFlagged()"
            >
                <bm-icon icon="flag-outline" size="2x" />
                <span class="d-none d-lg-block"> {{ markAsFlaggedText }}</span>
            </bm-button>
            <bm-button
                v-show="showMarkAsUnflagged"
                variant="inline-light"
                class="unflagged btn-lg-simple-dark"
                :title="markAsUnflaggedAriaText()"
                :aria-label="markAsUnflaggedAriaText()"
                @click="markAsUnflagged()"
            >
                <bm-icon icon="flag-fill" size="2x" class="text-warning" />
                <span class="d-none d-lg-block"> {{ markAsUnflaggedText }}</span>
            </bm-button>
            <mail-toolbar-selected-conversations-other-actions />
        </template>
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { BmButton, BmIcon } from "@bluemind/styleguide";
import MailToolbarSelectedConversationsMoveAction from "./MailToolbarSelectedConversationsMoveAction";
import MailToolbarSelectedConversationsOtherActions from "./MailToolbarSelectedConversationsOtherActions";
import { ActionTextMixin, FlagMixin, RemoveMixin } from "~/mixins";
import { ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE } from "~/getters";

export default {
    name: "MailToolbarSelectedConversations",
    components: {
        BmButton,
        BmIcon,
        MailToolbarSelectedConversationsMoveAction,
        MailToolbarSelectedConversationsOtherActions
    },
    mixins: [ActionTextMixin, FlagMixin, RemoveMixin],
    computed: {
        ...mapGetters("mail", { ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE })
    }
};
</script>
