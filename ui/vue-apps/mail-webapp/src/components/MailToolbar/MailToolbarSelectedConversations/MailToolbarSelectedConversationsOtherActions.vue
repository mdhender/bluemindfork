<template>
    <bm-dropdown
        :no-caret="true"
        variant="inline-on-fill-primary"
        :aria-label="$tc('mail.toolbar.more.aria')"
        :title="$tc('mail.toolbar.more.aria')"
        toggle-class="btn-lg-simple-neutral"
        class="mail-toolbar-consult-message-other-actions h-100"
        right
    >
        <template slot="button-content">
            <bm-icon icon="3dots" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.toolbar.more") }}</span>
        </template>
        <bm-dropdown-item v-if="!isTemplate && isSingleMessage" icon="pencil" @click="editAsNew()">
            {{ $t("mail.actions.edit_as_new") }}
        </bm-dropdown-item>
        <bm-dropdown-item v-if="isTemplate" icon="plus-document" @click="modifyTemplate()">
            {{ $t("mail.actions.modify_template") }}
        </bm-dropdown-item>
        <bm-dropdown-item
            v-if="isTemplate && showMarkAsRead"
            icon="read"
            :title="markAsReadAriaText()"
            @click="markAsRead()"
        >
            {{ markAsReadText }}
        </bm-dropdown-item>
        <bm-dropdown-item v-else-if="isTemplate" icon="unread" :title="markAsUnreadAriaText()" @click="markAsUnread()">
            {{ markAsUnreadText }}
        </bm-dropdown-item>
        <bm-dropdown-item
            icon="printer"
            :title="$t('mail.actions.print.title', { subject })"
            :disabled="selectionLength > 1"
            @click="printContent()"
        >
            {{ $t("common.print") }}
        </bm-dropdown-item>
        <bm-dropdown-item :shortcut="$t('mail.shortcuts.purge')" :title="removeAriaText()" @click="remove()">
            {{ $t("mail.actions.purge") }}
        </bm-dropdown-item>
    </bm-dropdown>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";
import { BmDropdown, BmDropdownItem, BmIcon } from "@bluemind/styleguide";
import { message } from "@bluemind/mail";
import { ActionTextMixin, RemoveMixin, SelectionMixin, FlagMixin, PrintMixin, MailRoutesMixin } from "~/mixins";
import { CURRENT_CONVERSATION_METADATA, MY_DRAFTS, MY_TEMPLATES } from "~/getters";
import { SET_MESSAGE_COMPOSING } from "~/mutations";
import MessagePathParam from "~/router/MessagePathParam";
import MailMessagePrint from "~/components/MailViewer/MailMessagePrint";

const { MessageCreationModes } = message;

export default {
    name: "MailToolbarSelectedConversationsOtherActions",
    // eslint-disable-next-line vue/no-unused-components
    components: { BmDropdown, BmDropdownItem, BmIcon, MailMessagePrint },
    mixins: [ActionTextMixin, RemoveMixin, FlagMixin, PrintMixin, SelectionMixin, MailRoutesMixin],
    computed: {
        ...mapGetters("mail", { CURRENT_CONVERSATION_METADATA, MY_DRAFTS, MY_TEMPLATES }),
        ...mapState("mail", { messages: state => state.conversations.messages }),
        isTemplate() {
            if (this.selectionLength === 1) {
                return this.CURRENT_CONVERSATION_METADATA.folderRef.key === this.MY_TEMPLATES.key;
            }
            return false;
        },
        isSingleMessage() {
            if (this.selectionLength === 1) {
                return this.CURRENT_CONVERSATION_METADATA.size === 1;
            }
            return false;
        },
        subject() {
            if (this.selectionLength === 1) {
                return this.CURRENT_CONVERSATION_METADATA.subject;
            }
            return "";
        }
    },
    methods: {
        ...mapMutations("mail", { SET_MESSAGE_COMPOSING }),
        printContent() {
            const index = this.CURRENT_CONVERSATION_METADATA.messages.length - 1;
            const message = this.messages[this.CURRENT_CONVERSATION_METADATA.messages[index]];
            this.print(this.$createElement("mail-message-print", { props: { message } }));
        },
        editAsNew() {
            const template = this.messages[this.CURRENT_CONVERSATION_METADATA.messages[0]];
            this.$router.navigate({
                name: "mail:message",
                params: { messagepath: this.draftPath(this.MY_DRAFTS) },
                query: { action: MessageCreationModes.EDIT_AS_NEW, message: MessagePathParam.build("", template) }
            });
        },
        modifyTemplate() {
            const messageKey = this.selected[0].messages[0];
            this.SET_MESSAGE_COMPOSING({ messageKey, composing: true });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-toolbar-consult-message-other-actions .dropdown-menu {
    border: none !important;
    margin-top: $sp-1 !important;
    padding: 0 !important;
}
</style>
