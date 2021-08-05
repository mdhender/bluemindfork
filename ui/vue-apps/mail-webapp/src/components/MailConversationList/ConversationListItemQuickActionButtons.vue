<template>
    <bm-button-toolbar class="conversation-list-item-quick-action-buttons">
        <bm-button-group>
            <template v-if="folderOfMessage.writable">
                <bm-button
                    :aria-label="removeAriaText"
                    :title="removeAriaText"
                    class="p-1 mr-2"
                    variant="inline-secondary"
                    @click.shift.exact.prevent.stop="REMOVE_CONVERSATIONS([conversation])"
                    @click.exact.prevent.stop="MOVE_CONVERSATIONS_TO_TRASH([conversation])"
                >
                    <bm-icon icon="trash" size="lg" />
                </bm-button>
                <bm-button
                    v-if="showMarkAsRead"
                    class="p-1"
                    :aria-label="markAsReadAriaText"
                    :title="markAsReadAriaText"
                    variant="inline-secondary"
                    @click.prevent.stop="markAsRead(conversation)"
                >
                    <bm-icon icon="read" size="lg" />
                </bm-button>
                <bm-button
                    v-else
                    class="p-1"
                    :aria-label="markAsUnreadAriaText"
                    :title="markAsUnreadAriaText"
                    variant="inline-secondary"
                    @click.prevent.stop="markAsUnread(conversation)"
                >
                    <bm-icon icon="unread" size="lg" />
                </bm-button>
                <bm-button
                    v-if="showMarkAsFlagged"
                    class="p-1 ml-2"
                    :aria-label="markAsFlaggedAriaText"
                    :title="markAsFlaggedAriaText"
                    variant="inline-secondary"
                    @click.prevent.stop="markAsFlagged(conversation)"
                >
                    <bm-icon icon="flag-outline" size="lg" />
                </bm-button>
                <bm-button
                    v-else
                    class="p-1 ml-2"
                    :aria-label="markAsUnflaggedAriaText"
                    :title="markAsUnflaggedAriaText"
                    variant="inline-secondary"
                    @click.prevent.stop="markAsUnflagged(conversation)"
                >
                    <bm-icon class="text-warning" icon="flag-fill" size="lg" />
                </bm-button>
            </template>
        </bm-button-group>
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar, BmButtonGroup, BmButton, BmIcon } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import { ActionTextMixin, FlagMixin, RemoveMixin } from "~/mixins";
import { CONVERSATION_MESSAGE_BY_KEY, MY_SENT } from "~/getters";
import { removeSentDuplicates } from "~/model/conversations";

export default {
    name: "ConversationListItemQuickActionButtons",
    components: {
        BmButtonToolbar,
        BmButtonGroup,
        BmButton,
        BmIcon
    },
    mixins: [ActionTextMixin, FlagMixin, RemoveMixin],
    props: {
        conversation: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", ["folders"]),
        ...mapGetters("mail", {
            CONVERSATION_MESSAGE_BY_KEY,
            MY_SENT
        }),
        folderOfMessage() {
            return this.folders[this.conversation.folderRef.key];
        },
        conversationSize() {
            return removeSentDuplicates(this.CONVERSATION_MESSAGE_BY_KEY(this.conversation.key), this.MY_SENT).length;
        },
        selected() {
            return [this.conversation];
        }
    }
};
</script>
<style lang="scss">
.conversation-list-item-quick-action-buttons {
    .hovershadow:hover {
        box-shadow: 0px 2px 4px rgba(0, 0, 0, 0.25);
    }
}
</style>
