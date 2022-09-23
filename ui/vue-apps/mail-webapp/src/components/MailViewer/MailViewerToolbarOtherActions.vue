<template>
    <div>
        <bm-icon-dropdown
            no-caret
            boundary="viewport"
            icon="3dots"
            variant="regular-accent"
            :size="size"
            :label="$t('mail.toolbar.more')"
            :aria-label="$tc('mail.toolbar.more.aria')"
            :title="$tc('mail.toolbar.more.aria')"
            class="other-viewer-actions"
        >
            <bm-dropdown-item v-if="!message.flags.includes(Flag.SEEN)" @click.stop="MARK_MESSAGE_AS_READ(message)">
                {{ $tc("mail.actions.mark_as_read", 1) }}
            </bm-dropdown-item>
            <bm-dropdown-item v-else @click.stop="MARK_MESSAGE_AS_UNREAD(message)">
                {{ $tc("mail.actions.mark_as_unread", 1) }}
            </bm-dropdown-item>
            <bm-dropdown-item
                v-if="!message.flags.includes(Flag.FLAGGED)"
                @click.stop="MARK_MESSAGE_AS_FLAGGED(message)"
            >
                {{ $t("mail.actions.mark_flagged") }}
            </bm-dropdown-item>
            <bm-dropdown-item v-else @click.stop="MARK_MESSAGE_AS_UNFLAGGED(message)">
                {{ $t("mail.actions.mark_unflagged") }}
            </bm-dropdown-item>
            <bm-dropdown-item @click.stop="move(message)">
                {{ $t("mail.actions.move") }}
            </bm-dropdown-item>
            <bm-dropdown-item
                @click.exact.stop="MOVE_MESSAGES_TO_TRASH(message, conversation)"
                @click.shift.exact.stop="REMOVE_MESSAGES(message, conversation)"
            >
                {{ $t("mail.actions.remove") }}
            </bm-dropdown-item>
            <bm-dropdown-item @click.stop.exact="REMOVE_MESSAGES(message, conversation)">
                {{ $t("mail.actions.purge") }}
            </bm-dropdown-item>
            <mail-open-in-popup-with-shift v-if="isTemplate" v-slot="action" :href="modifyTemplateRoute">
                <bm-dropdown-item
                    :icon="action.icon('plus-document')"
                    :title="action.label($t('mail.actions.modify_template'))"
                    @click="action.execute(modifyTemplate)"
                >
                    {{ $t("mail.actions.modify_template") }}
                </bm-dropdown-item>
            </mail-open-in-popup-with-shift>
            <mail-open-in-popup-with-shift v-else v-slot="action" :href="editAsNew">
                <bm-dropdown-item
                    :icon="action.icon('pencil')"
                    @click.stop="action.execute(() => $router.push(editAsNew))"
                >
                    {{ $t("mail.actions.edit_as_new") }}
                </bm-dropdown-item>
            </mail-open-in-popup-with-shift>

            <bm-dropdown-item icon="printer" @click.stop="printContent()">
                {{ $t("common.print") }}
            </bm-dropdown-item>
            <mail-open-in-popup
                v-slot="action"
                :href="$router.relative({ name: 'mail:popup:message', params: { messagepath } })"
            >
                <bm-dropdown-item :icon="action.icon" @click.stop="action.execute()">
                    {{ action.label }}
                </bm-dropdown-item>
            </mail-open-in-popup>
            <bm-dropdown-item icon="code" @click.stop="showSource(message)">
                {{ $t("mail.actions.show_source") }}
            </bm-dropdown-item>
            <bm-dropdown-item icon="download" @click.stop="downloadEml(message)">
                {{ $t("mail.actions.download_eml") }}
            </bm-dropdown-item>
        </bm-icon-dropdown>
        <choose-folder-modal
            ref="move-modal"
            :cancel-title="$t('common.cancel')"
            :ok-title="$t('mail.actions.move')"
            :title="$t('mail.toolbar.move.tooltip')"
            :is-excluded="isFolderExcluded"
            :default-folders="[MY_TRASH, MY_INBOX]"
            :mailboxes="MAILBOXES"
            @ok="moveOk"
        />
    </div>
</template>

<script>
import { mapActions, mapGetters, mapMutations } from "vuex";
import { Flag } from "@bluemind/email";
import { BmIconDropdown, BmDropdownItem } from "@bluemind/styleguide";
import { messageUtils, folderUtils } from "@bluemind/mail";
import { EmlMixin, RemoveMixin, MoveMixin, PrintMixin, MailRoutesMixin } from "~/mixins";
import {
    MARK_MESSAGE_AS_FLAGGED,
    MARK_MESSAGE_AS_READ,
    MARK_MESSAGE_AS_UNFLAGGED,
    MARK_MESSAGE_AS_UNREAD
} from "~/actions";
import { MAILBOXES, MY_DRAFTS, MY_TRASH, MY_INBOX, MY_TEMPLATES } from "~/getters";
import { SET_MESSAGE_COMPOSING } from "~/mutations";
import MessagePathParam from "~/router/MessagePathParam";
import ChooseFolderModal from "../ChooseFolderModal";
import MailMessagePrint from "./MailMessagePrint";
import MailOpenInPopup from "../MailOpenInPopup";
import MailOpenInPopupWithShift from "../MailOpenInPopupWithShift";

const { MessageCreationModes } = messageUtils;
const { isRoot, getInvalidCharacter } = folderUtils;

export default {
    name: "MailViewerToolbarOtherActions",
    components: {
        BmIconDropdown,
        BmDropdownItem,
        ChooseFolderModal,
        MailOpenInPopup,
        MailOpenInPopupWithShift,
        // eslint-disable-next-line vue/no-unused-components
        MailMessagePrint
    },
    mixins: [EmlMixin, RemoveMixin, MoveMixin, PrintMixin, MailRoutesMixin],
    props: {
        message: {
            type: Object,
            required: true
        },
        conversation: {
            type: Object,
            default: undefined
        },
        size: {
            type: String,
            required: true
        }
    },
    data() {
        return { Flag };
    },
    computed: {
        ...mapGetters("mail", { MAILBOXES, MY_DRAFTS, MY_TEMPLATES, MY_TRASH, MY_INBOX }),
        messagepath() {
            return MessagePathParam.build("", this.message);
        },
        editAsNew() {
            return this.$router.relative({
                name: "mail:message",
                params: { messagepath: this.draftPath(this.MY_DRAFTS) },
                query: { action: MessageCreationModes.EDIT_AS_NEW, message: this.messagepath }
            });
        },
        modifyTemplateRoute() {
            return this.$router.relative({
                name: "mail:message",
                params: { messagepath: this.messagepath },
                query: { action: MessageCreationModes.EDIT }
            });
        },
        isTemplate() {
            return this.message.folderRef.key === this.MY_TEMPLATES.key;
        }
    },
    methods: {
        ...mapActions("mail", {
            MARK_MESSAGE_AS_FLAGGED,
            MARK_MESSAGE_AS_READ,
            MARK_MESSAGE_AS_UNFLAGGED,
            MARK_MESSAGE_AS_UNREAD
        }),
        ...mapMutations("mail", { SET_MESSAGE_COMPOSING }),
        printContent() {
            this.print(this.$createElement("mail-message-print", { props: { message: this.message } }));
        },
        move() {
            this.$refs["move-modal"].show();
        },
        moveOk(selectedFolder) {
            this.MOVE_CONVERSATION_MESSAGE({
                conversation: this.conversation,
                message: this.message,
                folder: selectedFolder
            });
        },
        isFolderExcluded(folder) {
            if (folder) {
                if (folder.key === this.message.folderRef.key) {
                    return this.$t("mail.actions.move_message.excluded_folder.same");
                }
                if (isRoot(folder)) {
                    return this.$t("mail.actions.move_message.excluded_folder.root");
                }
                const invalidCharacter = getInvalidCharacter(folder.path);
                if (invalidCharacter) {
                    return this.$t("common.invalid.character", {
                        character: invalidCharacter
                    });
                }
            }
            return false;
        },
        modifyTemplate() {
            this.SET_MESSAGE_COMPOSING({ messageKey: this.message.key, composing: true });
        }
    }
};
</script>
