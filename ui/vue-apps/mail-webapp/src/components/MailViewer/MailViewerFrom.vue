<template>
    <bm-contact :contact="contact" avatar-size="md" show-address transparent bold-dn />
</template>

<script>
import { BmContact } from "@bluemind/styleguide";

export default {
    name: "MailViewerFrom",
    components: { BmContact },
    props: {
        contact: {
            type: Object,
            required: true
        }
    }
};
</script>
