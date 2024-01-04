import map from "lodash.map";
import flatmap from "lodash.flatmap";
import chunk from "lodash.chunk";
import { inject } from "@bluemind/inject";
import { messageUtils } from "@bluemind/mail";
import { extractFolderUid } from "@bluemind/mbox";
import { ItemFlag } from "@bluemind/core.container.api";
import { FolderAdaptor } from "../folders/helpers/FolderAdaptor";
const { MessageAdaptor, getLeafParts } = messageUtils;
import { ConversationListFilter, SortOrder } from "../conversationList";
import SearchHelper from "../../components/MailSearch/SearchHelper";

export const MAX_CHUNK_SIZE = 500;

export default {
    multipleDeleteById(messages) {
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, ({ itemsId }, folder) => api(folder).multipleDeleteById(itemsId));
        return Promise.all(requests);
    },
    deleteFlag(messages, mailboxItemFlag) {
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, ({ itemsId }, folder) => api(folder).deleteFlag({ itemsId, mailboxItemFlag }));
        return Promise.all(requests);
    },
    addFlag(messages, mailboxItemFlag) {
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, ({ itemsId }, folder) => api(folder).addFlag({ itemsId, mailboxItemFlag }));
        return Promise.all(requests);
    },
    async multipleGetById(messages) {
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, async ({ itemsId, folderRef }, folderUid) => {
            const items = flatmap(
                await Promise.all(
                    chunk(itemsId, MAX_CHUNK_SIZE).map(chunkedIds => api(folderUid).multipleGetById(chunkedIds))
                )
            );
            return items.map(item => MessageAdaptor.fromMailboxItem(item, folderRef));
        });
        return flatmap(await Promise.all(requests));
    },
    async getForUpdate(message) {
        const remoteMessage = await api(message.folderRef.uid).getForUpdate(message.remoteRef.internalId);
        return MessageAdaptor.fromMailboxItem(remoteMessage, message.folderRef);
    },
    move(messages, destination) {
        const destinationUid = destination.remoteRef.uid;
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, ({ itemsId }, sourceUid) => importApi(sourceUid, destinationUid).move(itemsId));
        return Promise.all(requests);
    },
    sortedIds(filter, sort, folder) {
        return api(folder.remoteRef.uid).sortedIds(toSortDescriptor(filter, sort));
    },

    async search({ pattern, folder, deep }, filter, sort, currentFolder) {
        const payload = {
            query: searchQuery(pattern, filter, folder && folder.uid, deep),
            sort: toSearchSort(sort)
        };
        const { results, hasMoreResults } = await folderApi(currentFolder.mailboxRef.uid).searchItems(payload);
        if (!results) {
            return {};
        }
        const resultKeys = new Set();
        const filteredResults = [];
        results.forEach(({ itemId, containerUid }) => {
            const resultKey = `${itemId}@${containerUid}`;
            if (!resultKeys.has(resultKey)) {
                const folderRef = FolderAdaptor.toRef(extractFolderUid(containerUid));
                filteredResults.push({ id: itemId, folderRef });
                resultKeys.add(resultKey);
            }
        });
        return { results: filteredResults || [], hasMoreResults };
    },
    fetchComplete(message) {
        return api(message.folderRef.uid).fetchComplete(message.remoteRef.imapUid);
    },
    removeParts(message) {
        getLeafParts(message.structure).forEach(
            part => part.address && api(message.folderRef.uid).removePart(part.address)
        );
    },
    multipleUnexpungeById(messages) {
        const byFolder = groupByFolder(messages);
        const requests = map(byFolder, ({ itemsId }, folder) => {
            return api(folder).multipleUnexpungeById(itemsId);
        });
        return Promise.all(requests);
    }
};

function api(folderUid) {
    return inject("MailboxItemsPersistence", folderUid);
}

function folderApi(mailboxUid) {
    return inject("MailboxFoldersPersistence", mailboxUid);
}

function importApi(sourceUid, destinationUid) {
    return inject("ItemsTransferPersistence", sourceUid, destinationUid);
}

function groupByFolder(messages) {
    return messages.reduce((byFolder, message) => {
        if (!byFolder[message.folderRef.uid]) {
            byFolder[message.folderRef.uid] = { itemsId: [], folderRef: message.folderRef };
        }
        byFolder[message.folderRef.uid].itemsId.push(message.remoteRef.internalId);
        return byFolder;
    }, {});
}

function searchQuery(pattern, filter, folderUid, deep) {
    const { query, recordQuery } = splitFields(pattern);
    const completeRecordQuery = [...recordQuery, filterQuery(filter)].filter(Boolean).join(" AND ");
    const completeQuery = query.join(" ");
    return {
        query: completeQuery,
        recordQuery: completeRecordQuery,
        maxResults: MAX_CHUNK_SIZE,
        scope: searchScope(folderUid, deep)
    };
}

function splitFields(queryPattern) {
    const query = [];
    const recordQuery = [];
    try {
        const byFields = SearchHelper.parseSearchPattern(queryPattern);
        const queryFieldsValues = Object.values(SearchHelper.QUERY_FIELDS);
        const recordQueryFieldsValues = Object.values(SearchHelper.RECORD_QUERY_FIELDS);

        for (const [field, val] of Object.entries(byFields)) {
            const value = field === SearchHelper.QUERY_FIELDS.CONTENT ? escape(val) : `${field}:${val}`;
            if (queryFieldsValues.includes(field)) {
                query.push(value);
            } else if (recordQueryFieldsValues.includes(field)) {
                recordQuery.push(value);
            }
        }
        return {
            recordQuery,
            query
        };
    } catch {
        return {
            recordQuery: [],
            query: [escape(queryPattern)]
        };
    }
}
function filterQuery(filter) {
    switch (filter) {
        case ConversationListFilter.UNREAD:
        case ConversationListFilter.FLAGGED:
            return "is:" + filter;
        default:
            return "";
    }
}

function searchScope(folderUid, deep = false) {
    return { folderScope: { folderUid }, isDeepTraversal: deep };
}

function toSearchSort(sort) {
    const searchSort = { criteria: [{ field: null, order: sort.order === SortOrder.ASC ? "Asc" : "Desc" }] };
    switch (sort.field) {
        case "date":
        case "size":
            searchSort.criteria[0].field = sort.field;
            break;
        case "subject":
            searchSort.criteria[0].field = "subject_kw";
            break;
        case "sender":
            searchSort.criteria[0].field = "headers.from";
            break;
    }
    return searchSort;
}

function toSortDescriptor(filter, sort) {
    const sortDescriptor = {
        fields: [
            {
                column: sort.field === "date" ? "internal_date" : sort.field,
                dir: sort.order === SortOrder.ASC ? "Asc" : "Desc"
            }
        ],
        filter: { must: [], mustNot: [] }
    };
    switch (filter) {
        case ConversationListFilter.ALL:
            sortDescriptor.filter.mustNot.push(ItemFlag.Deleted);
            break;
        case ConversationListFilter.UNREAD:
            sortDescriptor.filter.mustNot.push(ItemFlag.Seen);
            sortDescriptor.filter.mustNot.push(ItemFlag.Deleted);
            break;
        case ConversationListFilter.FLAGGED:
            sortDescriptor.filter.must.push(ItemFlag.Important);
            sortDescriptor.filter.mustNot.push(ItemFlag.Deleted);
            break;
        case ConversationListFilter.DELETED:
            sortDescriptor.filter.must.push(ItemFlag.Deleted);
            break;
    }
    return sortDescriptor;
}

function escape(term) {
    const charsToEscape = ["\\", "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", '"', "~", "*", "?", ":"];
    for (let i = 0; i < charsToEscape.length; i++) {
        term = term.split(charsToEscape[i]).join("\\" + charsToEscape[i]);
    }
    return term;
}
