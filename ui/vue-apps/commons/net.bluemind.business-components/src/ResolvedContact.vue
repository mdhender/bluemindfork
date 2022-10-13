<script>
import { searchVCardsHelper } from "@bluemind/contact";
import { inject } from "@bluemind/inject";

export default {
    name: "ResolvedContact",
    props: {
        criteria: { type: Object, required: true }
    },
    data() {
        return { resolvedContact: null };
    },
    watch: {
        criteria: {
            handler: function () {
                this.resolveContact();
            },
            immediate: true,
            deep: true
        }
    },
    methods: {
        async resolveContact() {
            if (this.criteria.contact) {
                this.resolvedContact = this.criteria.contact;
            } else if (this.criteria.address) {
                this.resolvedContact = await this.addressToContact(this.criteria.address);
            }
        },
        async addressToContact() {
            const partialContacts = await inject("AddressBooksPersistence").search(
                searchVCardsHelper(this.criteria.address)
            );
            const promises = partialContacts.values?.map(v =>
                inject("AddressBookPersistence", v.containerUid).getComplete(v.uid)
            );
            const fullContacts = await Promise.all(promises);
            // TODO merge contacts
            const merged = fullContacts[0];
            console.log(">>>>>>>>>>>>>>>>>>merged ", merged);
            return merged;
        }
    },
    render() {
        return this.$scopedSlots.default({ resolvedContact: this.resolvedContact });
    }
};
</script>
