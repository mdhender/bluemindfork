<template>
    <div class="mail-search-form" role="search" @keyup.esc="showForm = false" @click.stop>
        <global-events @click="showForm = false" />
        <div class="d-flex mail-search-form-quicksearch">
            <bm-form-input
                v-model="pattern"
                class="flex-fill no-border-right"
                size="sm"
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
                class="toggle-button no-border-left no-box-shadow"
                variant="outline"
                size="sm"
                @click="showForm = !showForm"
            >
                <div class="text-truncate">{{ compressFolderFullName(selectedFolder) }}</div>
                <bm-icon icon="caret-down" size="xs" />
            </bm-button>
        </div>
        <bm-collapse
            id="search-form"
            ref="searchForm"
            v-model="showForm"
            class="search-form position-absolute bg-surface shadow p-5"
        >
            <bm-form
                class="d-flex flex-column h-100"
                @submit.prevent="search"
                @reset.prevent="setSelectedFolder(initialFolder)"
                @drag.stop
            >
                <bm-icon-button class="d-lg-none" variant="compact" size="lg" icon="arrow-back" @click="close" />
                <bm-form-group
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
                        <span class="flex-fill"> {{ translatePath(item.path) }}</span>
                        <mail-mailbox-icon v-if="item.key !== null" no-text :mailbox="mailboxes[item.mailboxRef.key]" />
                    </bm-combo-box>
                </bm-form-group>
                <div class="d-flex modal-footer">
                    <bm-button type="reset" variant="text">
                        {{ $t("common.action.reset") }}
                    </bm-button>
                    <bm-button type="submit" variant="contained-accent" :disabled="!pattern">{{
                        $t("common.action.search")
                    }}</bm-button>
                </div>
            </bm-form>
        </bm-collapse>
    </div>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";
import {
    BmButton,
    BmIconButton,
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
        BmIconButton,
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
    .no-border-right {
        &,
        & input {
            border-right: none !important;
            border-top-right-radius: 0;
            border-bottom-right-radius: 0;
        }
    }
    .no-border-left {
        border-left: none !important;
        border-top-left-radius: 0;
        border-bottom-left-radius: 0;
    }
    .no-box-shadow {
        box-shadow: none !important;
    }

    $toggle-button-padding-right: $sp-4;

    .btn-outline.toggle-button {
        max-width: 50%;
        padding-right: $toggle-button-padding-right;
        .slot-wrapper {
            display: flex;
            align-items: center;
            gap: $sp-3;
        }
    }

    .bm-form-input:focus-within + .btn-outline.toggle-button {
        border: 2 * $input-border-width solid $secondary-fg;
        padding-right: calc(#{$toggle-button-padding-right} - #{$input-border-width});
    }

    input {
        box-shadow: none !important;
    }

    .search-form {
        width: 200%;
        .form-group {
            display: flex;
            align-items: center;
            margin-right: 0;

            & > div {
                padding-right: 0px;
            }
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
            .bm-icon {
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
    }
}
</style>
