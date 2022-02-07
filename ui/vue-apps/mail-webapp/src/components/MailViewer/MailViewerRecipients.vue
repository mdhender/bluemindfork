<template>
    <div class="mail-viewer-recipients">
        <mail-viewer-recipient v-if="message.to.length > 0" :recipients="message.to">{{
            $t("mail.content.to")
        }}</mail-viewer-recipient>
        <mail-viewer-recipient v-if="message.cc.length > 0" class="pb-2" :recipients="message.cc">{{
            $t("mail.content.copy")
        }}</mail-viewer-recipient>
    </div>
</template>

<script>
import MailViewerRecipient from "./MailViewerRecipient";

export default {
    name: "MailViewerRecipients",
    components: { MailViewerRecipient },
    props: {
        message: {
            type: Object,
            required: true
        }
    }
};
</script>
