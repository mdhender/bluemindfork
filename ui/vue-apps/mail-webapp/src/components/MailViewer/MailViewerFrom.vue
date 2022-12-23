<template>
    <mail-contact-card-slots
        :component="Contact"
        class="mail-viewer-from"
        :contact="contact"
        avatar-size="md"
        show-address
        transparent
        bold-dn
        enable-card
    />
</template>

<script>
import { Contact } from "@bluemind/business-components";
import MailContactCardSlots from "../MailContactCardSlots";

export default {
    name: "MailViewerFrom",
    components: { MailContactCardSlots },
    props: {
        contact: {
            type: Object,
            required: true
        }
    },
    data() {
        return { Contact };
    }
};
</script>
