<template>
    <div class="mail-viewer-from">
        <mail-contact-card-slots
            :component="Contact"
            :contact="message.from"
            avatar-size="md"
            show-address
            transparent
            bold-dn
            enable-card
        />
        <bm-extension id="webapp.mail" path="viewer.sender.suffix" :message="message" />
    </div>
</template>

<script>
import { Contact } from "@bluemind/business-components";
import { BmExtension } from "@bluemind/extensions.vue";
import MailContactCardSlots from "../MailContactCardSlots";

export default {
    name: "MailViewerFrom",
    components: { MailContactCardSlots, BmExtension },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return { Contact };
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.mail-viewer-from {
    display: flex;
    align-items: center;
    .contact {
        margin-right: $sp-3;
    }
}
</style>
