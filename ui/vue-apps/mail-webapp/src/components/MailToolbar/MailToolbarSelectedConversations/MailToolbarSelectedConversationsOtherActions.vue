<template>
    <div
        :aria-label="$tc('mail.toolbar.more.aria')"
        :title="$tc('mail.toolbar.more.aria')"
        toggle-class="btn-lg-simple-neutral"
        class="mail-toolbar-consult-message-other-actions"
        icon="3dots-horizontal"
        :label="$tc('mail.toolbar.more')"
        no-caret
        right
        :compact="compact"
    >
        <mail-open-in-popup-with-shift v-if="!isTemplate && isSingleMessage" v-slot="action" :href="editAsNew">
            <bm-dropdown-item :icon="action.icon('pencil')" @click="action.execute(() => $router.push(editAsNew))">
                {{ $t("mail.actions.edit_as_new") }}
            </bm-dropdown-item>
        </mail-open-in-popup-with-shift>
        <mail-open-in-popup-with-shift v-if="isTemplate" v-slot="action" :href="modifyTemplateRoute">
            <bm-dropdown-item
                :icon="action.icon('document-plus')"
                :title="action.label($t('mail.actions.modify_template'))"
                @click="action.execute(modifyTemplate)"
            >
                {{ $t("mail.actions.modify_template") }}
            </bm-dropdown-item>
        </mail-open-in-popup-with-shift>
        <bm-dropdown-item
            v-if="showMarkAsReadInOthers(isTemplate)"
            :title="markAsReadAriaText()"
            icon="mail-open"
            @click="markAsRead()"
        >
            {{ markAsReadText }}
        </bm-dropdown-item>
        <bm-dropdown-item
            v-if="showMarkAsUnreadInOthers(isTemplate)"
            :title="markAsUnreadAriaText()"
            icon="mail-dot"
            @click="markAsUnread()"
        >
            {{ markAsUnreadText }}
        </bm-dropdown-item>
        <bm-dropdown-item
            v-if="showMarkAsFlaggedInOthers"
            :title="markAsFlaggedAriaText()"
            icon="flag-fill"
            class="mark-as-flagged-item"
            @click="markAsFlagged()"
        >
            {{ markAsFlaggedText }}
        </bm-dropdown-item>
        <bm-dropdown-item
            v-if="showMarkAsUnflaggedInOthers"
            :title="markAsUnflaggedAriaText()"
            icon="flag"
            @click="markAsUnflagged()"
        >
            {{ markAsUnflaggedText }}
        </bm-dropdown-item>
        <bm-dropdown-item
            icon="printer"
            :title="$t('mail.actions.print.title', { subject })"
            :disabled="!isSingleMessage"
            @click="printContent()"
        >
            {{ $t("common.print") }}
        </bm-dropdown-item>
        <bm-dropdown-item :shortcut="$t('mail.shortcuts.purge')" :title="removeAriaText()" @click="remove()">
            {{ $t("mail.actions.purge") }}
        </bm-dropdown-item>
        <template v-if="selectionLength === 1">
            <bm-dropdown-item icon="code" @click.stop="showSource(lastMessage)">
                {{ $t("mail.actions.show_source") }}
            </bm-dropdown-item>
            <bm-dropdown-item icon="box-arrow-down" @click.stop="downloadEml(lastMessage)">
                {{ $t("mail.actions.download_eml") }}
            </bm-dropdown-item>
            <bm-dropdown-item
                icon="mail-paperclip"
                @click.stop="forwardEml(CURRENT_CONVERSATION_METADATA, lastMessage)"
            >
                {{ $t("mail.actions.forward_eml") }}
            </bm-dropdown-item>
        </template>
    </div>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";
import { BmDropdownItem, BmIcon } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import {
    ActionTextMixin,
    EmlMixin,
    FlagMixin,
    MailRoutesMixin,
    PrintMixin,
    RemoveMixin,
    ReplyAndForwardRoutesMixin,
    SelectionMixin
} from "~/mixins";
import { CURRENT_CONVERSATION_METADATA, MY_DRAFTS, MY_TEMPLATES } from "~/getters";
import { SET_MESSAGE_COMPOSING } from "~/mutations";
import MessagePathParam from "~/router/MessagePathParam";
import MailToolbarResponsiveDropdown from "~/components/MailToolbar/MailToolbarResponsiveDropdown";
import MailMessagePrint from "~/components/MailViewer/MailMessagePrint";
import MailOpenInPopupWithShift from "~/components/MailOpenInPopupWithShift";

const { MessageCreationModes } = messageUtils;

export default {
    name: "MailToolbarSelectedConversationsOtherActions",
    // eslint-disable-next-line vue/no-unused-components
    components: { BmDropdownItem, BmIcon, MailToolbarResponsiveDropdown, MailMessagePrint, MailOpenInPopupWithShift },
    mixins: [
        ActionTextMixin,
        EmlMixin,
        FlagMixin,
        MailRoutesMixin,
        PrintMixin,
        RemoveMixin,
        ReplyAndForwardRoutesMixin,
        SelectionMixin
    ],
    props: {
        compact: {
            type: Boolean,
            default: false
        }
    },
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
                return this.CURRENT_CONVERSATION_METADATA.messages.length === 1;
            }
            return false;
        },
        editAsNew() {
            if (this.isSingleMessage) {
                const template = this.messages[this.CURRENT_CONVERSATION_METADATA.messages[0]];
                return this.$router.relative({
                    name: "mail:message",
                    params: { messagepath: this.draftPath(this.MY_DRAFTS) },
                    query: { action: MessageCreationModes.EDIT_AS_NEW, message: MessagePathParam.build("", template) }
                });
            }
            return {};
        },
        subject() {
            if (this.selectionLength === 1) {
                return this.CURRENT_CONVERSATION_METADATA.subject;
            }
            return "";
        },
        lastMessage() {
            if (this.selectionLength === 1) {
                const messageKeys = this.CURRENT_CONVERSATION_METADATA.messages;
                return this.messages[messageKeys[messageKeys.length - 1]];
            }
            return null;
        },
        modifyTemplateRoute() {
            if (this.isTemplate) {
                const message = this.messages[this.CURRENT_CONVERSATION_METADATA.messages[0]];
                return this.$router.relative({
                    name: "mail:message",
                    params: { messagepath: MessagePathParam.build("", message) },
                    query: { action: MessageCreationModes.EDIT }
                });
            }
            return {};
        }
    },
    methods: {
        ...mapMutations("mail", { SET_MESSAGE_COMPOSING }),
        printContent() {
            const index = this.CURRENT_CONVERSATION_METADATA.messages.length - 1;
            const message = this.messages[this.CURRENT_CONVERSATION_METADATA.messages[index]];
            this.print(this.$createElement("mail-message-print", { props: { message } }));
        },
        modifyTemplate() {
            const messageKey = this.selected[0].messages[0];
            this.SET_MESSAGE_COMPOSING({ messageKey, composing: true });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-toolbar-consult-message-other-actions {
    .mark-as-flagged-item {
        .bm-icon {
            color: $warning-fg;
        }
    }
}
</style>
