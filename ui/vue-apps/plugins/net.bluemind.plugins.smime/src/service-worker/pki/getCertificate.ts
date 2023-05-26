import { pki } from "node-forge";
import { AddressBooksClient, AddressBookClient, VCard, VCardInfo } from "@bluemind/addressbook.api";
import { searchVCardsHelper } from "@bluemind/contact";
import { ItemValue, ItemContainerValue } from "@bluemind/core.container.api";
import session from "@bluemind/session";
import checkCertificate from "./checkCertificate";
import { CertificateRecipientNotFoundError } from "../../lib/exceptions";

class CertificateFinderIterator {
    #addressbooks: [string, string[]][];
    #sid: string;

    #currentVCards: ItemValue<VCard>[] = [];
    #currentCertificates: VCard.BasicAttribute[] = [];

    #iAddressbook = -1;
    #iVCard = -1;
    #iCertificate = -1;

    constructor(addressbooks: [string, string[]][], sid: string) {
        this.#addressbooks = addressbooks;
        this.#sid = sid;
    }

    async getNext(): Promise<string | null> {
        const next = this.#nextVCardPem();
        if (next) return next;

        let vcard;
        do {
            vcard = this.#nextVCard();
        } while (vcard !== null && !this.#vCardHasCert(vcard.value));
        if (vcard) {
            this.#iCertificate = 0;
            this.#currentCertificates = vcard.value.security!.keys;
            return this.#currentCertificates[0].value;
        }

        let nextAddressbookVCards;
        do {
            nextAddressbookVCards = await this.#nextAddressbookVCards();
        } while (nextAddressbookVCards !== null && !this.#anyCertInVCards(nextAddressbookVCards));
        if (nextAddressbookVCards) {
            this.#currentVCards = nextAddressbookVCards;
            this.#iVCard = this.#findFirstVCardsWithCert(nextAddressbookVCards);
            this.#iCertificate = 0;
            this.#currentCertificates = this.#currentVCards[this.#iCertificate].value.security!.keys;
            return this.#currentCertificates[this.#iCertificate].value;
        }
        return null;
    }

    #findFirstVCardsWithCert(vCards: ItemValue<VCard>[]): number {
        return vCards.findIndex(vcard => !!vcard.value.security && vcard.value.security.keys.length > 0);
    }

    #nextAddressbookVCards(): Promise<ItemValue<VCard>[]> | null {
        if (this.#iAddressbook < this.#addressbooks.length - 1) {
            this.#iAddressbook++;
            const [addressbookUid, vcardUids] = this.#addressbooks[this.#iAddressbook];
            return new AddressBookClient(this.#sid, addressbookUid).multipleGet(vcardUids);
        }
        return null;
    }

    #nextVCard() {
        if (this.#iVCard < this.#currentVCards.length - 1) {
            this.#iVCard++;
            return this.#currentVCards[this.#iVCard];
        }
        return null;
    }

    #nextVCardPem() {
        if (this.#iCertificate < this.#currentCertificates.length - 1) {
            this.#iCertificate++;
            return this.#currentCertificates[this.#iCertificate].value;
        }
        return null;
    }

    #anyCertInVCards(vcards: ItemValue<VCard>[]): boolean {
        return vcards.some(({ value }) => this.#vCardHasCert(value));
    }

    #vCardHasCert(vcard: VCard): boolean {
        return !!vcard.security && vcard.security.keys.length > 0;
    }
}

export default async function getCertificate(email: string, smimeUsage: string): Promise<pki.Certificate> {
    const sid = await session.sid;

    const contactsInfo = await new AddressBooksClient(sid).search(searchVCardsHelper(email));
    const withSecurityKey = contactsInfo.values!.filter(info => info.value.hasSecurityKey);
    if (withSecurityKey.length === 0) {
        throw new CertificateRecipientNotFoundError(email);
    }

    const byContainers = groupByContainer(withSecurityKey);
    const iterator = new CertificateFinderIterator(Object.entries(byContainers), sid);
    let lastError, pem;
    while ((pem = await iterator.getNext()) !== null) {
        try {
            const certificate = await checkCertificate(pem, { expectedAddress: email, smimeUsage });
            return certificate;
        } catch (e) {
            lastError = e;
        }
    }
    throw lastError || new CertificateRecipientNotFoundError(email);
}

function groupByContainer(contactsInfo: ItemContainerValue<VCardInfo>[]) {
    const byContainers: { [container: string]: string[] } = contactsInfo.reduce(
        (byContainer: { [containerUid: string]: string[] }, contactInfo: ItemContainerValue<VCardInfo>) => {
            if (!byContainer[contactInfo.containerUid]) {
                byContainer[contactInfo.containerUid] = [];
            }
            byContainer[contactInfo.containerUid].push(contactInfo.uid!);
            return byContainer;
        },
        {}
    );
    return byContainers;
}
