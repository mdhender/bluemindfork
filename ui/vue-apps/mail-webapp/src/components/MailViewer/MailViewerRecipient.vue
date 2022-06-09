<template>
    <div class="mail-viewer-recipient d-flex mb-1">
        <span class="text-neutral"><slot />&nbsp;</span>
        <div v-for="(recipient, index) in recipients" :key="recipient.address">
            <bm-contact :contact="recipient" variant="simple" /><template v-if="index != recipients.length - 1"
                >,&nbsp;</template
            >
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
<style lang="scss">
.mail-viewer-recipient {
    flex-wrap: wrap;
}
</style>
