<template>
    <bm-dropdown
        ref="move-dropdown"
        v-bm-tooltip.bottom.ds500
        no-caret
        class="h-100 move-message"
        variant="link"
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
        <bm-dropdown-autocomplete-item
            ref="moveAutocomplete"
            v-model.trim="pattern"
            :items="matchingFolders"
            search-icon
            :max-results="maxFolders"
            has-divider-under-input
            @selected="selectFolder"
            @keydown.esc.native="resetPattern"
        >
            <template v-slot="{ item }">
                <mail-folder-icon
                    v-bm-tooltip.left.ds500
                    :shared="item.isShared"
                    :folder="item.value"
                    class="text-nowrap text-truncate w-100"
                    :aria-label="$tc('mail.actions.move.item', 1, { path: item.value.path })"
                    :title="$tc('mail.actions.move.item', 1, { path: item.value.path })"
                >
                    {{ item.value.path }}
                </mail-folder-icon>
            </template>
        </bm-dropdown-autocomplete-item>
        <bm-dropdown-divider />
        <bm-dropdown-form
            v-if="pattern === ''"
            v-bm-tooltip.left.ds500
            class="new-folder position-relative"
            :aria-label="$t('mail.actions.create.folder')"
            :title="$t('mail.actions.create.folder')"
        >
            <mail-folder-input
                :submit-on-focusout="false"
                @submit="newFolderName => selectFolder({ value: { fullName: newFolderName, path: newFolderName } })"
                @keydown.left.native.stop
                @keydown.right.native.stop
            />
        </bm-dropdown-form>
        <bm-dropdown-item-button
            v-else-if="displayCreateFolderBtnFromPattern"
            v-bm-tooltip.left.ds500
            :aria-label="$tc('mail.actions.move.item', 1, { path: pattern })"
            :title="$tc('mail.actions.move.item', 1, { path: pattern })"
            @click="selectFolder({ value: { fullName: pattern, path: pattern } })"
        >
            <bm-label-icon icon="plus">{{ $t("mail.folder.new.from_pattern", [pattern]) }} </bm-label-icon>
        </bm-dropdown-item-button>
    </bm-dropdown>
</template>

<script>
import {
    BmDropdown,
    BmDropdownAutocompleteItem,
    BmDropdownDivider,
    BmDropdownForm,
    BmDropdownItemButton,
    BmIcon,
    BmLabelIcon,
    BmTooltip
} from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import MailFolderIcon from "../../MailFolderIcon";
import MailFolderInput from "../../MailFolderInput";

export default {
    name: "MailToolbarConsultMessageMoveAction",
    components: {
        BmDropdown,
        BmDropdownAutocompleteItem,
        BmDropdownDivider,
        BmDropdownForm,
        BmDropdownItemButton,
        BmIcon,
        BmLabelIcon,
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
        ...mapGetters("mail-webapp", ["nextMessageKey", "my", "mailshares"]),
        matchingFolders() {
            if (this.pattern !== "") {
                return [this.my].concat(this.mailshares).reduce((matches, mailbox) => {
                    return matches.concat(this.filter(mailbox, this.pattern, this.maxFolders - matches.length));
                }, []);
            } else {
                return [this.my.INBOX, this.my.TRASH]
                    .filter(folder => folder && folder.key !== this.currentFolderKey)
                    .map(folder => toFolderItem(folder));
            }
        },
        displayCreateFolderBtnFromPattern() {
            let pattern = this.pattern;
            if (pattern !== "") {
                pattern = pattern.replace(/\/+/, "/").replace(/^\/?(.*)\/?$/g, "$1");
                return (
                    pattern &&
                    !this.matchingFolders.some(match => match.value.fullName.toLowerCase() === pattern.toLowerCase())
                );
            }
            return false;
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
        filter(mailbox, pattern, max) {
            const matches = [];
            if (mailbox.writable) {
                for (let i = 0; i < mailbox.folders.length && matches.length < max; i++) {
                    const folder = mailbox.folders[i];
                    const root = folder.value.parentUid !== null ? mailbox.root : "";
                    if (folder.key !== this.currentFolderKey && folder.match(pattern.replace(root, ""))) {
                        const folderItem = toFolderItem(
                            folder,
                            mailbox.type === "mailshare",
                            root && root + "/" + folder.value.fullName
                        );
                        matches.push(folderItem);
                    }
                }
            }
            return matches;
        },
        resetPattern() {
            this.pattern = "";
        }
    }
};

function toFolderItem(folder, isShared = false, path = undefined) {
    return {
        key: folder.key,
        isShared: !!isShared,
        value: {
            path: path || folder.value.fullName,
            fullName: folder.value.fullName,
            parentUid: folder.value.parentUid,
            name: folder.value.name
        }
    };
}
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.move-message {
    input {
        border: none !important;
    }

    .dropdown-menu {
        min-width: 20vw;
    }

    .bm-dropdown-autocomplete-item form {
        input {
            padding-left: $sp-3;
        }
        padding: 0 !important;
    }

    .dropdown-divider {
        margin: 0 !important;
    }

    .dropdown-item,
    .b-dropdown-form {
        padding-left: $sp-3;
        color: $dark !important;
        outline: none;
        background-color: $surface-bg;

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
        padding-bottom: $sp-1;
        padding-right: $sp-1;
    }
}
</style>
