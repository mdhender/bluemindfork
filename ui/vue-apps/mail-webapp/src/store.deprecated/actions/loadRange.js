const loading = [];
const PAGE_SIZE = 100;

export async function loadRange({ dispatch, rootState }, range) {
    const messages = rootState.mail.messages;
    const sorted = rootState.mail.messageList.messageKeys;
    const keys = getKeysForRange(messages, sorted, range);
    await loadMessages(dispatch, keys, loading);
}

async function loadMessages(dispatch, keys, loading) {
    if (keys.length > 0) {
        loading.push(...keys);
        await dispatch("messages/multipleByKey", keys);
        keys.forEach(key => loading.splice(loading.indexOf(key), 1));
    }
}

function getKeysForRange(messages, keys, { start, end }) {
    const cacheSize = end - start;
    let range = enlargeRange({ start, end }, keys.length, cacheSize);
    const neededMissingKeys = missingMessages(messages, keys, range);
    if (neededMissingKeys.length > 0) {
        range = enlargeRange(range, keys.length, PAGE_SIZE);
        return missingMessages(messages, keys, range);
    }
    return neededMissingKeys;
}

function enlargeRange({ start, end }, maxLength, delta) {
    return sanitizeRange(start - delta, end + delta, maxLength);
}

function sanitizeRange(start, end, rangeLength) {
    return {
        start: Math.max(0, start),
        end: Math.min(end, rangeLength)
    };
}

function missingMessages(messages, keys, { start, end }) {
    const missings = [];

    for (let i = start; i < end; i++) {
        if (!messages[i]) {
            const id = keys[i];
            if (loading.indexOf(id) < 0) {
                missings.push(id);
            }
        }
    }
    return missings;
}
