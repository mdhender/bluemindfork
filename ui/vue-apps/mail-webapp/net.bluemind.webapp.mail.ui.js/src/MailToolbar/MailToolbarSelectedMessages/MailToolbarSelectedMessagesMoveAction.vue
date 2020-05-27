<template>
    <bm-dropdown
        ref="move-dropdown"
        v-bm-tooltip.bottom.ds500
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
                v-bm-tooltip.left.ds500
                class="text-nowrap text-truncate w-100"
                :title="$tc('mail.actions.move.item', 1, { path: item.value.path })"
                @click="selectFolder(item)"
            >
                <template #icon>
                    <mail-folder-icon no-text :shared="item.isShared" :folder="item.value" />
                </template>
                {{ item.value.path }}
            </bm-dropdown-item-button>
        </bm-dropdown-autocomplete>
        <bm-dropdown-divider />
        <bm-dropdown-form
            v-if="pattern === ''"
            v-bm-tooltip.left.ds500
            class="new-folder position-relative"
            :aria-label="$t('mail.actions.create.folder')"
            :title="$t('mail.actions.create.folder')"
        >
            <mail-folder-input
                class="pl-2 pr-1"
                :submit-on-focusout="false"
                @submit="newFolderName => selectFolder({ value: { fullName: newFolderName, path: newFolderName } })"
                @keydown.left.native.stop
                @keydown.right.native.stop
                @keydown.esc.native.stop
            />
        </bm-dropdown-form>
        <bm-dropdown-item-button
            v-else-if="displayCreateFolderBtnFromPattern"
            v-bm-tooltip.left.ds500
            :aria-label="$tc('mail.actions.move.item', 1, { path: pattern })"
            :title="$tc('mail.actions.move.item', 1, { path: pattern })"
            icon="plus"
            @click="selectFolder({ value: { fullName: pattern, path: pattern } })"
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

    .bm-dropdown-autocomplete form {
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
