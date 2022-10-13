<template>
    <div v-if="contact" class="contact-card d-flex flex-column p-4">
        <contact-card-header :contact="contact" />
        <contact-card-body :contact="contact" />
    </div>
    <contact-card-skeleton v-else class="contact-card" />
</template>

<script>
import ContactCardBody from "./ContactCardBody";
import ContactCardHeader from "./ContactCardHeader";
import ContactCardSkeleton from "./ContactCardSkeleton";

export default {
    name: "ContactCard",
    components: { ContactCardBody, ContactCardHeader, ContactCardSkeleton },
    props: {
        contact: {
            type: Object,
            default: null
        }
    }
};
</script>
