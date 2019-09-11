const loading = [];
import debounce from "lodash.debounce";

let debouncedLoadMessage = debounce(loadMessages, 250);

export function loadRange({ dispatch, rootGetters, rootState }, { start, end }) {
    const messages = rootGetters["backend.mail/items/messages"];
    const sorted = rootState["backend.mail/items"].sortedIds;
    const folder = rootGetters["backend.mail/folders/currentFolder"];
    const essentials = missingMessages(messages, sorted, start, end);
    const full = essentials
        .concat(missingMessages(messages, sorted, start - 100, start))
        .concat(missingMessages(messages, sorted, end, end + 100));
    if (essentials.length > 0) {
        debouncedLoadMessage.cancel();
        return loadMessages(dispatch, folder, full);
    } else {
        return debouncedLoadMessage(dispatch, folder, full);
    }
}

function loadMessages(dispatch, folder, ids) {
    loading.push(...ids);
    return dispatch("backend.mail/items/multipleById", { folder, ids }, { root: true }).then(() => {
        ids.forEach(id => loading.splice(loading.indexOf(id), 1));
    });
}

function missingMessages(messages, ids, start, end) {
    const missings = [];
    start = Math.max(0, start);
    end = Math.min(end, ids.length);
    for (let i = start; i < end; i++) {
        if (!messages[i]) {
            const id = ids[i];
            if (loading.indexOf(id) < 0) {
                missings.push(id);
            }
        }
    }
    return missings;
}
