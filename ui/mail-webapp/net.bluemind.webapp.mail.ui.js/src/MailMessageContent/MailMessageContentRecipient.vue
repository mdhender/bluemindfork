<template>
    <div class="d-flex flex-wrap mb-1">
        <span class="pr-1"><slot /></span> 
        <div v-for="(recipient, index) in recipients" :key="recipient" class="d-flex">
            <bm-contact 
                :closeable="false" 
                :email="recipient" 
                variant="simple" 
            />
            <span v-if="index != (recipients.length - 1)" class="pr-1">,</span>
        </div>
    </div>
</template>

<script>
import BmContact from "@bluemind/styleguide/components/BmContact";

export default {
    name: "MailMessageContentRecipient",
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