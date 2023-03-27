<template>
    <div class="mail-search-form d-flex" role="search" :class="{ active }" @click.stop>
        <global-events @click="hideForm" @keydown.escape="hideForm" />
        <div class="d-none d-lg-flex flex-fill">
            <div class="d-flex flex-fill mail-search-form-quicksearch" :class="{ active }">
                <mail-search-form-context class="context" />
                <mail-search-field
                    :value.sync="pattern"
                    :size="active ? 'md' : 'sm'"
                    @reset="reset"
                    @search="search"
                    @active="active = true"
                />
            </div>
            <div class="search-button-wrapper">
                <bm-button
                    size="lg"
                    class="search-button"
                    variant="fill-accent"
                    :disabled="!pattern || pattern.length === 0"
                    @click="search"
                >
                    {{ $t("common.action.search") }}
                </bm-button>
            </div>
        </div>
        <div class="d-lg-none">
            <bm-icon-button size="lg" variant="compact-on-fill-primary" icon="search" @click="$emit('showSearch')" />
        </div>
    </div>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import { BmButton, BmIconButton, BmToggle } from "@bluemind/ui-components";
import { folderUtils, loadingStatusUtils } from "@bluemind/mail";

import debounce from "lodash.debounce";
import { SearchHelper } from "./../SearchHelper";
import { CONVERSATION_LIST_IS_FILTERED, MY_SENT, MY_MAILBOX, MY_INBOX, MY_TRASH, FOLDERS } from "~/getters";
import { ConversationListStatus } from "~/store/conversationList";
import { SET_CONVERSATION_LIST_STATUS } from "~/mutations";
import { MailRoutesMixin } from "~/mixins";
import MailSearchField from "./MailSearchField";
import MailSearchFormContext from "./MailSearchFormContext";
const { isSharedRoot, translatePath } = folderUtils;
const { LoadingStatus } = loadingStatusUtils;

const SPINNER_TIMEOUT = 250;
const UPDATE_ROUTE_TIMEOUT = 1000;

export default {
    name: "MailSearchForm",
    components: {
        BmButton,
        BmIconButton,
        GlobalEvents,
        MailSearchField,
        MailSearchFormContext
    },
    directives: {
        BmToggle
    },
    mixins: [MailRoutesMixin],
    data() {
        return {
            active: false,
            initialFolder: null,
            selectedFolder: null,
            folderPattern: "",
            pattern: null,
            maxFolders: 10
        };
    },
    computed: {
        ...mapState("mail", { currentSearch: ({ conversationList }) => conversationList.search }),
        ...mapState("mail", ["folders", "mailboxes", "activeFolder"]),
        ...mapGetters("mail", {
            CONVERSATION_LIST_IS_FILTERED,
            MY_INBOX,
            MY_MAILBOX,
            MY_SENT,
            MY_TRASH,
            FOLDERS
        }),
        filteredFolders() {
            if (this.folderPattern !== "") {
                const filtered = this.FOLDERS.filter(
                    folder =>
                        folder.path.toLowerCase().includes(this.folderPattern.toLowerCase()) ||
                        translatePath(folder.path).toLowerCase().includes(this.folderPattern.toLowerCase())
                );
                if (filtered) {
                    return filtered.slice(0, this.maxFolders);
                }
            }
            return this.suggestedFolders;
        },
        suggestedFolders() {
            if (this.MY_MAILBOX && this.MY_MAILBOX.loading === LoadingStatus.LOADED) {
                return [
                    { key: null, path: this.$t("common.all") },
                    this.MY_INBOX,
                    { ...this.folders[this.activeFolder], ...{ path: this.$t("mail.search.options.folder.current") } },
                    this.MY_SENT,
                    this.MY_TRASH
                ];
            } else {
                return [{ key: null, path: this.$t("common.all") }];
            }
        },
        searchQuery() {
            let searchQuery = '"' + this.pattern + '"';
            if (this.selectedFolder.key && !this.isSharedRootFolder(this.selectedFolder)) {
                searchQuery += " AND in:" + this.selectedFolder.key;
            }
            return searchQuery;
        }
    },
    watch: {
        currentSearch: {
            async handler({ pattern, folder }) {
                this.pattern = pattern;
                this.setFolderFromKeyOrInitial(folder);
            },
            deep: true
        },
        active(val) {
            this.$emit("searchMode", val);
        }
    },
    async created() {
        this.pattern = this.currentSearch.pattern;
        this.initialFolder = this.suggestedFolders[0];
        this.setFolderFromKeyOrInitial(this.currentSearch.folder);
    },
    methods: {
        ...mapMutations("mail", { SET_CONVERSATION_LIST_STATUS }),
        filter(folder, { root }) {
            return folder.path.match(this.folderPattern, root);
        },
        setSelectedFolder(item) {
            this.selectedFolder = item;
            this.folderPattern = translatePath(item.path);
        },
        cancelSpinner() {
            this.SET_CONVERSATION_LIST_STATUS(ConversationListStatus.SUCCESS);
        },
        reset() {
            this.cancel();
            this.pattern = null;
            this.$router.navigate({ name: "v:mail:home", params: { search: null } });
            this.setSelectedFolder(this.initialFolder);
        },
        cancel() {
            this.cancelRoute();
            this.cancelSpinner();
        },
        cancelRoute() {
            this.$router.navigate({
                name: "v:mail:home"
            });
        },
        hideForm() {
            if (!this.pattern) {
                this.active = false;
            }
        },
        search() {
            if (this.pattern) {
                if (
                    !SearchHelper.isSameSearch(
                        this.currentSearch.pattern,
                        this.currentSearch.folder && this.currentSearch.folder.key,
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
        setFolderFromKeyOrInitial(folder) {
            if (folder) {
                this.setSelectedFolder(this.folders[folder.key]);
            } else {
                this.setSelectedFolder(this.initialFolder);
            }
        },
        showSpinner: debounce(function () {
            this.SET_CONVERSATION_LIST_STATUS(ConversationListStatus.LOADING), SPINNER_TIMEOUT;
        }),
        isSharedRootFolder(folder) {
            return isSharedRoot(folder, this.mailboxes[folder.mailboxRef.key]);
        },
        updateRoute: debounce(function () {
            this.$router.navigate({
                name: "v:mail:home",
                params: {
                    search: this.searchQuery,
                    ...this.folderRoute(this.selectedFolder).params
                }
            }),
                UPDATE_ROUTE_TIMEOUT;
        }),
        translatePath
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/mixins/_responsiveness";
@import "~@bluemind/ui-components/src/css/variables";

.mail-search-form {
    &:not(.active) {
        .search-button-wrapper {
            display: none;
        }
        .mail-search-form-context {
            display: none;
        }
    }
    .mail-search-form-quicksearch {
        outline: $input-border-width solid $neutral-fg;
        outline-offset: -$input-border-width;

        &:focus-within {
            outline: 2 * $input-border-width solid $secondary-fg;
            outline-offset: -2 * $input-border-width;
        }
    }
    .search-button-wrapper {
        background: $surface;
        margin-left: -$input-border-width * 2;
    }
}

@include until-lg {
    .mail-search-form-quicksearch {
        .form-control,
        .form-control::placeholder,
        .form-control:focus,
        .toggle-button,
        .toggle-button:focus,
        .toggle-button:active,
        .toggle-button::before,
        .close,
        .bm-icon {
            color: $fill-primary-fg !important;
            background: transparent !important;
        }
    }
}
</style>
