<template>
    <mail-contact
        class="mail-viewer-from"
        :contact="contact"
        avatar-size="md"
        show-address
        transparent
        bold-dn
        popover
    />
</template>

<script>
import MailContact from "./MailContact";

export default {
    name: "MailViewerFrom",
    components: { MailContact },
    props: {
        contact: {
            type: Object,
            required: true
        }
    }
};
</script>
