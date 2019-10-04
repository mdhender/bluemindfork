<template>
    <div class="mail-toolbar-consult-message">
        <global-events @keydown.tab.capture="forceCloseMoveAutocomplete" />
        <bm-button
            v-if="currentMessage.states.includes('not-seen')"
            variant="none"
            class="unread"
            :aria-label="$tc('mail.actions.mark_read.aria')"
            @click="markAsRead(currentMessage.id)"
        >
            <bm-icon icon="read" size="2x" /> {{ $tc("mail.actions.mark_read") }}
        </bm-button>
        <bm-button
            v-else
            variant="none"
            class="read"
            :aria-label="$tc('mail.actions.mark_unread.aria')"
            @click="markAsUnread(currentMessage.id)"
        >
            <bm-icon icon="unread" size="2x" /> {{ $tc("mail.actions.mark_unread") }}
        </bm-button>
        <bm-dropdown
            ref="move-dropdown"
            :no-caret="true"
            class="h-100 move-message"
            :disabled="disableMove"
            variant="none"
            @shown="openMoveAutocomplete"
            @hide="closeMoveAutocomplete"
        >
            <template slot="button-content" :aria-label="$tc('mail.toolbar.move.aria')">
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
        <bm-button variant="none" :aria-label="$tc('mail.actions.spam.aria')">
            <bm-icon icon="forbidden" size="2x" />
            {{ $tc("mail.actions.spam") }}
        </bm-button>
        <bm-button variant="none" :aria-label="$tc('mail.actions.remove.aria')" @click="remove">
            <bm-icon icon="trash" size="2x" />
            {{ $tc("mail.actions.remove") }}
        </bm-button>
        <bm-button variant="none" :aria-label="$tc('mail.toolbar.more.aria')">
            <bm-icon icon="3dots" size="2x" /> {{ $tc("mail.toolbar.more") }}
        </bm-button>
    </div>
</template>

<script>
import { BmAutocomplete, BmButton, BmDropdown, BmLabelIcon, BmIcon } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import MailFolderIcon from "../MailFolderIcon";

export default {
    name: "MailToolbarConsultMessage",
    components: {
        BmAutocomplete,
        BmButton,
        BmDropdown,
        BmIcon,
        BmLabelIcon,
        GlobalEvents,
        MailFolderIcon
    },
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
        ...mapGetters("mail-webapp/messages", ["messages", "count"]),
        defaultFolders() {
            const defaultFolders = this.$store.getters["mail-webapp/folders/defaultFolders"];
            return [defaultFolders.INBOX, defaultFolders.TRASH].filter(
                folder => folder && folder.uid != this.currentFolderUid
            );
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["markAsRead", "markAsUnread", "move"]),
        remove() {
            this.$router.push("" + (this.nextMessageId || ""));
            this.$store.dispatch("mail-webapp/remove", this.currentMessage.id);
        },
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

.mail-toolbar-consult-message .unread,
.mail-toolbar-consult-message .read {
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
