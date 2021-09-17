<template>
    <bm-contact :contact="contact" :closeable="false" variant="transparent" />
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
