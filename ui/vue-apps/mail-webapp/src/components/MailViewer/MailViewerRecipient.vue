<template>
    <div class="d-flex flex-wrap mb-1">
        <span class="text-secondary"><slot />&nbsp;</span>
        <div v-for="(recipient, index) in recipients" :key="recipient.address" class="d-flex">
            <bm-contact :contact="recipient" variant="simple" />
            <span v-if="index != recipients.length - 1" class="pr-1">,</span>
        </div>
    </div>
</template>

<script>
import { BmContact } from "@bluemind/styleguide";

export default {
    name: "MailViewerRecipient",
    components: {
        BmContact
    },
    props: {
        recipients: {
            type: Array,
            required: true
        }
    }
};
</script>
