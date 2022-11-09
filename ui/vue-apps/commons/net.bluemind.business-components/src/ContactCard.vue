<template>
    <div v-if="contact" class="contact-card d-flex flex-column p-4">
        <contact-card-header :contact="contact" />
        <contact-card-body :contact="contact">
            <template #email="slotProps">
                <slot name="email" :email="slotProps.email" />
            </template>
        </contact-card-body>
        <contact-card-footer :contact="contact" />
    </div>
    <contact-card-skeleton v-else class="contact-card" />
</template>

<script>
import ContactCardBody from "./ContactCardBody";
import ContactCardFooter from "./ContactCardFooter";
import ContactCardHeader from "./ContactCardHeader";
import ContactCardSkeleton from "./ContactCardSkeleton";

export default {
    name: "ContactCard",
    components: { ContactCardBody, ContactCardFooter, ContactCardHeader, ContactCardSkeleton },
    props: {
        contact: { type: Object, default: undefined }
    }
};
</script>
