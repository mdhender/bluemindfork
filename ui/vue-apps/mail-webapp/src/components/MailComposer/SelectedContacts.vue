<template>
    <div class="selected-contacts selected bg-surface">
        <contact-input
            :contacts.sync="selectedContacts"
            readonly
            variant="inline"
            class="ml-6 flex-fill"
            @expand="expandContact"
        >
            <span class="selected-label">{{ $t("common.to") }}</span>
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
        contacts: { type: Array, default: undefined }
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
            this.selectedContacts = contacts;
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

    min-height: $avatar-height-sm + base-px-to-rem(20);
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
            max-height: base-px-to-rem(124);
            padding-top: base-px-to-rem(4);
        }
    }
}
</style>
