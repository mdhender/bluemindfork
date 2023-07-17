<template>
    <div class="selected-contacts selected bg-surface">
        <contact-input
            :contacts.sync="selectedContacts"
            readonly
            :enable-card="false"
            variant="inline"
            class="ml-6 flex-fill"
            @expand="expandContact"
        >
            <span class="selected-label">{{ $t(`common.${contactsType}`) }}</span>
        </contact-input>
    </div>
</template>

<script>
import { ContactInput } from "@bluemind/business-components";
import { fetchContactMembers } from "@bluemind/contact";

export default {
    name: "SelectedContacts",
    components: { ContactInput },
    props: {
        contacts: { type: Array, default: undefined },
        contactsType: { type: String, required: true }
    },
    computed: {
        selectedContacts: {
            get() {
                return this.contacts;
            },
            set(value) {
                this.$emit("update:contacts", value);
            }
        }
    },
    methods: {
        async expandContact(index) {
            const contacts = [...this.selectedContacts];
            const contact = contacts[index];
            contact.members = await fetchContactMembers(contactContainerUid(contact), contact.uid);
            contacts.splice(index, 1, ...contact.members);
            this.selectedContacts = this.removeDuplicates(contacts);
        },
        removeDuplicates(contacts) {
            return contacts.reduce(
                (allContacts, current) => (isDuplicate(allContacts, current) ? allContacts : [...allContacts, current]),
                []
            );

            function isDuplicate(contacts, aContact) {
                return contacts.findIndex(c => c.address === aContact.address) !== -1;
            }
        }
    }
};

function contactContainerUid(contact) {
    return contact.urn?.split("@")[1];
}
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/type";
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "@bluemind/ui-components/src/css/utils/scroller";
@import "@bluemind/ui-components/src/css/utils/variables";

.selected-contacts {
    display: flex;
    align-items: flex-start;

    .contact-input {
        .selected-label {
            color: $neutral-fg-hi1;
            @include bold;
            margin-right: $sp-5;
            @include from-lg {
                margin-right: $sp-6;
            }
            height: 100%;
            vertical-align: top;
            padding-top: base-px-to-rem(12);
        }
        .contacts {
            @include scroller-y;
            min-height: base-px-to-rem(40);
            max-height: base-px-to-rem(124);
            padding: $sp-3 0;
        }
    }
}
</style>
