<template>
    <mail-toolbar-responsive-dropdown
        ref="move-dropdown"
        no-caret
        class="mail-toolbar-selected-conversations-move-action"
        icon="folder"
        :label="moveText"
        :title="moveAriaText()"
        :aria-label="moveAriaText()"
        @shown="openMoveAutocomplete"
        @hide="resetPattern"
    >
        <global-events @keydown.tab.capture="forceCloseMoveAutocomplete" />
        <bm-dropdown-autocomplete
            ref="moveAutocomplete"
            v-slot="{ item }"
            v-model.trim="pattern"
            :items="pattern ? matchingFolders(isExcluded) : [MY_TRASH, MY_INBOX]"
            icon="search"
            :max-results="maxFolders"
            @keydown.esc.native="resetPattern"
        >
            <bm-dropdown-item-button
                class="text-nowrap text-truncate w-100"
                :title="$tc('mail.actions.move.item', 1, { path: item.path })"
                @click="moveToFolder(item)"
            >
                <template #icon>
                    <mail-folder-icon no-text :mailbox="mailboxes[item.mailboxRef.key]" :folder="item" />
                </template>
                <div class="d-flex align-items-center">
                    <span class="flex-fill text-truncate"> {{ translatePath(item.path) }}</span>
                    <mail-mailbox-icon :mailbox="mailboxes[item.mailboxRef.key]" />
                </div>
            </bm-dropdown-item-button>
        </bm-dropdown-autocomplete>
        <bm-notice
            v-if="invalidCharacter"
            class="position-absolute w-100"
            :text="$t('common.invalid.character', { character: invalidCharacter })"
        />
        <bm-dropdown-divider />
        <bm-dropdown-form
            v-if="pattern === ''"
            class="new-folder position-relative"
            :aria-label="$t('mail.actions.create.folder')"
            :title="$t('mail.actions.create.folder')"
            @focus="$refs['mail-folder-input'].focus()"
        >
            <div class="d-flex align-items-center">
                <mail-folder-input
                    ref="mail-folder-input"
                    class="pl-2 pr-1 flex-fill"
                    :submit-on-focusout="false"
                    :mailboxes="[MY_MAILBOX]"
                    @submit="newFolderName => moveToFolder({ name: newFolderName, path: newFolderName })"
                    @keydown.left.native.stop
                    @keydown.right.native.stop
                    @keydown.esc.native.stop
                />
                <mail-mailbox-icon :mailbox="MY_MAILBOX" />
            </div>
        </bm-dropdown-form>
        <bm-dropdown-item-button
            v-else-if="newFolderValidity === true"
            :aria-label="$tc('mail.actions.move.item', 1, { path: pattern })"
            :title="$tc('mail.actions.move.item', 1, { path: pattern })"
            icon="plus"
            @click="moveToFolder({ name: pattern, path: pattern })"
        >
            <div class="d-flex align-items-center">
                <span class="flex-fill"> {{ $t("mail.folder.new.from_pattern", [pattern]) }}</span>
                <mail-mailbox-icon :mailbox="MY_MAILBOX" />
            </div>
        </bm-dropdown-item-button>
    </mail-toolbar-responsive-dropdown>
</template>

<script>
import {
    BmDropdownAutocomplete,
    BmDropdownDivider,
    BmDropdownForm,
    BmDropdownItemButton,
    BmNotice
} from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import { folderUtils } from "@bluemind/mail";
import MailToolbarResponsiveDropdown from "../MailToolbarResponsiveDropdown";
import MailFolderIcon from "../../MailFolderIcon";
import MailMailboxIcon from "../../MailMailboxIcon";
import MailFolderInput from "../../MailFolderInput";
import { MY_MAILBOX, FOLDERS_BY_PATH, MY_TRASH, MY_INBOX } from "~/getters";
import { ActionTextMixin, FilterFolderMixin, MoveMixin, SelectionMixin } from "~/mixins";

const { getInvalidCharacter, isNameValid, translatePath } = folderUtils;
const LOOP_PERF_LIMIT = 100;

export default {
    name: "MailToolbarSelectedConversationsMoveAction",
    components: {
        BmDropdownAutocomplete,
        BmDropdownDivider,
        BmDropdownForm,
        BmDropdownItemButton,
        BmNotice,
        GlobalEvents,
        MailToolbarResponsiveDropdown,
        MailFolderIcon,
        MailFolderInput,
        MailMailboxIcon
    },
    mixins: [ActionTextMixin, FilterFolderMixin, MoveMixin, SelectionMixin],
    computed: {
        ...mapState("mail", ["folders", "mailboxes"]),
        ...mapGetters("mail", { MY_MAILBOX, FOLDERS_BY_PATH, MY_TRASH, MY_INBOX }),
        newFolderValidity() {
            if (this.pattern) {
                const pattern = this.pattern.replace(/\/+/, "/").replace(/^\/?(.*)\/?$/g, "$1");
                return pattern && isNameValid(pattern, pattern, p => this.FOLDERS_BY_PATH(p)[0]);
            }
            return null;
        },
        invalidCharacter() {
            return getInvalidCharacter(this.pattern);
        }
    },
    methods: {
        moveToFolder(folder) {
            this.MOVE_CONVERSATIONS({ conversations: this.selected, folder });
            this.$refs["move-dropdown"].hide(true);
        },
        openMoveAutocomplete() {
            this.$nextTick(() => this.$refs["moveAutocomplete"].focus());
        },
        forceCloseMoveAutocomplete() {
            this.$refs["move-dropdown"].hide(true);
            this.pattern = "";
        },
        resetPattern() {
            this.pattern = "";
        },
        translatePath,
        isExcluded(folder) {
            return folder && this.excludedFolderKeys().includes(folder.key);
        },
        /**
         * Excludes a folder if it is the same for all conversations.
         * In search results, conversations from different folders may be selected. In that case we should allow
         * to move them anywhere, even if some conversations may not move.
         */
        excludedFolderKeys() {
            const rootKey = null;
            let excludedFolderKeys = [rootKey];
            if (this.selected.length <= LOOP_PERF_LIMIT) {
                const differentKeys = this.selected.reduce((differentKeys, conversation) => {
                    differentKeys.add(conversation.folderRef.key);
                    return differentKeys;
                }, new Set());
                if (differentKeys.size === 1) {
                    excludedFolderKeys = [...excludedFolderKeys, Array.from(differentKeys)];
                }
            }
            return excludedFolderKeys;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
@import "~@bluemind/styleguide/css/mixins";

.mail-toolbar-selected-conversations-move-action {
    .dropdown-menu {
        min-width: 20vw;
        max-width: 40vw;

        .bm-avatar {
            margin-left: $sp-4;
        }
    }

    .b-dropdown-form {
        padding-top: $sp-1;
    }

    .new-folder .b-dropdown-form {
        padding-left: 0;
        padding-bottom: 0;

        .mail-folder-input input {
            border: none !important;
        }
    }
}
</style>
