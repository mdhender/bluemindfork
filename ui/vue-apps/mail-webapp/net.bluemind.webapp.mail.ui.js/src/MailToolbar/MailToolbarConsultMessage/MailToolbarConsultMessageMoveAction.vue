<template>
    <bm-dropdown
        ref="move-dropdown"
        v-bm-tooltip.bottom.ds500
        :no-caret="true"
        class="h-100 move-message"
        variant="link"
        :title="$tc('mail.toolbar.move.tooltip')"
        :aria-label="$tc('mail.toolbar.move.aria')"
        @shown="openMoveAutocomplete"
        @hide="pattern = ''"
    >
        <global-events @keydown.tab.capture="forceCloseMoveAutocomplete" />
        <template slot="button-content">
            <bm-icon icon="folder" size="2x" /> <span class="d-none d-lg-block"> {{ $tc("mail.toolbar.move") }}</span>
        </template>
        <bm-autocomplete
            ref="moveAutocomplete"
            v-model="pattern"
            class="autocomplete-folders shadow-sm"
            :items="matchingFolders"
            search-icon
            :max-results="maxFolders + 1"
            @selected="selectFolder"
            @keydown.esc.native="pattern = ''"
        >
            <template v-slot="f">
                <div
                    v-bm-tooltip.bottom.ds500
                    class="text-nowrap text-truncate"
                    :aria-label="$tc('mail.toolbar.move.item.aria', 1, f.item.value)"
                    :title="$tc('mail.toolbar.move.item', 1, { path: f.item.value.path })"
                >
                    <mail-folder-icon v-if="f.item.key" :shared="f.item.isShared" :folder="f.item.value">
                        {{ f.item.value.path }}
                    </mail-folder-icon>
                    <bm-label-icon v-else icon="plus">
                        {{ $t("mail.folder.new", [f.item.value.path]) }}
                    </bm-label-icon>
                </div>
            </template>
        </bm-autocomplete>
    </bm-dropdown>
</template>

<script>
import { BmAutocomplete, BmDropdown, BmIcon, BmLabelIcon, BmTooltip } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import MailFolderIcon from "../../MailFolderIcon";

export default {
    name: "MailToolbarConsultMessageMoveAction",
    components: {
        BmAutocomplete,
        BmDropdown,
        BmIcon,
        BmLabelIcon,
        GlobalEvents,
        MailFolderIcon
    },
    directives: { BmTooltip },
    data() {
        return {
            maxFolders: 5, // FIXME ?
            pattern: ""
        };
    },
    computed: {
        ...mapState("mail-webapp", ["currentMessageKey", "currentFolderKey"]),
        ...mapGetters("mail-webapp", ["nextMessageKey", "my", "mailshares"]),
        ...mapGetters("mail-webapp/folders", ["folders"]),
        matchingFolders() {
            let pattern = this.pattern.trim();
            if (pattern !== "") {
                const matches = [this.my].concat(this.mailshares).reduce((matches, mailbox) => {
                    return matches.concat(this.filter(mailbox, pattern, this.maxFolders - matches.length));
                }, []);
                pattern = pattern.replace(/\/+/, "/").replace(/^\/?(.*)\/?$/g, "$1");
                if (pattern && !matches.some(match => match.value.fullName.toLowerCase() === pattern.toLowerCase())) {
                    matches.push({ value: { fullName: pattern, path: pattern } });
                }
                return matches;
            } else {
                return [this.my.INBOX, this.my.TRASH]
                    .filter(folder => folder && folder.key !== this.currentFolderKey)
                    .map(f => toFolderItem(f));
            }
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["move"]),
        selectFolder(item) {
            this.$router.push("" + (this.nextMessageKey || ""));

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
                        matches.push(
                            toFolderItem(
                                folder,
                                mailbox.type === "mailshare",
                                root && root + "/" + folder.value.fullName
                            )
                        );
                    }
                }
            }
            return matches;
        }
    }
};

function toFolderItem(folder, isShared, path) {
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
    padding-top: 0;
    padding-bottom: 0;
    border: none;
}

.move-message .dropdown-menu {
    border: none !important;
    padding: 0 !important;
    margin-top: $sp-1 !important;
}

.move-message .btn.dropdown-toggle:hover {
    background-color: $extra-light !important;
    border-color: $extra-light !important;
}
</style>
