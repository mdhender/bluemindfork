<template>
    <div>
        <bm-dropdown
            :no-caret="true"
            variant="simple-primary"
            :aria-label="$tc('mail.toolbar.more.aria')"
            :title="$tc('mail.toolbar.more.aria')"
            class="other-viewer-actions"
        >
            <template slot="button-content">
                <bm-icon icon="3dots" size="2x" />
                <span class="d-lg-none">{{ $t("mail.toolbar.more") }}</span>
            </template>
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
                @click.exact="MOVE_MESSAGES_TO_TRASH(conversation, message)"
                @click.shift.exact="REMOVE_MESSAGES(conversation, message)"
            >
                {{ $t("mail.actions.remove") }}
            </bm-dropdown-item>
            <bm-dropdown-item icon="pencil" @click="editAsNew()">
                {{ $t("mail.actions.edit_as_new") }}
            </bm-dropdown-item>
            <bm-dropdown-item icon="printer" @click.stop="printContent()">
                {{ $t("common.print") }}
            </bm-dropdown-item>
            <bm-dropdown-item @click.stop.exact="REMOVE_MESSAGES(conversation, message)">
                {{ $t("mail.actions.purge") }}
            </bm-dropdown-item>
        </bm-dropdown>
        <choose-folder-modal
            :ref="'move-modal-' + message.key"
            :title="$t('mail.toolbar.move.tooltip')"
            :ok-title="$t('mail.actions.move')"
            :cancel-title="$t('common.cancel')"
            :excluded-folders="[message.folderRef.key]"
            @ok="moveOk($event)"
        />
    </div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import { Flag } from "@bluemind/email";
import { BmButton, BmDropdown, BmDropdownItem, BmIcon } from "@bluemind/styleguide";
import { RemoveMixin, MoveMixin, PrintMixin } from "~/mixins";
import {
    MARK_MESSAGE_AS_FLAGGED,
    MARK_MESSAGE_AS_READ,
    MARK_MESSAGE_AS_UNFLAGGED,
    MARK_MESSAGE_AS_UNREAD
} from "~/actions";
import { MY_DRAFTS } from "~/getters";
import { MessageCreationModes } from "~/model/message";
import { draftPath } from "~/model/draft";
import MessagePathParam from "~/router/MessagePathParam";
import ChooseFolderModal from "../ChooseFolderModal";
import MailMessagePrint from "./MailMessagePrint.vue";

export default {
    name: "MailViewerToolbarOtherActions",
    components: { BmDropdown, BmDropdownItem, BmIcon, ChooseFolderModal },
    mixins: [RemoveMixin, MoveMixin, PrintMixin],
        // eslint-disable-next-line vue/no-unused-components
        MailMessagePrint
    props: {
        message: {
            type: Object,
            required: true
        },
        conversation: {
            type: Object,
            required: true
        }
    },
    data() {
        return { Flag };
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS })
    },
    methods: {
        ...mapActions("mail", {
            MARK_MESSAGE_AS_FLAGGED,
            MARK_MESSAGE_AS_READ,
            MARK_MESSAGE_AS_UNFLAGGED,
            MARK_MESSAGE_AS_UNREAD
        }),
        printContent() {
            this.print(this.$createElement("mail-message-print", { props: { message: this.message } }));
        },
        move() {
            this.$refs[`move-modal-${this.message.key}`].show();
        },
        moveOk(selectedFolder) {
            this.MOVE_CONVERSATION_MESSAGE({
                conversation: this.conversation,
                message: this.message,
                folder: selectedFolder
            });
        },
        editAsNew() {
            this.$router.navigate({
                name: "mail:message",
                params: { messagepath: draftPath(this.MY_DRAFTS) },
                query: { action: MessageCreationModes.EDIT_AS_NEW, message: MessagePathParam.build("", this.message) }
            });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.other-viewer-actions .dropdown-menu {
    border: none !important;
    margin-top: $sp-1 !important;
    padding: 0 !important;
}
</style>
