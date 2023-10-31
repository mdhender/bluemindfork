<template>
    <div v-if="contact" class="contact-card d-flex flex-column p-4">
        <contact-card-header :contact="contact" />
        <contact-card-body :contact="contact">
            <template #email="slotProps">
                <slot name="email" :email="slotProps.email" />
            </template>
        </contact-card-body>
        <div class="ml-3 mt-5">
            <slot name="actions" :contact="contact" />
        </div>
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
        contact: { type: Object, default: undefined }
    }
};
</script>
