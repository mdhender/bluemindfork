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
import { isCollectAddressBook, isDomainAddressBook, isPersonalAddressBook } from "@bluemind/contact";

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
                const { userId, domain } = inject("UserSession");
                this.addressBook =
                    value &&
                    (isDomainAddressBook(value.containerUid, domain) ||
                        isPersonalAddressBook(value.containerUid, userId) ||
                        isCollectAddressBook(value.containerUid, userId))
                        ? await inject("AddressBooksMgmtPersistence").getComplete(value.containerUid)
                        : undefined;
            },
            immediate: true
        }
    }
};
</script>
