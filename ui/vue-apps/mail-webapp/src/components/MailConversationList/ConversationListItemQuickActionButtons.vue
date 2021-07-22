<template>
    <bm-button-group>
        <template v-if="folderOfMessage.writable">
            <bm-button
                :aria-label="removeAriaText(1, subject)"
                :title="removeAriaText(1, subject)"
                class="p-1 mr-2"
                variant="inline-secondary"
                @click.shift.exact.prevent.stop="REMOVE_CONVERSATIONS([conversation])"
                @click.exact.prevent.stop="MOVE_CONVERSATIONS_TO_TRASH([conversation])"
            >
                <bm-icon icon="trash" size="lg" />
            </bm-button>
            <bm-button
                v-if="showMarkAsRead && !isTemplate"
                class="p-1"
                :aria-label="markAsReadAriaText(1, subject)"
                :title="markAsReadAriaText(1, subject)"
                variant="inline-secondary"
                @click.prevent.stop="markAsRead(conversation)"
            >
                <bm-icon icon="read" size="lg" />
            </bm-button>
            <bm-button
                v-else-if="!isTemplate"
                class="p-1"
                :aria-label="markAsUnreadAriaText(1, subject)"
                :title="markAsUnreadAriaText(1, subject)"
                variant="inline-secondary"
                @click.prevent.stop="markAsUnread(conversation)"
            >
                <bm-icon icon="unread" size="lg" />
            </bm-button>
            <bm-button
                v-if="showMarkAsFlagged"
                class="p-1 ml-2"
                :aria-label="markAsFlaggedAriaText(1, subject)"
                :title="markAsFlaggedAriaText(1, subject)"
                variant="inline-secondary"
                @click.prevent.stop="markAsFlagged(conversation)"
            >
                <bm-icon icon="flag-outline" size="lg" />
            </bm-button>
            <bm-button
                v-else
                class="p-1 ml-2"
                :aria-label="markAsUnflaggedAriaText(1, subject)"
                :title="markAsUnflaggedAriaText(1, subject)"
                variant="inline-secondary"
                @click.prevent.stop="markAsUnflagged(conversation)"
            >
                <bm-icon class="text-warning" icon="flag-fill" size="lg" />
            </bm-button>
        </template>
    </bm-button-group>
</template>

<script>
import { BmButtonGroup, BmButton, BmIcon } from "@bluemind/styleguide";
import { mapState, mapGetters } from "vuex";
import { ActionTextMixin, FlagMixin, RemoveMixin } from "~/mixins";
import { MY_TEMPLATES } from "~getters";

export default {
    name: "ConversationListItemQuickActionButtons",
    components: {
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
        ...mapGetters("mail", { MY_TEMPLATES }),
        selected() {
            return [this.conversation];
        },
        subject() {
            return this.conversation.subject;
        },
        folder() {
            return this.folders[this.message.folderRef.key];
        },
        isTemplate() {
            return this.folder.key === this.MY_TEMPLATES.key;
        }
    }
};
</script>
