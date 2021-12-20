<template>
    <bm-avatar v-if="isUserMailbox" :alt="mailbox.name" />
    <bm-avatar v-else icon="folder-shared" :alt="$t('common.mailshares')" />
</template>

<script>
import { BmAvatar } from "@bluemind/styleguide";

import { MailboxType } from "~/model/mailbox";
export default {
    name: "MailMailboxIcon",
    components: {
        BmAvatar
    },
    props: {
        mailbox: {
            type: Object,
            required: true
        },
        noText: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    computed: {
        isUserMailbox() {
            return this.mailbox.type === MailboxType.USER;
        }
    }
};
</script>
