<template>
    <bm-avatar size="sm" :alt="tooltip" :icon="icon" />
</template>

<script>
import { BmAvatar } from "@bluemind/ui-components";
import { mailboxUtils } from "@bluemind/mail";
const { MailboxType } = mailboxUtils;

export default {
    name: "MailMailboxIcon",
    components: { BmAvatar },
    props: {
        mailbox: {
            type: Object,
            required: true
        }
    },
    computed: {
        tooltip() {
            switch (this.mailbox.type) {
                case MailboxType.USER:
                    return this.mailbox.name;
                case MailboxType.MAILSHARE:
                    return this.$t("common.mailshares");
                case MailboxType.GROUP:
                    return this.$t("mail.folders.groups");
                default:
                    return "";
            }
        },
        icon() {
            switch (this.mailbox.type) {
                case MailboxType.MAILSHARE:
                    return "folder-network";
                case MailboxType.GROUP:
                    return "users2";
                default:
                    return undefined;
            }
        }
    }
};
</script>
