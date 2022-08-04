<template>
    <div class="mail-search-form" role="search" @keyup.esc="showForm = false" @click.stop>
        <global-events @click="showForm = false" />
        <div class="d-flex mail-search-form-quicksearch">
            <bm-form-input
                v-model="pattern"
                class="flex-fill no-border-right"
                :placeholder="$t('common.search')"
                icon="search"
                resettable
                left-icon
                :aria-label="$t('common.search')"
                autocomplete="off"
                @input="search"
                @keydown.enter="search"
                @reset="reset"
            />
            <bm-button
                ref="toggleButton"
                :title="$t('common.searchAdvanced')"
                class="toggle-button no-border-left no-box-shadow text-truncate"
                variant="outline-neutral"
                @click="showForm = !showForm"
            >
                {{ compressFolderFullName(selectedFolder) }}<bm-icon class="ml-2" icon="caret-down" size="xs" />
            </bm-button>
        </div>
        <bm-collapse
            id="search-form"
            ref="searchForm"
            v-model="showForm"
            class="search-form position-absolute bg-surface shadow-sm p-3 z-index-110"
        >
            <bm-form
                class="d-flex flex-column h-100"
                @submit.prevent="search"
                @reset.prevent="setSelectedFolder(initialFolder)"
                @drag.stop
            >
                <div class="d-lg-none">
                    <bm-button variant="inline-neutral" size="lg" @click="close">
                        <bm-icon icon="arrow-back" />
                    </bm-button>
                </div>
                <bm-form-group
                    class="pr-0 mr-0"
                    label-cols-lg="3"
                    label-cols="6"
                    label-for="folderCombo"
                    :label="$t('mail.search.options.folder.label')"
                >
                    <bm-combo-box
                        id="folderCombo"
                        v-slot="{ item }"
                        v-model="folderPattern"
                        class="w-100"
                        :items="filteredFolders"
                        :max-results="maxFolders"
                        @selected="setSelectedFolder"
                        @close="folderPattern = translatePath(selectedFolder.path)"
                        @icon-click="folderPattern = ''"
                    >
                        <div class="d-flex align-items-center">
                            <span class="flex-fill"> {{ translatePath(item.path) }}</span>
                            <mail-mailbox-icon
                                v-if="item.key !== null"
                                no-text
                                :mailbox="mailboxes[item.mailboxRef.key]"
                            />
                        </div>
                    </bm-combo-box>
                </bm-form-group>
                <div class="d-flex flex-grow-1 align-items-end justify-content-end">
                    <bm-button type="submit" variant="secondary" :disabled="!pattern">{{
                        $t("common.action.search")
                    }}</bm-button>
                    <bm-button type="reset" variant="inline-neutral" class="ml-2">
                        {{ $t("common.action.reset") }}
                    </bm-button>
                </div>
            </bm-form>
        </bm-collapse>
    </div>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";
import {
    BmButton,
    BmCollapse,
    BmComboBox,
    BmForm,
    BmFormGroup,
    BmFormInput,
    BmIcon,
    BmToggle
} from "@bluemind/styleguide";
import { folderUtils, loadingStatusUtils } from "@bluemind/mail";

import debounce from "lodash.debounce";
import GlobalEvents from "vue-global-events";
import { SearchHelper } from "./SearchHelper";
import { MY_SENT, MY_MAILBOX, MY_INBOX, MY_TRASH, FOLDERS } from "~/getters";
const { isSharedRoot, translatePath } = folderUtils;
import { ConversationListStatus } from "~/store/conversationList";
import { SET_CONVERSATION_LIST_STATUS } from "~/mutations";
import { MailRoutesMixin } from "~/mixins";
const { LoadingStatus } = loadingStatusUtils;
import MailMailboxIcon from "./MailMailboxIcon.vue";

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
        GlobalEvents,
        MailMailboxIcon
    },
    directives: {
        BmToggle
    },
    mixins: [MailRoutesMixin],
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
                            ...this.folderRoute(this.selectedFolder).params
                        }
                    }),
                UPDATE_ROUTE_TIMEOUT
            ),
            showSpinner: debounce(
                () => this.SET_CONVERSATION_LIST_STATUS(ConversationListStatus.LOADING),
                SPINNER_TIMEOUT
            ),
            showForm: false,
            maxFolders: 10
        };
    },
    computed: {
        ...mapState("mail", { currentSearch: ({ conversationList }) => conversationList.search }),
        ...mapState("mail", ["folders", "mailboxes", "activeFolder"]),
        ...mapGetters("mail", { MY_INBOX, MY_MAILBOX, MY_SENT, MY_TRASH, FOLDERS }),
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
        cancel() {
            this.updateRoute.cancel();
            this.showSpinner.cancel();
        },
        reset() {
            this.cancel();
            this.SET_CONVERSATION_LIST_STATUS(ConversationListStatus.IDLE);
            this.pattern = null;
            this.$router.navigate({ name: "v:mail:home", params: { search: null } });
            this.setSelectedFolder(this.initialFolder);
        },
        close() {
            this.showForm = false;
        },
        search() {
            this.showForm = false;
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
        compressFolderFullName(item) {
            let text = translatePath(item.path);
            const size = text.length;
            const slashCount = (text.match(/\//g) || []).length;
            if (size > 30 && slashCount > 1) {
                const parts = text.split("/");
                text = parts[0] + "/.../" + parts[parts.length - 1];
            }
            return text;
        },
        setFolderFromKeyOrInitial(folder) {
            if (folder) {
                this.setSelectedFolder(this.folders[folder.key]);
            } else {
                this.setSelectedFolder(this.initialFolder);
            }
        },
        isSharedRootFolder(folder) {
            return isSharedRoot(folder, this.mailboxes[folder.mailboxRef.key]);
        },
        translatePath
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
        border-color: $secondary-fg;
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

    @media (max-width: map-get($grid-breakpoints, "lg")) {
        .mail-search-form-quicksearch {
            .form-control,
            .form-control::placeholder,
            .form-control:focus,
            .toggle-button,
            .toggle-button:focus,
            .toggle-button:active,
            .toggle-button::before,
            .close,
            .icon-wrapper {
                color: $fill-primary-fg !important;
                background: transparent !important;
                border-color: transparent !important;
                border-bottom-color: $fill-primary-fg !important;
                opacity: 1;
            }
        }

        .search-form {
            position: fixed !important;
            top: 0;
            bottom: 0;
            right: 0;
            left: 0;
            width: auto;
        }

        .form-group {
            padding: $sp-2 $sp-3;
            & label {
                margin-top: $sp-1;
            }
        }
    }
}
</style>
