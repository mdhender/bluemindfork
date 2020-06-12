<template>
    <bm-contact :contact="{ email: address, displayName: dn }" :closeable="false" variant="transparent" />
</template>

<script>
import { BmContact } from "@bluemind/styleguide";

export default {
    name: "MailViewerFrom",
    components: {
        BmContact
    },
    props: {
        dn: {
            type: String,
            required: false,
            default: ""
        },
        address: {
            type: String,
            required: true
        }
    }
};
</script>
