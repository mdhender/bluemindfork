<template>
    <bm-form-select
        v-if="userFolders.length > 0"
        v-model="action.value"
        :options="userFolders"
        class="pref-filter-rule-folder-action-editor"
        menu-class="col-5"
        :boundary="$el"
        no-flip
    >
        <template v-slot:selected="slotProps">
            <span class="selected-text float-left mr-2 font-weight-normal">
                <bm-label-icon
                    v-if="slotProps.selected"
                    :icon="icon(slotProps.selected.value)"
                    :tooltip="slotProps.selected.value"
                    :aria-label="slotProps.selected.value"
                >
                    {{ slotProps.selected.text }}
                </bm-label-icon>
                <template v-else>{{ $t("preferences.mail.filters.modal.action.deliver.placeholder") }}</template>
            </span>
        </template>
        <template v-slot:item="slotProps">
            <bm-label-icon
                :icon="icon(slotProps.item.value)"
                :tooltip="slotProps.item.value"
                :aria-label="slotProps.item.value"
            >
                {{ slotProps.item.text }}
            </bm-label-icon>
        </template>
    </bm-form-select>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmFormSelect, BmLabelIcon } from "@bluemind/styleguide";

export default {
    name: "PrefFilterRuleFolderActionEditor",
    components: { BmFormSelect, BmLabelIcon },
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

    result.push({ text: DEFAULT_FOLDER_NAMES.INBOX, value: DEFAULT_FOLDER_NAMES.INBOX });
    result.push({ text: DEFAULT_FOLDER_NAMES.TRASH, value: DEFAULT_FOLDER_NAMES.TRASH });
    result.push({ text: DEFAULT_FOLDER_NAMES.JUNK, value: DEFAULT_FOLDER_NAMES.JUNK });

    let customFolders = raw
        .map(
            r =>
                !Object.values(DEFAULT_FOLDER_NAMES).includes(r.value.fullName) && {
                    text: r.displayName,
                    value: r.value.fullName
                }
        )
        .filter(Boolean);
    customFolders = customFolders.sort((a, b) => a.text.localeCompare(b.text));
    result.push.apply(result, customFolders);

    result.push({ text: DEFAULT_FOLDER_NAMES.TEMPLATES, value: DEFAULT_FOLDER_NAMES.TEMPLATES });
    result.push({ text: DEFAULT_FOLDER_NAMES.DRAFTS, value: DEFAULT_FOLDER_NAMES.DRAFTS });
    result.push({ text: DEFAULT_FOLDER_NAMES.SENT, value: DEFAULT_FOLDER_NAMES.SENT });
    result.push({ text: DEFAULT_FOLDER_NAMES.OUTBOX, value: DEFAULT_FOLDER_NAMES.OUTBOX });

    return result;
}
</script>

<style lang="scss">
.pref-filter-rule-folder-action-editor {
    .dropdown-menu {
        min-width: unset;
    }
}
</style>
