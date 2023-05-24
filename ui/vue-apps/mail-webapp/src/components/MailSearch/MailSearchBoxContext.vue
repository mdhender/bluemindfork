<template>
    <bm-form-select
        v-if="selectedOption"
        :value="selectedOption"
        :auto-min-width="false"
        :options="Object.values(OPTIONS)"
        class="mail-search-box-context"
        @input="update"
    >
        <template #header>
            <folder-tree-header v-if="CURRENT_MAILBOX" :mailbox="CURRENT_MAILBOX" />
        </template>
        <template #selected="{ selected }">
            <bm-label-icon :inline="false" class="selected" :icon="getIcon(selected.value)">
                {{ $t(`mail.search.context.${selected.value}`, { folder: folder.name }) }}
            </bm-label-icon>
        </template>
        <template #item="{ item }">
            <bm-label-icon :inline="false" :icon="getIcon(item.value)">
                {{ $t(`mail.search.context.${item.value}`, { folder: folder.name }) }}
            </bm-label-icon>
        </template>
    </bm-form-select>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";
import { BmFormSelect, BmLabelIcon } from "@bluemind/ui-components";
import { folderUtils } from "@bluemind/mail";
import { CURRENT_MAILBOX } from "~/getters";
import { SET_CURRENT_SEARCH_FOLDER, SET_CURRENT_SEARCH_DEEP } from "~/mutations";
import FolderTreeHeader from "../MailFolder/FolderTreeHeader";

const { DEFAULT_FOLDERS, folderIcon } = folderUtils;
const OPTIONS = {
    ALL: "all",
    CURRENT_FOLDER_AND_DESCENDANTS: "current_folder_and_descendants",
    CURRENT_FOLDER: "current_folder"
};

export default {
    name: "MailSearchBoxContext",
    components: { BmFormSelect, BmLabelIcon, FolderTreeHeader },
    props: {
        folder: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            OPTIONS
        };
    },
    computed: {
        ...mapState("mail", { currentSearch: ({ conversationList }) => conversationList.search.currentSearch }),
        ...mapGetters("mail", { CURRENT_MAILBOX }),
        selectedOption() {
            if (!this.currentSearch.folder) {
                return OPTIONS.ALL;
            } else if (this.currentSearch.deep) {
                return OPTIONS.CURRENT_FOLDER_AND_DESCENDANTS;
            } else {
                return OPTIONS.CURRENT_FOLDER;
            }
        }
    },
    watch: {
        folder: {
            handler() {
                this.SET_CURRENT_SEARCH_FOLDER(this.folder.imapName === DEFAULT_FOLDERS.INBOX ? null : this.folder);
                this.SET_CURRENT_SEARCH_DEEP(true);
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", { SET_CURRENT_SEARCH_FOLDER, SET_CURRENT_SEARCH_DEEP }),
        update(selection) {
            this.SET_CURRENT_SEARCH_FOLDER(selection === OPTIONS.ALL ? null : this.folder);
            this.SET_CURRENT_SEARCH_DEEP(selection === OPTIONS.CURRENT_FOLDER_AND_DESCENDANTS);
            this.$emit("update");
        },
        getIcon(option) {
            return option === OPTIONS.ALL ? "folders" : folderIcon(this.folder.imapName, this.CURRENT_MAILBOX?.type);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";
@import "@bluemind/ui-components/src/css/_type.scss";

.mail-search-box-context {
    .folder-tree-header,
    .dropdown-item {
        padding: $sp-4;
        .dropdown-item-content {
            margin-left: 0;
            display: flex;
            align-items: center;
        }
    }

    .bm-label-icon {
        gap: base-px-to-rem(4);
        display: flex;
        &.selected > div {
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
        }
        @extend %regular;
    }
}
</style>
