<template>
    <div class="mail-viewer-from">
        <contact :contact="contact" avatar-size="md" show-address transparent bold-dn popover />
    </div>
</template>

<script>
import { Contact } from "@bluemind/business-components";

export default {
    name: "MailViewerFrom",
    components: { Contact },
    props: {
        contact: {
            type: Object,
            required: true
        }
    }
};
</script>
