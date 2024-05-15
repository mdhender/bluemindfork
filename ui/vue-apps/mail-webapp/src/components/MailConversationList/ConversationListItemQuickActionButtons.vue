<template>
    <bm-toolbar v-if="isWritable" class="conversation-list-item-quick-action-buttons">
        <template v-if="isDeleted">
            <bm-toolbar-icon-button
                :aria-label="unexpungeAriaText(1, subject)"
                :title="unexpungeAriaText(1, subject)"
                variant="compact"
                icon="clock-rewind"
                @click.prevent.stop="unexpunge([conversation])"
            />
        </template>
        <template v-else>
            <bm-toolbar-icon-button
                :aria-label="removeAriaText(1, subject)"
                :title="removeAriaText(1, subject)"
                variant="compact"
                icon="trash"
                @click.shift.exact.prevent.stop="REMOVE_CONVERSATIONS([conversation])"
                @click.exact.prevent.stop="MOVE_CONVERSATIONS_TO_TRASH([conversation])"
            />
            <bm-toolbar-icon-button
                v-if="isTemplate"
                :aria-label="$tc('mail.actions.edit_from_template.aria')"
                :title="$tc('mail.actions.edit_from_template.aria')"
                variant="compact"
                icon="mail-plus"
                @click.prevent.stop="editFromTemplate(conversation)"
            />
            <bm-toolbar-icon-button
                v-if="showMarkAsReadInMain(isTemplate)"
                :aria-label="markAsReadAriaText(1, subject)"
                :title="markAsReadAriaText(1, subject)"
                variant="compact"
                icon="read"
                @click.prevent.stop="markAsRead(conversation)"
            />
            <bm-toolbar-icon-button
                v-if="showMarkAsUnreadInMain(isTemplate)"
                :aria-label="markAsUnreadAriaText(1, subject)"
                :title="markAsUnreadAriaText(1, subject)"
                variant="compact"
                icon="mail-dot"
                @click.prevent.stop="markAsUnread(conversation)"
            />
            <bm-toolbar-icon-button
                v-if="showMarkAsFlaggedInMain"
                :aria-label="markAsFlaggedAriaText(1, subject)"
                :title="markAsFlaggedAriaText(1, subject)"
                variant="compact"
                icon="flag"
                @click.prevent.stop="markAsFlagged(conversation)"
            />
            <bm-toolbar-icon-button
                v-if="showMarkAsUnflaggedInMain"
                :aria-label="markAsUnflaggedAriaText(1, subject)"
                :title="markAsUnflaggedAriaText(1, subject)"
                variant="compact"
                icon="flag-fill"
                class="text-warning"
                @click.prevent.stop="markAsUnflagged(conversation)"
            />
        </template>
    </bm-toolbar>
</template>

<script>
import { BmToolbar, BmToolbarIconButton } from "@bluemind/ui-components";
import { mapState, mapGetters, mapActions } from "vuex";
import { messageUtils } from "@bluemind/mail";
import { Flag } from "@bluemind/email";
import { ActionTextMixin, FlagMixin, RemoveMixin, MailRoutesMixin } from "~/mixins";
import { MY_DRAFTS, MY_TEMPLATES } from "~/getters";

import MessagePathParam from "~/router/MessagePathParam";

const { MessageCreationModes } = messageUtils;

export default {
    name: "ConversationListItemQuickActionButtons",
    components: {
        BmToolbar,
        BmToolbarIconButton
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
        isWritable() {
            return this.folder.writable;
        },
        isDeleted() {
            return this.conversation.flags.includes(Flag.DELETED);
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
