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
            <bm-dropdown-item
                v-if="!message.flags.includes(Flag.SEEN)"
                @click.prevent.stop="MARK_MESSAGE_AS_READ(message)"
            >
                {{ $tc("mail.actions.mark_as_read", 1) }}
            </bm-dropdown-item>
            <bm-dropdown-item v-else @click.prevent.stop="MARK_MESSAGE_AS_UNREAD(message)">
                {{ $tc("mail.actions.mark_as_unread", 1) }}
            </bm-dropdown-item>
            <bm-dropdown-item
                v-if="!message.flags.includes(Flag.FLAGGED)"
                @click.prevent.stop="MARK_MESSAGE_AS_FLAGGED(message)"
            >
                {{ $t("mail.actions.mark_flagged") }}
            </bm-dropdown-item>
            <bm-dropdown-item v-else @click.prevent.stop="MARK_MESSAGE_AS_UNFLAGGED(message)">
                {{ $t("mail.actions.mark_unflagged") }}
            </bm-dropdown-item>
            <bm-dropdown-item @click.prevent.stop="move(message)">
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
            <bm-dropdown-item icon="printer" @click="printMessage(message)">
                {{ $t("common.print") }}
            </bm-dropdown-item>
            <bm-dropdown-item @click.prevent.stop.exact="REMOVE_MESSAGES(conversation, message)">
                {{ $t("mail.actions.purge") }}
            </bm-dropdown-item>
        </bm-dropdown>

        <bm-modal
            :ref="'move-modal-' + message.key"
            centered
            :title="$t('mail.toolbar.move.tooltip')"
            auto-focus-button="ok"
            :scrollable="false"
            @ok="moveOk"
            @cancel="moveCancel"
            @hide="moveCancel"
        >
            <template #default>
                <div class="d-flex">
                    <div class="flex-columns mr-4">
                        <bm-icon icon="folder" size="5x" class="mr-2 text-secondary" />
                    </div>
                    <div class="flex-columns modal-form-autocomplete">
                        <p>{{ $t("mail.actions.move.modal.combo.label") }}</p>
                        <bm-form-autocomplete-input
                            v-slot="{ item }"
                            v-model.trim="pattern"
                            variant="outline-secondary"
                            :items="itemsOrDefaults()"
                            icon="search"
                            actionable-icon
                            :max-results="maxFolders"
                            @selected="folderSelection"
                            @input="onInputUpdate"
                        >
                            <div class="d-flex align-items-center">
                                <span class="flex-fill"> {{ translatePath(item.path) }}</span>
                                <mail-mailbox-icon no-text :mailbox="mailboxes[item.mailboxRef.key]" />
                            </div>
                        </bm-form-autocomplete-input>
                    </div>
                </div>
            </template>

            <template #modal-footer="{ ok, cancel }">
                <bm-button type="submit" variant="primary" :disabled="!folderSelected" @click.prevent="ok()">
                    {{ $t("mail.actions.move") }}
                </bm-button>
                <bm-button variant="outline-secondary" class="ml-2" @click.prevent="cancel()">
                    {{ $t("common.cancel") }}
                </bm-button>
            </template>
        </bm-modal>
    </div>
</template>

<script>
import { mapActions, mapGetters, mapState } from "vuex";
import { Flag } from "@bluemind/email";
import { BmButton, BmDropdown, BmDropdownItem, BmFormAutocompleteInput, BmIcon, BmModal } from "@bluemind/styleguide";
import { RemoveMixin, MoveMixin, FilterFolderMixin, PrintMixin } from "~/mixins";
import { translatePath } from "~/model/folder";
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
import MailMailboxIcon from "../MailMailboxIcon.vue";
import Vue from "vue";
Vue.component("BmButton", BmButton);

export default {
    name: "MailViewerToolbarOtherActions",
    components: {
        BmButton,
        BmDropdown,
        BmDropdownItem,
        BmFormAutocompleteInput,
        BmIcon,
        BmModal,
        MailMailboxIcon
    },
    mixins: [RemoveMixin, MoveMixin, FilterFolderMixin, PrintMixin],
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
        return {
            folderSelected: null,
            Flag
        };
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS }),
        ...mapState("mail", ["mailboxes"])
    },
    methods: {
        ...mapActions("mail", {
            MARK_MESSAGE_AS_FLAGGED,
            MARK_MESSAGE_AS_READ,
            MARK_MESSAGE_AS_UNFLAGGED,
            MARK_MESSAGE_AS_UNREAD
        }),
        folderSelection(folder) {
            this.folderSelected = folder;
            this.pattern = translatePath(folder.path);
        },
        itemsOrDefaults() {
            return this.folderSelected ? [] : this.matchingFolders;
        },
        onInputUpdate() {
            this.folderSelected = null;
        },
        move() {
            this.$refs[`move-modal-${this.message.key}`].show();
        },
        moveOk() {
            this.MOVE_CONVERSATION_MESSAGE({
                conversation: this.conversation,
                message: this.message,
                folder: this.folderSelected
            });
        },
        moveCancel() {
            this.pattern = "";
            this.folderSelected = null;
        },
        translatePath(path) {
            return translatePath(path);
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
@import "~@bluemind/styleguide/css/_zIndex.scss";

.other-viewer-actions .dropdown-menu {
    border: none !important;
    margin-top: $sp-1 !important;
    padding: 0 !important;
}

.modal-body {
    overflow: visible;
    padding-bottom: 1rem;
    .flex-columns.modal-form-autocomplete {
        width: 100%;
    }
}
</style>
