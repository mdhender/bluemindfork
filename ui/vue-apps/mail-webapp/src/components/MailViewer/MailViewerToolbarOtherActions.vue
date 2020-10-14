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
            <bm-dropdown-item v-if="!message.flags.includes(Flag.SEEN)" @click="MARK_MESSAGE_AS_READ(message)">
                {{ $tc("mail.actions.mark_as_read", 1) }}
            </bm-dropdown-item>
            <bm-dropdown-item v-else @click="MARK_MESSAGE_AS_UNREAD(message)">
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
            <bm-dropdown-item @click="move(message)">
                {{ $t("mail.actions.move") }}
            </bm-dropdown-item>
            <bm-dropdown-item @click.exact.prevent.stop="MOVE_MESSAGES_TO_TRASH(conversation, message)">
                {{ $t("mail.actions.remove") }}
            </bm-dropdown-item>
        </bm-dropdown>

        <bm-modal
            :id="'move-modal-' + message.key"
            centered
            :title="$t('mail.toolbar.move.tooltip')"
            auto-focus-button="ok"
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
                        <p>Dossier de destination</p>
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
                            <div>
                                {{ translatePath(item.path) }}
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
import { mapActions } from "vuex";
import { Flag } from "@bluemind/email";
import { BmButton, BmDropdown, BmDropdownItem, BmFormAutocompleteInput, BmIcon, BmModal } from "@bluemind/styleguide";
import { RemoveMixin, MoveMixin, FilterFolderMixin } from "~/mixins";
import { translatePath } from "~/model/folder";
import {
    MARK_MESSAGE_AS_FLAGGED,
    MARK_MESSAGE_AS_READ,
    MARK_MESSAGE_AS_UNFLAGGED,
    MARK_MESSAGE_AS_UNREAD
} from "~/actions";

export default {
    name: "MailViewerToolbarOtherActions",
    components: {
        BmButton,
        BmDropdown,
        BmDropdownItem,
        BmFormAutocompleteInput,
        BmIcon,
        BmModal
    },
    mixins: [RemoveMixin, MoveMixin, FilterFolderMixin],
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
            this.$bvModal.show(`move-modal-${this.message.key}`);
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
