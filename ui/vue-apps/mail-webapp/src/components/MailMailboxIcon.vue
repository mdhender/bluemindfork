<template>
    <bm-avatar v-if="isUserMailbox" class="flex-shrink-0" :alt="mailbox.name" />
    <bm-avatar v-else class="flex-shrink-0" icon="folder-shared" :alt="$t('common.mailshares')" />
</template>

<script>
import { BmAvatar } from "@bluemind/styleguide";
import { mailbox } from "@bluemind/mail";
const { MailboxType } = mailbox;

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
