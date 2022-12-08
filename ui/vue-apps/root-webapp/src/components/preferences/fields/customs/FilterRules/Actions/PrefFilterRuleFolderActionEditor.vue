<template>
    <bm-form-select
        v-if="userFolders.length > 0"
        v-model="folderValue"
        :options="userFolders"
        :auto-min-width="false"
        class="pref-filter-rule-folder-action-editor"
        scrollbar
    >
        <template v-slot:selected="slotProps">
            <div v-if="slotProps.selected" class="folder-path font-weight-normal" :title="slotProps.selected.value">
                <bm-icon
                    class="mr-4"
                    :icon="icon(slotProps.selected.value.folder)"
                    :tooltip="slotProps.selected.value.folder"
                    :aria-label="slotProps.selected.value.folder"
                >
                </bm-icon>
                <span class="d-inline-block text-truncate">{{ start(slotProps.selected.text) }}</span>
                <span class="flex-fill text-nowrap text-truncate">{{ end(slotProps.selected.text) }}</span>
            </div>
            <div v-else class="folder-path font-weight-normal">
                {{ $t("preferences.mail.filters.modal.action.deliver.placeholder") }}
            </div>
        </template>
        <template v-slot:item="slotProps">
            <div class="folder-path" :title="slotProps.item.value">
                <bm-icon
                    class="mr-4"
                    :icon="icon(slotProps.item.value.folder)"
                    :tooltip="slotProps.item.value.folder"
                    :aria-label="slotProps.item.value.folder"
                >
                </bm-icon>
                <span class="d-inline-block text-truncate">{{ start(slotProps.item.text) }}</span>
                <span class="text-nowrap text-truncate"> {{ end(slotProps.item.text) }}</span>
            </div>
        </template>
    </bm-form-select>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmFormSelect, BmIcon } from "@bluemind/ui-components";

export default {
    name: "PrefFilterRuleFolderActionEditor",
    components: { BmFormSelect, BmIcon },
    props: {
        action: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            userFolders: []
        };
    },
    computed: {
        folderValue: {
            get() {
                return { subtree: this.action.subtree, id: this.action.id, folder: this.action.folder };
            },
            set(value) {
                this.$set(this.action, "subtree", value?.subtree);
                this.$set(this.action, "id", value?.id);
                this.$set(this.action, "folder", value?.folder);
            }
        }
    },
    async created() {
        const userId = inject("UserSession").userId;
        const mailboxUid = "user." + userId;
        const raw = await inject("MailboxFoldersPersistence", mailboxUid).all();
        this.userFolders = sort(raw);
    },
    methods: {
        icon(folderPath) {
            switch (folderPath) {
                case DEFAULT_FOLDER_NAMES.INBOX:
                    return "inbox";
                case DEFAULT_FOLDER_NAMES.DRAFTS:
                    return "pencil";
                case DEFAULT_FOLDER_NAMES.TRASH:
                    return "trash";
                case DEFAULT_FOLDER_NAMES.JUNK:
                    return "forbidden";
                case DEFAULT_FOLDER_NAMES.OUTBOX:
                    return "clock";
                case DEFAULT_FOLDER_NAMES.SENT:
                    return "paper-plane";
                case DEFAULT_FOLDER_NAMES.TEMPLATES:
                    return "documents";
                default:
                    return "folder";
            }
        },
        start(path) {
            return path.substring(0, path.lastIndexOf("/"));
        },
        end(path) {
            return path.substring(path.lastIndexOf("/"));
        }
    }
};

const DEFAULT_FOLDER_NAMES = {
    INBOX: "INBOX",
    SENT: "Sent",
    DRAFTS: "Drafts",
    TRASH: "Trash",
    JUNK: "Junk",
    TEMPLATES: "Templates",
    OUTBOX: "Outbox"
};

function sort(raw) {
    const result = [];

    result.push(userFolder(DEFAULT_FOLDER_NAMES.INBOX));
    result.push(userFolder(DEFAULT_FOLDER_NAMES.TRASH));
    result.push(userFolder(DEFAULT_FOLDER_NAMES.JUNK));

    let customFolders = raw
        .map(
            r =>
                !Object.values(DEFAULT_FOLDER_NAMES).includes(r.value.fullName) &&
                userFolder(r.value.fullName, r.internalId)
        )
        .filter(Boolean);
    customFolders = customFolders.sort((a, b) => a.text.localeCompare(b.text));
    result.push.apply(result, customFolders);

    result.push(userFolder(DEFAULT_FOLDER_NAMES.TEMPLATES));
    result.push(userFolder(DEFAULT_FOLDER_NAMES.DRAFTS));
    result.push(userFolder(DEFAULT_FOLDER_NAMES.SENT));
    result.push(userFolder(DEFAULT_FOLDER_NAMES.OUTBOX));

    return result;
}

function userFolder(folder, id) {
    const i18n = inject("i18n");
    const text = Object.values(DEFAULT_FOLDER_NAMES).includes(folder)
        ? i18n.t(`common.folder.${folder.toLowerCase()}`)
        : folder;
    return { text, value: { subtree: "user", id: id ? id : null, folder } };
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/mixins";

.pref-filter-rule-folder-action-editor {
    min-width: 0 !important;
    .scrollbar {
        overflow-x: hidden;
        width: 100%;
    }
    .dropdown-item-content {
        max-width: 100%;
    }
    .dropdown-toggle {
        min-width: 0;
    }

    .folder-path {
        display: flex;
        align-items: center;
        min-width: 0;
        flex: 1 1 auto;
        *:nth-child(2) {
            @include text-overflow;
        }
    }
}
</style>
