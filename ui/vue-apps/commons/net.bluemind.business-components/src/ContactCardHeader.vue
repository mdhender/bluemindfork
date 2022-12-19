<template>
    <div class="contact-card-header d-flex align-items-center mb-5">
        <bm-avatar :alt="displayName" :url="contact.value.identification.photoBinary" size="md" />
        <h3 class="my-0 ml-4 mr-auto">{{ displayName }}</h3>
        <bm-icon
            v-if="addressBook"
            icon="user-check"
            class="text-neutral"
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
