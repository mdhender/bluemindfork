<template>
    <bm-contact 
        :contact="{ email: address, displayName: dn }" 
        :closeable="false"
        variant="transparent" 
    />
</template>

<script>
import BmContact from "@bluemind/styleguide/components/BmContact";

export default {
    name: "MailMessageContentFrom",
    components: {
        BmContact
    },
    props: {
        dn: {
            type: String,
            required: true
        },
        address: {
            type: String,
            required: true
        }
    }
};
</script>
