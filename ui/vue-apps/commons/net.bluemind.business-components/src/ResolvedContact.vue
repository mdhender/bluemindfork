<script>
import { recipientStringToVCardItem, searchVCardsByIdHelper, searchVCardsHelper } from "@bluemind/contact";
import { inject } from "@bluemind/inject";
import { EmailExtractor } from "@bluemind/email";
import mergeContacts from "./mergeContacts";

export default {
    name: "ResolvedContact",
    props: {
        recipient: { type: String, default: undefined },
        contact: { type: Object, default: undefined },
        uid: { type: String, default: undefined },
        containerUid: { type: String, default: undefined }
    },
    data() {
        return { resolvedContact: null };
    },
    watch: {
        async recipient() {
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
            } else if (this.recipient) {
                this.resolvedContact = await this.recipientToContact();
            } else if (this.containerUid) {
                throw "Missing 'uid' property value.";
            }
        },
        async recipientToContact() {
            const searchToken = EmailExtractor.extractEmail(this.recipient) || EmailExtractor.extractDN(this.recipient);
            const searchResults = await inject("AddressBooksPersistence").search(searchVCardsHelper(searchToken, -1));
            return searchResults.values?.length
                ? searchResultsToContact(searchResults)
                : recipientStringToVCardItem(this.recipient);
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
    const groupedByContainer = searchResults.values?.reduce((res, current) => {
        if (!res[current.containerUid]) {
            res[current.containerUid] = [];
        }
        res[current.containerUid].push(current.uid);
        return res;
    }, {});

    const promises = Object.keys(groupedByContainer).map(async containerUid =>
        (
            await inject("AddressBookPersistence", containerUid).multipleGet(groupedByContainer[containerUid])
        ).map(res => ({ ...res, containerUid }))
    );
    const fullContacts = (await Promise.all(promises)).flatMap(r => r);

    return mergeContacts(fullContacts);
}
</script>
