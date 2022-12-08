<template>
    <bm-button-group>
        <template v-if="folder.writable">
            <bm-icon-button
                :aria-label="removeAriaText(1, subject)"
                :title="removeAriaText(1, subject)"
                variant="compact"
                icon="trash"
                @click.shift.exact.prevent.stop="REMOVE_CONVERSATIONS([conversation])"
                @click.exact.prevent.stop="MOVE_CONVERSATIONS_TO_TRASH([conversation])"
            />
            <bm-icon-button
                v-if="isTemplate"
                :aria-label="$tc('mail.actions.edit_from_template.aria')"
                :title="$tc('mail.actions.edit_from_template.aria')"
                variant="compact"
                icon="plus-enveloppe"
                @click.prevent.stop="editFromTemplate(conversation)"
            />
            <bm-icon-button
                v-if="showMarkAsReadInMain(isTemplate)"
                :aria-label="markAsReadAriaText(1, subject)"
                :title="markAsReadAriaText(1, subject)"
                variant="compact"
                icon="read"
                @click.prevent.stop="markAsRead(conversation)"
            />
            <bm-icon-button
                v-if="showMarkAsUnreadInMain(isTemplate)"
                :aria-label="markAsUnreadAriaText(1, subject)"
                :title="markAsUnreadAriaText(1, subject)"
                variant="compact"
                icon="unread"
                @click.prevent.stop="markAsUnread(conversation)"
            />
            <bm-icon-button
                v-if="showMarkAsFlaggedInMain"
                :aria-label="markAsFlaggedAriaText(1, subject)"
                :title="markAsFlaggedAriaText(1, subject)"
                variant="compact"
                icon="flag-outline"
                @click.prevent.stop="markAsFlagged(conversation)"
            />
            <bm-icon-button
                v-if="showMarkAsUnflaggedInMain"
                :aria-label="markAsUnflaggedAriaText(1, subject)"
                :title="markAsUnflaggedAriaText(1, subject)"
                variant="compact"
                icon="flag-fill"
                class="text-warning"
                @click.prevent.stop="markAsUnflagged(conversation)"
            />
        </template>
    </bm-button-group>
</template>

<script>
import { BmButtonGroup, BmIconButton } from "@bluemind/ui-components";
import { mapState, mapGetters } from "vuex";
import { messageUtils } from "@bluemind/mail";
import { ActionTextMixin, FlagMixin, RemoveMixin, MailRoutesMixin } from "~/mixins";
import { MY_DRAFTS, MY_TEMPLATES } from "~/getters";
import MessagePathParam from "~/router/MessagePathParam";

const { MessageCreationModes } = messageUtils;

export default {
    name: "ConversationListItemQuickActionButtons",
    components: {
        BmButtonGroup,
        BmIconButton
    },
    mixins: [ActionTextMixin, FlagMixin, RemoveMixin, MailRoutesMixin],
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
                params: { messagepath: this.draftPath(this.MY_DRAFTS) },
                query: { action: MessageCreationModes.EDIT_AS_NEW, message: MessagePathParam.build("", template) }
            });
        }
    }
};
</script>
