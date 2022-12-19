<template>
    <div class="contact-card-header d-flex align-items-center mb-5">
        <bm-avatar :alt="displayName" :urn="image" size="md" />
        <h3 class="my-0 ml-4">{{ displayName }}</h3>
    </div>
</template>

<script>
import { BmAvatar } from "@bluemind/ui-components";

export default {
    name: "ContactCardHeader",
    components: { BmAvatar },
    props: {
        contact: {
            type: Object,
            required: true
        }
    },
    computed: {
        displayName() {
            return this.contact?.value?.identification.formatedName.value;
        },
        image() {
            const binary = this.contact?.value?.identification.photo && this.contact.value.identification.photoBinary;
            return binary ? URL.createObjectURL(new Blob([binary])) : null;
        }
    }
};
</script>
