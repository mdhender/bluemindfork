<script>
import { searchVCardsByIdHelper, searchVCardsHelper } from "@bluemind/contact";
import { inject } from "@bluemind/inject";
import mergeContacts from "./mergeContacts";

export default {
    name: "ResolvedContact",
    props: {
        address: { type: String, default: undefined },
        contact: { type: Object, default: undefined },
        uid: { type: String, default: undefined },
        containerUid: { type: String, default: undefined }
    },
    data() {
        return { resolvedContact: null };
    },
    watch: {
        async address() {
            await this.resolveContact();
        },
        contact: {
            handler: async function () {
                await this.resolveContact();
            },
            deep: true
        },
        async uid() {
            await this.resolveContact();
        },
        async containerUid() {
            await this.resolveContact();
        }
    },
    async created() {
        await this.resolveContact();
    },
    methods: {
        async resolveContact() {
            if (this.contact) {
                this.resolvedContact = this.contact;
            } else if (this.uid) {
                this.resolvedContact = await this.uidToContact();
            } else if (this.address) {
                this.resolvedContact = await this.addressToContact();
            } else if (this.containerUid) {
                throw "Missing 'uid' property value.";
            }
        },
        async addressToContact() {
            const searchResults = await inject("AddressBooksPersistence").search(searchVCardsHelper(this.address));
            return searchResultsToContact(searchResults);
        },
        async uidToContact() {
            const searchResults = await inject("AddressBooksPersistence").search(
                searchVCardsByIdHelper(this.uid, this.containerUid)
            );
            return searchResultsToContact(searchResults);
        }
    },
    render() {
        return this.$scopedSlots.default({ resolvedContact: this.resolvedContact });
    }
};

async function searchResultsToContact(searchResults) {
    const promises = searchResults.values?.map(
        async v => await inject("AddressBookPersistence", v.containerUid).getComplete(v.uid)
    );
    const fullContacts = await Promise.all(promises);
    return mergeContacts(
        fullContacts,
        searchResults.values?.map(pc => pc.containerUid)
    );
}
</script>
