<template>
    <bm-dropdown
        ref="move-dropdown"
        no-caret
        class="h-100 move-message"
        variant="inline-light"
        toggle-class="btn-lg-simple-dark"
        :title="$t('mail.toolbar.move.tooltip')"
        :aria-label="$t('mail.actions.move.aria')"
        @shown="openMoveAutocomplete"
        @hide="resetPattern"
    >
        <global-events @keydown.tab.capture="forceCloseMoveAutocomplete" />
        <template slot="button-content">
            <bm-icon icon="folder" size="2x" />
            <span class="d-none d-lg-block"> {{ $t("mail.actions.move") }}</span>
        </template>
        <bm-dropdown-autocomplete
            ref="moveAutocomplete"
            v-slot="{ item }"
            v-model.trim="pattern"
            :items="matchingFolders"
            search-icon
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
                    <mail-folder-icon no-text :shared="isFolderOfMailshare(item)" :folder="item" />
                </template>
                {{ translatePath(item.path) }}
            </bm-dropdown-item-button>
        </bm-dropdown-autocomplete>
        <bm-dropdown-divider />
        <bm-dropdown-form
            v-if="pattern === ''"
            class="new-folder position-relative"
            :aria-label="$t('mail.actions.create.folder')"
            :title="$t('mail.actions.create.folder')"
        >
            <mail-folder-input
                class="pl-2 pr-1"
                :submit-on-focusout="false"
                @submit="newFolderName => moveToFolder({ name: newFolderName, path: newFolderName })"
                @keydown.left.native.stop
                @keydown.right.native.stop
                @keydown.esc.native.stop
            />
        </bm-dropdown-form>
        <bm-dropdown-item-button
            v-else-if="displayCreateFolderBtnFromPattern"
            :aria-label="$tc('mail.actions.move.item', 1, { path: pattern })"
            :title="$tc('mail.actions.move.item', 1, { path: pattern })"
            icon="plus"
            @click="moveToFolder({ name: pattern, path: pattern })"
        >
            {{ $t("mail.folder.new.from_pattern", [pattern]) }}
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
    BmIcon
} from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import MailFolderIcon from "../../MailFolderIcon";
import MailFolderInput from "../../MailFolderInput";
import { MailboxType } from "~model/mailbox";
import { isNameValid, translatePath } from "~model/folder";
import { ACTIVE_MESSAGE, MY_INBOX, MY_TRASH, FOLDERS_BY_UPPERCASE_PATH } from "~getters";
import { MoveMixin } from "~mixins";

export default {
    name: "MailToolbarConsultMessageMoveAction",
    components: {
        BmDropdown,
        BmDropdownAutocomplete,
        BmDropdownDivider,
        BmDropdownForm,
        BmDropdownItemButton,
        BmIcon,
        GlobalEvents,
        MailFolderIcon,
        MailFolderInput
    },
    mixins: [MoveMixin],
    data() {
        return {
            maxFolders: 10,
            pattern: ""
        };
    },
    computed: {
        ...mapGetters("mail-webapp", ["nextMessageKey"]),
        ...mapState("mail", ["activeFolder", "folders", "mailboxes", "messages", "selection"]),
        ...mapGetters("mail", { ACTIVE_MESSAGE, MY_TRASH, MY_INBOX, FOLDERS_BY_UPPERCASE_PATH }),
        displayCreateFolderBtnFromPattern() {
            let pattern = this.pattern;
            if (pattern !== "") {
                pattern = pattern.replace(/\/+/, "/").replace(/^\/?(.*)\/?$/g, "$1");
                return pattern && isNameValid(pattern, pattern, this.FOLDERS_BY_UPPERCASE_PATH) === true;
            }
            return false;
        },
        matchingFolders() {
            if (this.pattern !== "") {
                const filtered = Object.values(this.folders).filter(
                    folder =>
                        folder.key !== this.activeFolder &&
                        folder.writable &&
                        (folder.path.toLowerCase().includes(this.pattern.toLowerCase()) ||
                            folder.name.toLowerCase().includes(this.pattern.toLowerCase()))
                );
                if (filtered) {
                    return filtered.slice(0, this.maxFolders);
                }
            }
            return [this.MY_INBOX, this.MY_TRASH].filter(folder => folder && folder.key !== this.activeFolder);
        }
    },
    methods: {
        moveToFolder(folder) {
            const selectedMessages = this.selection.map(key => this.messages[key]);
            const toBeMoved = selectedMessages.length > 0 ? selectedMessages : this.ACTIVE_MESSAGE;
            this.MOVE_MESSAGES({ messages: toBeMoved, folder });
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
        isFolderOfMailshare(folder) {
            return this.mailboxes[folder.mailboxRef.key].type === MailboxType.MAILSHARE;
        },
        translatePath(path) {
            return translatePath(path);
        },
        mailbox(folder) {
            return this.mailboxes[folder.mailboxRef.key];
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
@import "~@bluemind/styleguide/css/mixins";

.move-message {
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
            background-color: $extra-light !important;
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
        padding-right: 0;
        padding-left: 0;
        padding-bottom: 0;
    }
}
</style>
