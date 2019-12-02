<template>
    <bm-dropdown
        ref="move-dropdown"
        v-bm-tooltip.bottom.ds500
        :no-caret="true"
        class="h-100 move-message"
        :disabled="disableMove"
        variant="link"
        :title="$tc('mail.toolbar.move.aria')"
        :aria-label="$tc('mail.toolbar.move.aria')"
        @shown="openMoveAutocomplete"
        @hide="closeMoveAutocomplete"
    >
        <template slot="button-content"> <bm-icon icon="folder" size="2x" /> {{ $tc("mail.toolbar.move") }} </template>
        <bm-autocomplete
            ref="moveAutocomplete"
            class="autocomplete-folders shadow-sm"
            :value="searchFolderPattern"
            :items="matchingFolders === undefined ? defaultFolders : matchingFolders"
            search-icon
            :max-results="maxFoldersProposed + 1"
            @input="searchFolder"
            @selected="selectFolder"
            @keydown.esc.native="closeMoveAutocomplete"
        >
            <template v-slot="f">
                <div class="text-nowrap text-truncate">
                    <mail-folder-icon v-if="f.item.uid" :folder="f.item.value" />
                    <bm-label-icon v-else icon="plus">{{ f.item.displayname }}</bm-label-icon>
                </div>
            </template>
        </bm-autocomplete>
    </bm-dropdown>
</template>

<script>
import { BmAutocomplete, BmDropdown, BmIcon, BmLabelIcon, BmTooltip } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import MailFolderIcon from "../../MailFolderIcon";

export default {
    name: "MailToolbarConsultMessageMoveAction",
    components: {
        BmAutocomplete,
        BmDropdown,
        BmIcon,
        BmLabelIcon,
        MailFolderIcon
    },
    directives: { BmTooltip },
    data() {
        return {
            maxFoldersProposed: 5, // FIXME ?
            matchingFolders: undefined,
            searchFolderPattern: "",
            newFolderPattern: " (" + this.$t("mail.folder.new") + ")",
            disableMove: false
        };
    },
    computed: {
        ...mapState("mail-webapp", ["currentMessageKey", "currentFolderKey"]),
        ...mapGetters("mail-webapp", ["nextMessageKey", "my"]),
        defaultFolders() {
            return [this.my.INBOX, this.my.TRASH].filter(folder => folder && folder.key != this.currentFolderKey);
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["move"]),
        searchFolder(input) {
            if (input !== "") {
                this.searchFolderPattern = input;
                this.matchingFolders = [];
                this.my.folders.forEach(folder => {
                    if (this.isMatching(folder, input)) {
                        this.matchingFolders.push(folder);
                    }
                });
                this.matchingFolders.splice(this.maxFoldersProposed, this.matchingFolders.length);
                this.matchingFolders.push({
                    displayname: input + this.newFolderPattern,
                    value: {
                        name: input
                    }
                });
            } else {
                this.matchingFolders = undefined;
            }
        },
        isMatching(folder, input) {
            return (
                folder.value.name.toLowerCase().includes(input.toLowerCase()) && folder.key !== this.currentFolderKey
            );
        },
        selectFolder(item) {
            this.disableMove = true;
            this.$router.push("" + (this.nextMessageKey || ""));

            this.move({ messageKey: this.currentMessageKey, folder: item }).finally(() => (this.disableMove = false));
            this.$refs["move-dropdown"].hide(true);
        },
        openMoveAutocomplete() {
            this.$nextTick(() => this.$refs["moveAutocomplete"].focus());
        },
        closeMoveAutocomplete() {
            this.searchFolderPattern = "";
            this.matchingFolders = undefined;
        },
        forceCloseMoveAutocomplete() {
            this.$refs["move-dropdown"].hide(true);
            this.closeMoveAutocomplete();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.autocomplete-folders .list-group {
    border-top: 1px solid $primary;
}

.autocomplete-folders input {
    border: none !important;
    position: relative;
}

.autocomplete-folders .search-icon {
    z-index: 2;
}

.move-message .btn.dropdown-toggle {
    padding: 0;
    border: none;
}

.move-message .dropdown-menu {
    border: none !important;
    padding: 0 !important;
    margin-top: map-get($spacers, 1) !important;
}

.move-message .btn.dropdown-toggle:hover {
    background-color: $extra-light !important;
    border-color: $extra-light !important;
}
</style>
