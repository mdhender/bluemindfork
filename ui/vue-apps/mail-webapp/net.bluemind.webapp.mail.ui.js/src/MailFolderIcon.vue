<template>
    <bm-icon
        v-if="noText"
        class="mail-folder-icon"
        fixed-width
        :icon="icon"
        :aria-label="shared && $t('common.mailshares')"
    />
    <bm-label-icon v-else class="mail-folder-icon" :icon="icon" :aria-label="shared && $t('common.mailshares')">
        <slot>{{ folder.name }}</slot>
    </bm-label-icon>
</template>

<script>
import { BmLabelIcon, BmIcon } from "@bluemind/styleguide";

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
            switch (this.folder.path) {
                case "INBOX":
                    return "inbox";
                case "Drafts":
                    return "pencil" + modifier;
                case "Trash":
                    return "trash" + modifier;
                case "Junk":
                    return "forbidden";
                case "Outbox":
                    return "clock";
                case "Sent":
                    return "paper-plane" + modifier;
            }
            return "folder" + modifier;
        }
    }
};
</script>
