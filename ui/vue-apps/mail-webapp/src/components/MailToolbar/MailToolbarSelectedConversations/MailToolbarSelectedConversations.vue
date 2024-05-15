<template>
    <bm-toolbar class="mail-toolbar-selected-conversations" :class="{ compact }">
        <template v-if="ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE && !CONVERSATION_LIST_DELETED_FILTER_ENABLED">
            <mail-toolbar-responsive-button
                v-if="isTemplate"
                :title="$t('mail.actions.edit_from_template.aria', { subject })"
                icon="mail-plus"
                :label="$t('mail.actions.edit_from_template')"
                :compact="compact"
                @click="editFromTemplate"
            />
            <mail-toolbar-responsive-button
                v-if="showMarkAsReadInMain(isTemplate)"
                :title="markAsReadAriaText()"
                icon="read"
                :label="markAsReadText"
                :compact="compact"
                @click="markAsRead()"
            />
            <mail-toolbar-responsive-button
                v-if="showMarkAsUnreadInMain(isTemplate)"
                :title="markAsUnreadAriaText()"
                icon="mail-dot"
                :label="markAsUnreadText"
                :compact="compact"
                @click="markAsUnread()"
            />
            <mail-toolbar-selected-conversations-move-action :compact="compact" />
            <mail-toolbar-responsive-button
                :title="removeAriaText()"
                icon="trash"
                :label="removeText"
                :compact="compact"
                @click.exact="moveToTrash()"
                @click.shift.exact="remove()"
            />
            <mail-toolbar-responsive-button
                v-if="showMarkAsFlaggedInMain"
                :title="markAsFlaggedAriaText()"
                icon="flag"
                :label="$t('mail.state.flagging')"
                :compact="compact"
                @click="markAsFlagged()"
            />
            <mail-toolbar-responsive-button
                v-if="showMarkAsUnflaggedInMain"
                :title="markAsUnflaggedAriaText()"
                icon="flag-fill"
                class="mark-as-unflagged-btn"
                :label="$t('mail.state.flagging')"
                :compact="compact"
                @click="markAsUnflagged()"
            />
        </template>
        <template v-else-if="CONVERSATION_LIST_DELETED_FILTER_ENABLED">
            <mail-toolbar-responsive-button
                :title="unexpungeAriaText()"
                icon="clock-rewind"
                :label="unexpungeText"
                :compact="compact"
                @click="unexpunge()"
            />
        </template>

        <template #menu-button>
            <mail-toolbar-menu-button :compact="compact" />
        </template>
        <template #menu>
            <mail-toolbar-selected-conversations-other-actions
                v-if="ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE && !CONVERSATION_LIST_DELETED_FILTER_ENABLED"
            />
        </template>
    </bm-toolbar>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { messageUtils } from "@bluemind/mail";
import { BmToolbar } from "@bluemind/ui-components";
import MailToolbarResponsiveButton from "../MailToolbarResponsiveButton";
import MailToolbarSelectedConversationsMoveAction from "./MailToolbarSelectedConversationsMoveAction";
import { ActionTextMixin, FlagMixin, RemoveMixin, SelectionMixin, MailRoutesMixin } from "~/mixins";
import {
    ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE,
    CONVERSATION_LIST_DELETED_FILTER_ENABLED,
    CURRENT_CONVERSATION_METADATA,
    MY_DRAFTS,
    MY_TEMPLATES
} from "~/getters";
import MessagePathParam from "~/router/MessagePathParam";
import MailToolbarSelectedConversationsOtherActions from "./MailToolbarSelectedConversationsOtherActions";
import MailToolbarMenuButton from "../MailToolbarMenuButton";

const { MessageCreationModes } = messageUtils;

export default {
    name: "MailToolbarSelectedConversations",
    components: {
        BmToolbar,
        MailToolbarResponsiveButton,
        MailToolbarSelectedConversationsMoveAction,
        MailToolbarSelectedConversationsOtherActions,
        MailToolbarMenuButton
    },
    mixins: [ActionTextMixin, FlagMixin, SelectionMixin, RemoveMixin, MailRoutesMixin],
    props: {
        compact: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        ...mapState("mail", { messages: state => state.conversations.messages }),
        ...mapGetters("mail", {
            ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE,
            CONVERSATION_LIST_DELETED_FILTER_ENABLED,
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
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-toolbar-selected-conversations {
    display: flex;
    flex-direction: row;

    .mark-as-unflagged-btn {
        .bm-captioned-icon-button,
        .bm-icon-button {
            .bm-icon {
                color: $warning-fg;
            }
        }
    }
}
</style>
