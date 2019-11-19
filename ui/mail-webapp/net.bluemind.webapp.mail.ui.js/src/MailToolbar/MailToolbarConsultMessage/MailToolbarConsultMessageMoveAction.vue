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
        <template slot="button-content">
            <bm-icon icon="folder" size="2x" /> {{ $tc("mail.toolbar.move") }}
        </template>
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
    directives: {BmTooltip},
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
        ...mapState("mail-webapp", ["currentFolderUid"]),
        ...mapState("mail-webapp/folders", ["items"]),
        ...mapGetters("mail-webapp", ["currentMessage", "nextMessageId"]),
        defaultFolders() {
            const defaultFolders = this.$store.getters["mail-webapp/folders/defaultFolders"];
            return [defaultFolders.INBOX, defaultFolders.TRASH].filter(
                folder => folder && folder.uid != this.currentFolderUid
            );
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["move"]),
        searchFolder(input) {
            if (input !== "") {
                this.searchFolderPattern = input;
                this.matchingFolders = [];
                this.items.forEach(folder => {
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
                folder.value.name.toLowerCase().includes(input.toLowerCase()) && folder.uid !== this.currentFolderUid
            );
        },
        selectFolder(item) {
            this.disableMove = true;
            const destination = { name: item.value.name, uid: item.uid };
            this.$router.push("" + (this.nextMessageId || ""));

            this.move({ messageId: this.currentMessage.id, folder: destination }).finally(
                () => (this.disableMove = false)
            );
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