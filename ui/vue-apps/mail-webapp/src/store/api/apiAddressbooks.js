import { inject } from "@bluemind/inject";

const searchByAddressCache = {};

async function search(address) {
    if (searchByAddressCache[address]) {
        return searchByAddressCache[address];
    }
    const result = await inject("AddressBooksPersistence").search({
        from: 0,
        size: 1,
        query: address,
        escapeQuery: false
    });
    searchByAddressCache[address] = result;
    return result;
}

export async function invalidateCacheEntry(address) {
    searchByAddressCache[address] = undefined;
}

export default { invalidateCacheEntry, search };
