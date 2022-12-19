import { searchVCardsHelper } from "@bluemind/contact";
import { inject } from "@bluemind/inject";

const SEARCH_API_MAX_SIZE = 10000;
const searchCache = {};

async function search(pattern, limit = 0, noGroup = false) {
    limit = limit < 0 ? SEARCH_API_MAX_SIZE : limit;
    if (searchCache[pattern]) {
        return searchCache[pattern];
    }
    const result = await inject("AddressBooksPersistence").search(searchVCardsHelper(pattern, limit, noGroup));
    if (result.total > 0) {
        searchCache[pattern] = result;
    }
    return result;
}

export default { search };
