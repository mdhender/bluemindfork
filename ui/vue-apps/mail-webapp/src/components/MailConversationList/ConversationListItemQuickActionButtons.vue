<template>
    <bm-button-group>
        <template v-if="folder.writable">
            <bm-button
                :aria-label="removeAriaText(1, subject)"
                :title="removeAriaText(1, subject)"
                class="p-1 mr-2 btn-no-hover-bg"
                variant="inline-neutral"
                @click.shift.exact.prevent.stop="REMOVE_CONVERSATIONS([conversation])"
                @click.exact.prevent.stop="MOVE_CONVERSATIONS_TO_TRASH([conversation])"
            >
                <bm-icon icon="trash" size="lg" />
            </bm-button>
            <bm-button
                v-if="isTemplate"
                class="p-1 btn-no-hover-bg"
                :aria-label="$tc('mail.actions.edit_from_template.aria')"
                :title="$tc('mail.actions.edit_from_template.aria')"
                variant="inline-neutral"
                @click.prevent.stop="editFromTemplate(conversation)"
            >
                <bm-icon icon="plus-enveloppe" size="lg" />
            </bm-button>
            <bm-button
                v-else-if="showMarkAsRead"
                class="p-1 btn-no-hover-bg"
                :aria-label="markAsReadAriaText(1, subject)"
                :title="markAsReadAriaText(1, subject)"
                variant="inline-neutral"
                @click.prevent.stop="markAsRead(conversation)"
            >
                <bm-icon icon="read" size="lg" />
            </bm-button>
            <bm-button
                v-else
                class="p-1 btn-no-hover-bg"
                :aria-label="markAsUnreadAriaText(1, subject)"
                :title="markAsUnreadAriaText(1, subject)"
                variant="inline-neutral"
                @click.prevent.stop="markAsUnread(conversation)"
            >
                <bm-icon icon="unread" size="lg" />
            </bm-button>
            <bm-button
                v-if="showMarkAsFlagged"
                class="p-1 ml-2 btn-no-hover-bg"
                :aria-label="markAsFlaggedAriaText(1, subject)"
                :title="markAsFlaggedAriaText(1, subject)"
                variant="inline-neutral"
                @click.prevent.stop="markAsFlagged(conversation)"
            >
                <bm-icon icon="flag-outline" size="lg" />
            </bm-button>
            <bm-button
                v-else
                class="p-1 ml-2 btn-no-hover-bg"
                :aria-label="markAsUnflaggedAriaText(1, subject)"
                :title="markAsUnflaggedAriaText(1, subject)"
                variant="inline-neutral"
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
import { MY_DRAFTS, MY_TEMPLATES } from "~/getters";
import { draftPath } from "~/model/draft";
import MessagePathParam from "~/router/MessagePathParam";
import { MessageCreationModes } from "../../model/message";

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
        ...mapState("mail", { folders: "folders", messages: state => state.conversations.messages }),
        ...mapGetters("mail", { MY_DRAFTS, MY_TEMPLATES }),
        selected() {
            return [this.conversation];
        },
        subject() {
            return this.conversation.subject;
        },
        folder() {
            return this.folders[this.conversation.folderRef.key];
        },
        isTemplate() {
            return this.folder.key === this.MY_TEMPLATES.key;
        }
    },
    methods: {
        editFromTemplate() {
            const template = this.messages[this.conversation.messages[0]];
            this.$router.navigate({
                name: "mail:message",
                params: { messagepath: draftPath(this.MY_DRAFTS) },
                query: { action: MessageCreationModes.EDIT_AS_NEW, message: MessagePathParam.build("", template) }
            });
        }
    }
};
</script>
