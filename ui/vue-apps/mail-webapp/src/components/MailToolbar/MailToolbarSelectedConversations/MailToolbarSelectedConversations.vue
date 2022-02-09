<template>
    <div class="mail-toolbar-selected-conversations">
        <template v-if="ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE">
            <bm-button
                v-show="isTemplate"
                variant="inline-light"
                class="unread btn-lg-simple-dark"
                :title="$t('mail.actions.edit_from_template.aria', { subject })"
                @click="editFromTemplate"
            >
                <bm-icon icon="plus-enveloppe" size="2x" />
                <span class="d-none d-lg-block"> {{ $t("mail.actions.edit_from_template") }}</span>
            </bm-button>
            <bm-button
                v-show="showMarkAsRead && !isTemplate"
                variant="inline-light"
                class="unread btn-lg-simple-dark"
                :title="markAsReadAriaText()"
                @click="markAsRead()"
            >
                <bm-icon icon="read" size="2x" />
                <span class="d-none d-lg-block">{{ markAsReadText }}</span>
            </bm-button>
            <bm-button
                v-show="showMarkAsUnread && !isTemplate"
                variant="inline-light"
                class="read btn-lg-simple-dark"
                :title="markAsUnreadAriaText()"
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
import { mapGetters, mapState } from "vuex";
import { BmButton, BmIcon } from "@bluemind/styleguide";
import MailToolbarSelectedConversationsMoveAction from "./MailToolbarSelectedConversationsMoveAction";
import MailToolbarSelectedConversationsOtherActions from "./MailToolbarSelectedConversationsOtherActions";
import { ActionTextMixin, FlagMixin, RemoveMixin, SelectionMixin } from "~/mixins";
import {
    ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE,
    CURRENT_CONVERSATION_METADATA,
    MY_DRAFTS,
    MY_TEMPLATES
} from "~/getters";
import { draftPath } from "~/model/draft";
import MessagePathParam from "~/router/MessagePathParam";
import { MessageCreationModes } from "~/model/message";

export default {
    name: "MailToolbarSelectedConversations",
    components: {
        BmButton,
        BmIcon,
        MailToolbarSelectedConversationsMoveAction,
        MailToolbarSelectedConversationsOtherActions
    },
    mixins: [ActionTextMixin, FlagMixin, SelectionMixin, RemoveMixin],
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
                params: { messagepath: draftPath(this.MY_DRAFTS) },
                query: { action: MessageCreationModes.EDIT_AS_NEW, message: MessagePathParam.build("", template) }
            });
        }
    }
};
</script>
