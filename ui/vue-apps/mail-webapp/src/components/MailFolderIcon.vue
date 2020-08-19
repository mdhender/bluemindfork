<template>
    <bm-icon
        v-if="noText"
        class="mail-folder-icon"
        fixed-width
        :icon="icon"
        :aria-label="shared && $t('common.mailshares')"
    />
    <bm-label-icon
        v-else
        class="mail-folder-icon"
        :icon="icon"
        :aria-label="shared && $t('common.mailshares')"
        :tooltip="folder.path"
        >{{ folder.name }}</bm-label-icon
    >
</template>

<script>
import { BmLabelIcon, BmIcon } from "@bluemind/styleguide";
import { DEFAULT_FOLDER_NAMES } from "../store/helpers/DefaultFolders";

export default {
    name: "MailFolderIcon",
    components: {
        BmLabelIcon,
        BmIcon
    },
    props: {
        folder: {
            type: Object,
            required: true
        },
        shared: {
            type: Boolean,
            required: false,
            default: false
        },
        noText: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    computed: {
        icon() {
            const modifier = this.shared ? "-shared" : "";
            switch (this.folder.imapName) {
                case DEFAULT_FOLDER_NAMES.INBOX:
                    return "inbox";
                case DEFAULT_FOLDER_NAMES.DRAFTS:
                    return "pencil" + modifier;
                case DEFAULT_FOLDER_NAMES.TRASH:
                    return "trash" + modifier;
                case DEFAULT_FOLDER_NAMES.JUNK:
                    return "forbidden";
                case DEFAULT_FOLDER_NAMES.OUTBOX:
                    return "clock";
                case DEFAULT_FOLDER_NAMES.SENT:
                    return "paper-plane" + modifier;
                default:
                    return "folder" + modifier;
            }
        }
    }
};
</script>
