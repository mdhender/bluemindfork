import { searchVCardsHelper } from "@bluemind/contact";
import { inject } from "@bluemind/inject";

const searchByAddressCache = {};

async function search(address) {
    if (searchByAddressCache[address]) {
        return searchByAddressCache[address];
    }
    const result = await inject("AddressBooksPersistence").search(searchVCardsHelper(address, 1));
    if (result.total > 0) {
        searchByAddressCache[address] = result;
    }
    return result;
}

export default { search };
