<template>
    <bm-form-select
        v-if="selectedOption"
        v-model="selectedOption"
        :auto-min-width="false"
        :options="Object.values(OPTIONS)"
        class="mail-search-form-context"
    >
        <template #header>
            <folder-tree-header v-if="CURRENT_MAILBOX" :mailbox="CURRENT_MAILBOX" />
        </template>
        <template #selected="{ selected }">
            <bm-label-icon :inline="false" class="selected label" :icon="getIcon(selected.value)">
                {{ $t(`mail.search_form.context.${selected.value}`, { folder: currentFolderName }) }}
            </bm-label-icon>
        </template>
        <template #item="{ item }">
            <bm-label-icon :inline="false" class="label" :icon="getIcon(item.value)">
                {{ $t(`mail.search_form.context.${item.value}`, { folder: currentFolderName }) }}
            </bm-label-icon>
        </template>
    </bm-form-select>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { BmFormSelect, BmLabelIcon } from "@bluemind/ui-components";
import { folderUtils } from "@bluemind/mail";
import { CURRENT_MAILBOX } from "~/getters";
import FolderTreeHeader from "../MailFolder/FolderTreeHeader";
const { DEFAULT_FOLDERS, folderIcon } = folderUtils;

const OPTIONS = {
    ALL: "all",
    CURRENT_FOLDER_AND_DESCENDANTS: "current_folder_and_descendants",
    CURRENT_FOLDER: "current_folder"
};

export default {
    name: "MailSearchFormContext",
    components: { BmFormSelect, BmLabelIcon, FolderTreeHeader },
    data() {
        return {
            OPTIONS,
            selectedOption: null
        };
    },
    computed: {
        ...mapState("mail", { currentSearch: ({ conversationList }) => conversationList.search }),
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapGetters("mail", { CURRENT_MAILBOX }),
        currentFolder() {
            return this.folders[this.activeFolder];
        },
        currentFolderName() {
            return this.currentFolder?.name;
        },
        context() {
            switch (this.selectedOption) {
                case OPTIONS.CURRENT_FOLDER_AND_DESCENDANTS:
                    return { folder: this.currentFolder, deep: true };
                case OPTIONS.CURRENT_FOLDER: {
                    return { folder: this.currentFolder, deep: false };
                }
                default: {
                    return {};
                }
            }
        }
    },
    watch: {
        currentFolder: {
            handler(folder) {
                this.selectedOption =
                    !folder || folder.imapName === DEFAULT_FOLDERS.INBOX
                        ? OPTIONS.ALL
                        : OPTIONS.CURRENT_FOLDER_AND_DESCENDANTS;
            },
            immediate: true
        },
        context: {
            handler() {
                this.$emit("changed", this.context);
            },
            immediate: true
        }
    },
    methods: {
        getIcon(option) {
            return option === OPTIONS.ALL || !this.currentFolder
                ? "folders"
                : folderIcon(this.currentFolder.imapName, this.CURRENT_MAILBOX.type);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.mail-search-form-context {
    .folder-tree-header,
    .dropdown-item {
        padding: $sp-4;
        .dropdown-item-content {
            margin-left: 0;
            display: flex;
            align-items: center;
        }
    }
    & > .dropdown-toggle {
        &,
        &:hover {
            border-color: transparent !important;
            border-right-color: $neutral-fg-lo3 !important;
        }
    }
    .label {
        gap: base-px-to-rem(4);
        display: flex;
        align-items: center;
        &.selected > div {
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
        }
        font-weight: $font-weight-normal;
    }
}
</style>
