<template>
    <div class="mail-toolbar-selected-conversations">
        <template v-if="ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE">
            <mail-toolbar-responsive-button
                v-show="isTemplate"
                :title="$t('mail.actions.edit_from_template.aria', { subject })"
                icon="plus-enveloppe"
                :label="$t('mail.actions.edit_from_template')"
                @click="editFromTemplate"
            />
            <mail-toolbar-responsive-button
                v-show="showMarkAsReadInMain(isTemplate)"
                :title="markAsReadAriaText()"
                icon="read"
                :label="markAsReadText"
                @click="markAsRead()"
            />
            <mail-toolbar-responsive-button
                v-show="showMarkAsUnreadInMain(isTemplate)"
                :title="markAsUnreadAriaText()"
                icon="unread"
                :label="markAsUnreadText"
                @click="markAsUnread()"
            />
            <mail-toolbar-selected-conversations-move-action />
            <mail-toolbar-responsive-button
                :title="removeAriaText()"
                icon="trash"
                :label="removeText"
                @click.exact="moveToTrash()"
                @click.shift.exact="remove()"
            />
            <mail-toolbar-responsive-button
                v-show="showMarkAsFlaggedInMain"
                :title="markAsFlaggedAriaText()"
                icon="flag-fill"
                class="mark-as-flagged-btn"
                :label="markAsFlaggedText"
                @click="markAsFlagged()"
            />
            <mail-toolbar-responsive-button
                v-show="showMarkAsUnflaggedInMain"
                :title="markAsUnflaggedAriaText()"
                icon="flag-outline"
                :label="markAsUnflaggedText"
                @click="markAsUnflagged()"
            />
            <mail-toolbar-selected-conversations-other-actions />
        </template>
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { messageUtils } from "@bluemind/mail";
import MailToolbarResponsiveButton from "../MailToolbarResponsiveButton";
import MailToolbarSelectedConversationsMoveAction from "./MailToolbarSelectedConversationsMoveAction";
import MailToolbarSelectedConversationsOtherActions from "./MailToolbarSelectedConversationsOtherActions";
import { ActionTextMixin, FlagMixin, RemoveMixin, SelectionMixin, MailRoutesMixin } from "~/mixins";
import {
    ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE,
    CURRENT_CONVERSATION_METADATA,
    MY_DRAFTS,
    MY_TEMPLATES
} from "~/getters";
import MessagePathParam from "~/router/MessagePathParam";

const { MessageCreationModes } = messageUtils;

export default {
    name: "MailToolbarSelectedConversations",
    components: {
        MailToolbarResponsiveButton,
        MailToolbarSelectedConversationsMoveAction,
        MailToolbarSelectedConversationsOtherActions
    },
    mixins: [ActionTextMixin, FlagMixin, SelectionMixin, RemoveMixin, MailRoutesMixin],
    computed: {
        ...mapState("mail", { messages: state => state.conversations.messages }),
        ...mapGetters("mail", {
            ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE,
            CURRENT_CONVERSATION_METADATA,
            MY_DRAFTS,
            MY_TEMPLATES
        }),
        isTemplate() {
            if (this.selectionLength === 1) {
                return this.CURRENT_CONVERSATION_METADATA.folderRef.key === this.MY_TEMPLATES.key;
            }
            return false;
        },
        subject() {
            if (this.isTemplate) {
                return this.CURRENT_CONVERSATION_METADATA.subject;
            }
            return "";
        }
    },
    methods: {
        editFromTemplate() {
            const template = this.messages[this.CURRENT_CONVERSATION_METADATA.messages[0]];
            this.$router.navigate({
                name: "mail:message",
                params: { messagepath: this.draftPath(this.MY_DRAFTS) },
                query: { action: MessageCreationModes.EDIT_AS_NEW, message: MessagePathParam.build("", template) }
            });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-toolbar-selected-conversations {
    display: flex;
    flex-direction: row;

    .mark-as-flagged-btn {
        .bm-captioned-icon-button,
        .bm-icon-button {
            .bm-icon {
                color: $warning-fg;
            }
        }
    }
}
</style>
