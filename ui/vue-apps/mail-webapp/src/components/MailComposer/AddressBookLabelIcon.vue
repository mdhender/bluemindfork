<template>
    <bm-label-icon :icon="icon">{{ name }}</bm-label-icon>
</template>

<script>
import { isPersonalAddressBook, isDirectoryAddressBook, isCollectAddressBook } from "@bluemind/contact";
import { BmLabelIcon } from "@bluemind/ui-components";

export default {
    name: "AddressBookLabelIcon",
    components: { BmLabelIcon },
    props: {
        userId: { type: String, required: true },
        addressBook: { type: Object, required: true }
    },
    computed: {
        icon() {
            return (
                {
                    DIRECTORY: "buildings",
                    PERSONNAL: "user",
                    COLLECTED: "envelope-user",
                    OTHER: "user"
                }[this.type] + this.sharedSuffix
            );
        },
        type() {
            if (isDirectoryAddressBook(this.addressBook.uid, this.addressBook.domainUid)) {
                return "DIRECTORY";
            }
            if (isPersonalAddressBook(this.addressBook.uid, this.addressBook.owner)) {
                return "PERSONNAL";
            }
            if (isCollectAddressBook(this.addressBook.uid, this.addressBook.owner)) {
                return "COLLECTED";
            }
            return "OTHER";
        },
        sharedSuffix() {
            return this.isShared ? "-shared" : "";
        },
        isShared() {
            return this.type !== "DIRECTORY" && this.userId !== this.addressBook.owner;
        },
        name() {
            if (this.isShared) {
                return this.$t(`recipientPicker.addressbook.shared.${this.namePrefix}`, {
                    user: this.addressBook.ownerDisplayname,
                    addressBookName: this.addressBook.name
                });
            }
            return this.addressBook.name;
        },
        namePrefix() {
            return {
                PERSONNAL: "personnal",
                COLLECTED: "collected",
                OTHER: "others"
            }[this.type];
        }
    }
};
</script>
