<template>
    <bm-label-icon :icon="icon">{{ folder.name }}</bm-label-icon>
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
        }
    },
    computed: {
        icon() {
            if (!this.folder.parentUid) {
                switch (this.folder.name) {
                    case "INBOX":
                        return "inbox";
                    case "Drafts": 
                        return "pencil";
                    case "Trash":
                        return "trash";
                    case "Junk":
                        return "forbidden";
                    case "Outbox":
                        return "clock";
                    case "Sent":
                        return "paper-plane";
                }
            }
            return "folder";
        }
    }
};
</script>