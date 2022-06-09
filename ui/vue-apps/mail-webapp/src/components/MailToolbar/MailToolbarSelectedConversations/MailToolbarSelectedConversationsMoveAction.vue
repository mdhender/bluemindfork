<template>
    <bm-dropdown
        ref="move-dropdown"
        no-caret
        class="mail-toolbar-selected-conversations-move-action h-100"
        variant="inline-on-fill-primary"
        toggle-class="btn-lg-simple-neutral"
        :title="moveAriaText()"
        :aria-label="moveAriaText()"
        @shown="openMoveAutocomplete"
        @hide="resetPattern"
    >
        <global-events @keydown.tab.capture="forceCloseMoveAutocomplete" />
        <template slot="button-content">
            <bm-icon icon="folder" size="2x" />
            <span class="d-none d-lg-block"> {{ moveText }}</span>
        </template>
        <bm-dropdown-autocomplete
            ref="moveAutocomplete"
            v-slot="{ item }"
            v-model.trim="pattern"
            :items="pattern ? matchingFolders(isExcluded) : [MY_TRASH, MY_INBOX]"
            icon="search"
            :max-results="maxFolders"
            has-divider-under-input
            @keydown.esc.native="resetPattern"
        >
            <bm-dropdown-item-button
                class="text-nowrap text-truncate w-100"
                :title="$tc('mail.actions.move.item', 1, { path: item.path })"
                @click="moveToFolder(item)"
            >
                <template #icon>
                    <mail-folder-icon no-text :shared="isSharedMailbox(item)" :folder="item" />
                </template>
                <div class="d-flex align-items-center">
                    <span class="flex-fill"> {{ translatePath(item.path) }}</span>
                    <mail-mailbox-icon no-text :mailbox="mailboxes[item.mailboxRef.key]" />
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
                <mail-mailbox-icon no-text :mailbox="MY_MAILBOX" />
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
                <mail-mailbox-icon no-text :mailbox="MY_MAILBOX" />
            </div>
        </bm-dropdown-item-button>
    </bm-dropdown>
</template>

<script>
import {
    BmDropdown,
    BmDropdownAutocomplete,
    BmDropdownDivider,
    BmDropdownForm,
    BmDropdownItemButton,
    BmIcon,
    BmNotice
} from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import { folder, mailbox } from "@bluemind/mail";
import MailFolderIcon from "../../MailFolderIcon";
import MailMailboxIcon from "../../MailMailboxIcon";
import MailFolderInput from "../../MailFolderInput";
import { MY_MAILBOX, FOLDERS_BY_PATH, MY_TRASH, MY_INBOX } from "~/getters";
import { ActionTextMixin, FilterFolderMixin, MoveMixin, SelectionMixin } from "~/mixins";

const { getInvalidCharacter, isNameValid, translatePath } = folder;
const { MailboxType } = mailbox;
const LOOP_PERF_LIMIT = 100;

export default {
    name: "MailToolbarSelectedConversationsMoveAction",
    components: {
        BmDropdown,
        BmDropdownAutocomplete,
        BmDropdownDivider,
        BmDropdownForm,
        BmDropdownItemButton,
        BmNotice,
        BmIcon,
        GlobalEvents,
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
        isSharedMailbox(folder) {
            return this.mailboxes[folder.mailboxRef.key].type === MailboxType.MAILSHARE;
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
    input {
        border: none !important;
    }

    .dropdown-menu {
        min-width: 20vw;
        max-width: 40vw;
    }

    .bm-dropdown-autocomplete form {
        input {
            padding-left: $sp-3;
        }
        padding: 0 !important;
    }

    .dropdown-divider {
        margin: 0 !important;
    }

    .dropdown-item-content {
        @include text-overflow;
    }

    .dropdown-item,
    .b-dropdown-form {
        &:hover {
            background-color: $neutral-bg-lo1 !important;
        }

        &:focus,
        &:focus:hover {
            background-color: $surface-active-bg !important;
        }
    }

    .b-dropdown-form {
        padding-top: $sp-1;
    }

    .new-folder .b-dropdown-form {
        padding-left: 0;
        padding-bottom: 0;
    }
}
</style>
