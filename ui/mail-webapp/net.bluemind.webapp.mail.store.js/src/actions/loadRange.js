const loading = [];
import debounce from "lodash.debounce";

let debouncedLoadMessage = debounce(loadMessages, 250);

export function loadRange({ dispatch, getters, state }, { start, end }) {
    const messages = getters["messages/messages"];
    const sorted = state.messages.sortedIds;
    const folderUid = state.currentFolderUid;
    const essentials = missingMessages(messages, sorted, start, end);
    const full = essentials
        .concat(missingMessages(messages, sorted, start - 100, start))
        .concat(missingMessages(messages, sorted, end, end + 100));
    if (essentials.length > 0) {
        debouncedLoadMessage.cancel();
        return loadMessages(dispatch, folderUid, full);
    } else {
        return debouncedLoadMessage(dispatch, folderUid, full);
    }
}

function loadMessages(dispatch, folderUid, ids) {
    loading.push(...ids);
    return dispatch("messages/multipleById", { folderUid, ids }).then(() => {
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
