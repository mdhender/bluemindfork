const loading = [];
import debounce from "lodash.debounce";

let debouncedLoadMessage = debounce(loadMessages, 250);

export function loadRange({ dispatch, getters, state }, { start, end }) {
    const messages = getters["messages/messages"];
    const sorted = state.messages.itemKeys;
    const essentials = missingMessages(messages, sorted, start, end);
    const full = essentials
        .concat(missingMessages(messages, sorted, start - 100, start))
        .concat(missingMessages(messages, sorted, end, end + 100));
    if (essentials.length > 0) {
        debouncedLoadMessage.cancel();
        return loadMessages(dispatch, full);
    } else {
        return debouncedLoadMessage(dispatch, full);
    }
}

function loadMessages(dispatch, keys) {
    if (keys.length > 0) {
        loading.push(...keys);
        return dispatch("messages/multipleByKey", keys).then(() => {
            keys.forEach(key => loading.splice(loading.indexOf(key), 1));
        });
    }
}

function missingMessages(messages, keys, start, end) {
    const missings = [];
    start = Math.max(0, start);
    end = Math.min(end, keys.length);
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
