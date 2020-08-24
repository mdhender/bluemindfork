<template>
    <div class="mail-search-form" @keyup.esc="showForm = false" @click.stop>
        <global-events @click="showForm = false" />
        <div class="d-flex">
            <bm-form-input
                v-model="pattern"
                class="flex-fill no-border-right"
                :placeholder="$t('common.search')"
                icon="search"
                left-icon
                :aria-label="$t('common.search')"
                autocomplete="off"
                @input="search"
                @keydown.enter="search"
                @reset="reset"
            >
            </bm-form-input>
            <bm-button
                ref="toggleButton"
                v-bm-tooltip
                class="toggle-button no-border-left no-box-shadow text-truncate"
                variant="outline-secondary"
                :title="selectedFolder.path"
                @click="showForm = !showForm"
            >
                {{ compressFolderFullName(selectedFolder) }}<bm-icon class="ml-2" icon="caret-down" size="sm" />
            </bm-button>
        </div>
        <bm-collapse
            id="search-form"
            ref="searchForm"
            v-model="showForm"
            class="search-form position-absolute bg-surface shadow-sm p-3"
        >
            <bm-form @submit.prevent="search" @reset.prevent="setSelectedFolder(initialFolder)" @drag.stop>
                <bm-form-group
                    class="pr-0 mr-0"
                    label-cols="3"
                    label-for="folderCombo"
                    :label="$t('mail.search.options.folder.label')"
                >
                    <bm-combo-box
                        id="folderCombo"
                        v-slot="{ item }"
                        v-model="folderPattern"
                        class="w-100 z-index-110"
                        :items="filteredFolders"
                        :max-results="maxFolders"
                        @selected="setSelectedFolder"
                        @close="folderPattern = selectedFolder.path"
                    >
                        {{ item.path }}
                    </bm-combo-box>
                </bm-form-group>
                <div class="float-right">
                    <bm-button type="submit" variant="primary">{{ $t("common.action.search") }}</bm-button>
                    <bm-button type="reset" variant="inline-secondary">{{ $t("common.action.reset") }}</bm-button>
                </div>
            </bm-form>
        </bm-collapse>
    </div>
</template>

<script>
import {
    BmButton,
    BmCollapse,
    BmComboBox,
    BmForm,
    BmFormGroup,
    BmFormInput,
    BmIcon,
    BmToggle,
    BmTooltip
} from "@bluemind/styleguide";
import { mapGetters, mapMutations, mapState } from "vuex";
import debounce from "lodash.debounce";
import GlobalEvents from "vue-global-events";
import { SearchHelper } from "../store.deprecated/SearchHelper";
import { FolderAdaptor } from "../store/folders/helpers/FolderAdaptor";

const SPINNER_TIMEOUT = 250;
const UPDATE_ROUTE_TIMEOUT = 1000;

export default {
    name: "MailSearchForm",
    components: {
        BmButton,
        BmCollapse,
        BmComboBox,
        BmForm,
        BmFormGroup,
        BmFormInput,
        BmIcon,
        GlobalEvents
    },
    directives: {
        BmToggle,
        BmTooltip
    },
    data() {
        return {
            initialFolder: null,
            selectedFolder: null,
            folderPattern: "",
            pattern: null,
            updateRoute: debounce(
                () =>
                    this.$router.navigate({
                        name: "v:mail:home",
                        params: {
                            search: this.searchQuery,
                            folder:
                                this.selectedFolder.key && !this.isFolderOfMailshare(this.selectedFolder)
                                    ? this.selectedFolder.key
                                    : undefined,
                            mailshare:
                                this.selectedFolder.key && this.isFolderOfMailshare(this.selectedFolder)
                                    ? this.selectedFolder.key
                                    : undefined
                        }
                    }),
                UPDATE_ROUTE_TIMEOUT
            ),
            showSpinner: debounce(() => this.setStatus("loading"), SPINNER_TIMEOUT),
            showForm: false,
            maxFolders: 10
        };
    },

    computed: {
        ...mapState("mail-webapp/search", { currentSearchPattern: "pattern", currentSearchedFolder: "searchFolder" }),
        ...mapState("mail", ["folders", "mailboxes", "activeFolder"]),
        ...mapGetters("mail", ["MY_INBOX", "MY_SENT", "MY_TRASH"]),
        filteredFolders() {
            if (this.folderPattern !== "") {
                const filtered = Object.values(this.folders).filter(folder =>
                    folder.path.toLowerCase().includes(this.folderPattern.toLowerCase())
                );
                if (filtered) {
                    return filtered.slice(0, this.maxFolders);
                }
            }
            return this.suggestedFolders;
        },
        suggestedFolders() {
            return [
                { key: null, path: this.$t("common.all") },
                this.MY_INBOX,
                { ...this.folders[this.activeFolder], ...{ path: this.$t("mail.search.options.folder.current") } },
                this.MY_SENT,
                this.MY_TRASH
            ];
        },
        searchQuery() {
            let searchQuery = '"' + this.pattern + '"';
            if (this.selectedFolder.key && !this.isMailshareRootFolder(this.selectedFolder)) {
                searchQuery += " AND in:" + this.selectedFolder.key;
            }
            return searchQuery;
        }
    },
    watch: {
        currentSearchPattern(value) {
            this.pattern = value;
        },
        currentSearchedFolder(key) {
            this.setFolderFromKeyOrInitial(key);
        }
    },
    created() {
        this.pattern = this.currentSearchPattern;
        this.initialFolder = this.suggestedFolders[0];
        this.setFolderFromKeyOrInitial(this.currentSearchedFolder);
    },
    methods: {
        ...mapMutations("mail-webapp/search", ["setStatus"]),
        filter(folder, { root }) {
            return folder.path.match(this.folderPattern, root);
        },
        setSelectedFolder(item) {
            this.selectedFolder = item;
            this.folderPattern = item.path;
        },
        cancel() {
            this.updateRoute.cancel();
            this.showSpinner.cancel();
        },
        reset() {
            this.cancel();
            this.setStatus("idle");
            this.pattern = null;
            this.$router.navigate({ name: "v:mail:home", params: { search: null } });
            this.setSelectedFolder(this.initialFolder);
        },
        search() {
            this.showForm = false;
            if (this.pattern) {
                if (
                    !SearchHelper.isSameSearch(
                        this.currentSearchPattern,
                        this.currentSearchedFolder,
                        this.pattern,
                        this.selectedFolder.key
                    )
                ) {
                    this.showSpinner();
                    this.updateRoute();
                } else {
                    this.cancel();
                }
            } else {
                this.reset();
            }
        },
        compressFolderFullName(item) {
            let text = item.path;
            const size = text.length;
            const slashCount = (text.match(/\//g) || []).length;
            if (size > 30 && slashCount > 1) {
                const parts = text.split("/");
                text = parts[0] + "/.../" + parts[parts.length - 1];
            }
            return text;
        },
        setFolderFromKeyOrInitial(key) {
            const folder = this.folders[key];
            this.setSelectedFolder(folder || this.initialFolder);
        },
        isMailshareRootFolder(folder) {
            return FolderAdaptor.isMailshareRoot(folder, this.mailboxes[folder.mailbox]);
        },
        isFolderOfMailshare(folder) {
            return this.mailboxes[folder.mailbox].type === "mailshares";
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-search-form {
    .no-border-right input {
        border-right: none;
        border-top-right-radius: 0;
        border-bottom-right-radius: 0;
    }

    .toggle-button {
        &.no-border-left {
            border-left: none;
            border-top-left-radius: 0;
            border-bottom-left-radius: 0;
        }
        &.no-box-shadow {
            box-shadow: none;
        }
        max-width: 50%;
    }

    .bm-form-input:focus-within + .btn {
        border-color: $primary;
    }

    .dropdown-menu {
        position: absolute !important;
        top: 0 !important;
        left: 0px !important;
        line-height: 2;
    }

    button.close {
        line-height: 0.9;
    }

    input {
        box-shadow: none !important;
    }

    .search-form {
        width: 200%;
        .custom-select {
            background: none;
            option {
                line-height: 2 !important;
            }
        }
        .form-group > div {
            padding-right: 0px;
        }
    }
}
</style>
