<template>
    <div class="contact-card-header d-flex align-items-center mb-6">
        <bm-avatar :alt="displayName" :url="contact.value.identification.photoBinary" size="lg" />
        <div class="title my-0 ml-5 mr-auto text-truncate">{{ displayName }}</div>
        <bm-icon
            v-if="addressBook"
            icon="user-check"
            class="text-neutral ml-4"
            :title="$t('contact.address_book', { name: addressBook.name })"
        />
    </div>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmAvatar, BmIcon } from "@bluemind/ui-components";
import { isDirectoryAddressBook } from "@bluemind/contact";

export default {
    name: "ContactCardHeader",
    components: { BmAvatar, BmIcon },
    props: {
        contact: {
            type: Object,
            required: true
        }
    },
    data() {
        return { addressBook: undefined };
    },
    computed: {
        displayName() {
            return this.contact?.value?.identification.formatedName.value;
        }
    },
    watch: {
        contact: {
            handler: async function (value) {
                if (value?.containerUid) {
                    const { userId, domain } = inject("UserSession");
                    const lightContainers = await inject("ContainersPersistence").getContainersLight([
                        value.containerUid
                    ]);
                    this.addressBook =
                        isDirectoryAddressBook(value.containerUid, domain) ||
                        (lightContainers?.length && lightContainers[0].owner === userId)
                            ? lightContainers[0]
                            : undefined;
                }
            },
            immediate: true
        }
    }
};
</script>
