import { searchVCardsHelper } from "@bluemind/contact";
import { inject } from "@bluemind/inject";
import { VCardQuery } from "@bluemind/addressbook.api";

const SEARCH_API_MAX_SIZE = 10000;
const searchCache = {};
const searchCacheKey = args => args.reduce((total, current) => `${total}-${current}`, "");

async function search(pattern, limit = 0, noGroup = false, from, fields, orderBy, containerUid) {
    limit = limit < 0 ? SEARCH_API_MAX_SIZE : limit;
    const key = searchCacheKey(Array.from(arguments));
    if (searchCache[key]) {
        return searchCache[key];
    }
    const service = containerUid ? inject("AddressBookPersistence", containerUid) : inject("AddressBooksPersistence");
    const result = await service.search(searchVCardsHelper(pattern, { size: limit, noGroup, from, fields, orderBy }));
    if (result.total > 0) {
        searchCache[key] = result;
    }
    return result;
}

async function byPage(page, perPage, addressBookId) {
    const pageStartIndex = page * perPage - perPage;
    const { total, values } = await search(
        "",
        perPage,
        false,
        pageStartIndex,
        [],
        VCardQuery.OrderBy.FormatedName,
        addressBookId
    );
    const service = addressBookId ? inject("AddressBookPersistence", addressBookId) : inject("AddressBooksPersistence");
    const resultsInPage = await service.multipleGet(values.map(({ uid }) => uid));
    return { resultsInPage, pageStartIndex, totalLength: total };
}

export default { search, byPage };
