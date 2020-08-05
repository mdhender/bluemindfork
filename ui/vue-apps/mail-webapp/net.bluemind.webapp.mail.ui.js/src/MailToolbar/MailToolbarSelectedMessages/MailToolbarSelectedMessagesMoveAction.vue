<template>
    <bm-dropdown
        ref="move-dropdown"
        v-bm-tooltip.bottom
        no-caret
        class="h-100 move-message"
        variant="simple-dark"
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
                v-bm-tooltip.left
                class="text-nowrap text-truncate w-100"
                :title="$tc('mail.actions.move.item', 1, { path: item.path })"
                @click="selectFolder(item)"
            >
                <template #icon>
                    <mail-folder-icon no-text :shared="isFolderOfMailshare(item)" :folder="item" />
                </template>
                {{ item.path }}
            </bm-dropdown-item-button>
        </bm-dropdown-autocomplete>
        <bm-dropdown-divider />
        <bm-dropdown-form
            v-if="pattern === ''"
            v-bm-tooltip.left
            class="new-folder position-relative"
            :aria-label="$t('mail.actions.create.folder')"
            :title="$t('mail.actions.create.folder')"
        >
            <mail-folder-input
                class="pl-2 pr-1"
                :submit-on-focusout="false"
                @submit="newFolderName => selectFolder({ name: newFolderName, path: newFolderName })"
                @keydown.left.native.stop
                @keydown.right.native.stop
                @keydown.esc.native.stop
            />
        </bm-dropdown-form>
        <bm-dropdown-item-button
            v-else-if="displayCreateFolderBtnFromPattern"
            v-bm-tooltip.left
            :aria-label="$tc('mail.actions.move.item', 1, { path: pattern })"
            :title="$tc('mail.actions.move.item', 1, { path: pattern })"
            icon="plus"
            @click="selectFolder({ name: pattern, path: pattern })"
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
    BmIcon,
    BmTooltip
} from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import MailFolderIcon from "../../MailFolderIcon";
import MailFolderInput from "../../MailFolderInput";
import ItemUri from "@bluemind/item-uri";

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
    directives: { BmTooltip },
    data() {
        return {
            maxFolders: 10,
            pattern: ""
        };
    },
    computed: {
        ...mapState("mail-webapp", ["currentFolderKey"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapGetters("mail-webapp", ["nextMessageKey", "my"]),
        ...mapState("mail", ["folders", "mailboxes"]),
        displayCreateFolderBtnFromPattern() {
            let pattern = this.pattern;
            if (pattern !== "") {
                pattern = pattern.replace(/\/+/, "/").replace(/^\/?(.*)\/?$/g, "$1");
                return (
                    pattern && !this.matchingFolders.some(match => match.path.toLowerCase() === pattern.toLowerCase())
                );
            }
            return false;
        },
        matchingFolders() {
            if (this.pattern !== "") {
                const filtered = Object.values(this.folders).filter(folder =>
                    folder.path.toLowerCase().includes(this.pattern.toLowerCase())
                );
                if (filtered) {
                    return filtered.slice(0, this.maxFolders);
                }
            }
            return [this.my.INBOX, this.my.TRASH].filter(
                folder => folder && ItemUri.encode(folder.key, folder.mailbox) !== this.currentFolderKey
            );
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["move"]),
        selectFolder(item) {
            this.$router.navigate({ name: "v:mail:message", params: { message: this.nextMessageKey } });
            this.move({ messageKey: this.currentMessageKey, folder: item });
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
            return this.mailboxes[folder.mailbox].type === "mailshares";
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
