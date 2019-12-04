<template>
    <bm-label-icon :icon="icon" :aria-label="shared && $t('common.mailshares')">
        <slot>{{ folder.name }}</slot>
    </bm-label-icon>
</template>

<script>
import { BmLabelIcon } from "@bluemind/styleguide";

export default {
    name: "MailFolderIcon",
    components: {
        BmLabelIcon
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
        }
    },
    computed: {
        icon() {
            const modifier = this.shared ? "-shared" : "";
            switch (this.folder.fullName) {
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
