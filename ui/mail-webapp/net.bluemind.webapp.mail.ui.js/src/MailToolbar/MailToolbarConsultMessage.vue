<template>
    <div class="mail-toolbar-consult-message">
        <global-events @keydown.tab.capture="forceCloseMoveAutocomplete" />
        <bm-button
            v-if="message.states.includes('not-seen')"
            variant="none"
            class="unread"
            :aria-label="$tc('mail.actions.mark_read.aria')"
            @click="updateSeen({ folder, id: message.id, isSeen: true })"
        >
            <bm-icon icon="read" size="2x" /> {{ $tc("mail.actions.mark_read") }}
        </bm-button>
        <bm-button
            v-else
            variant="none"
            class="read"
            :aria-label="$tc('mail.actions.mark_unread.aria')"
            @click="updateSeen({ folder, id: message.id, isSeen: false })"
        >
            <bm-icon icon="unread" size="2x" /> {{ $tc("mail.actions.mark_unread") }}
        </bm-button>
        <bm-dropdown 
            ref="move-dropdown"
            :no-caret="true"
            class="h-100 move-message"
            @shown="openMoveAutocomplete"
            @hide="closeMoveAutocomplete"
        >
            <template slot="button-content" variant="none" :aria-label="$tc('mail.toolbar.move.aria')">
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
                    <div class="text-nowrap text-truncate"><mail-folder-icon :folder="f.item" /></div>
                </template>
            </bm-autocomplete>
        </bm-dropdown>
        <bm-button variant="none" :aria-label="$tc('mail.actions.spam.aria')">
            <bm-icon icon="forbidden" size="2x" />
            {{ $tc("mail.actions.spam") }}
        </bm-button>
        <bm-button variant="none" :aria-label="$tc('mail.actions.remove.aria')" @click="shouldRemoveItem(message.id)">
            <bm-icon icon="trash" size="2x" />
            {{ $tc("mail.actions.remove") }}
        </bm-button>
        <bm-button variant="none" :aria-label="$tc('mail.toolbar.more.aria')">
            <bm-icon icon="3dots" size="2x" /> {{ $tc("mail.toolbar.more") }}
        </bm-button>
    </div>
</template>

<script>
import { BmAutocomplete, BmButton, BmDropdown, BmIcon }  from "@bluemind/styleguide";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import GlobalEvents from 'vue-global-events';
import MailFolderIcon from "../MailFolderIcon";

export default {
    name: "MailToolbarConsultMessage",
    components: {
        BmAutocomplete,
        BmButton,
        BmDropdown,
        BmIcon,
        GlobalEvents,
        MailFolderIcon
    },
    data() {
        return {
            maxFoldersProposed: 5, // FIXME ?
            matchingFolders: undefined,
            searchFolderPattern: "",
            newFolderPattern: " (" + this.$t("mail.folder.new") +")"
        };
    },
    computed: {
        ...mapGetters("backend.mail/items", { message: "currentMessage", messages: "messages" }),
        ...mapGetters("backend.mail/folders", ["flat", "currentFolder"] ),
        ...mapState("backend.mail/items", ["count"]),
        defaultFolders() {
            return this.flat.filter(
                folder => (folder.name === "INBOX" || folder.name === "Trash") && folder.uid !== this.currentFolder
            );
        }
    },
    methods: {
        ...mapActions("backend.mail/items", ["updateSeen", "move"]),
        ...mapMutations("backend.mail/items", ["shouldRemoveItem"]),
        searchFolder(input) {
            if (input !== "") {
                this.searchFolderPattern = input;
                this.matchingFolders = [];
                this.flat.forEach(folder => {
                    if (this.isMatching(folder, input)) {
                        this.matchingFolders.push(folder);
                    }
                });
                this.matchingFolders = this.matchingFolders.slice(0, this.maxFoldersProposed);
                this.matchingFolders.push({ 
                    type: "create-folder",
                    name: input + this.newFolderPattern,
                    icon: "plus"
                });
            } else {
                this.matchingFolders = undefined;
            }
        },
        isMatching(folder, input) {
            return (folder.name.toLowerCase().includes(input.toLowerCase())) && (folder.uid !== this.currentFolder);
        },
        selectFolder(item) {
            const mailId = this.message.id;
            const index = this.messages.findIndex(message => mailId === message.id);

            this.move({ item, mailId, index, newFolderPattern: this.newFolderPattern });

            // Be careful : remove item mutation is synchronous in move action.
            //    --> item must be considered as already deleted here
            if (this.current !== null) {
                if (this.count === 0) {
                    this.$router.push('/mail/' + this.currentFolder + '/');
                } else if (this.count === index) {
                    this.$router.push('/mail/' + this.currentFolder + '/' + this.messages[index - 1].id);
                } else {
                    this.$router.push('/mail/' + this.currentFolder + '/' + this.messages[index].id);
                }
            }
        },
        openMoveAutocomplete() {
            this.$nextTick(() => this.$refs["moveAutocomplete"].focus());
        },
        closeMoveAutocomplete() {
            this.searchFolderPattern = "";
            this.matchingFolders = undefined;
        },
        forceCloseMoveAutocomplete() {
            this.$refs['move-dropdown'].hide(true);
            this.closeMoveAutocomplete();
        }
    }
};
</script>

<style lang="scss">
@import '~@bluemind/styleguide/css/_variables';

.mail-toolbar-consult-message .unread, .mail-toolbar-consult-message .read {
    width: 8rem;
}

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

.move-message .btn.dropdown-toggle:enabled {
    background-color: unset;
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
