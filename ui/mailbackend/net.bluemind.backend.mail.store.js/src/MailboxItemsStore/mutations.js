import Vue from "vue";

export function setItems(state, items) {
    state.items = items;
}

export function setCount(state, count) {
    state.count = count;
}

export function setCurrent(state, uid) {
    state.current = uid;
}

export function updateSeen(state, { uid, isSeen }) {
    let mailboxItem = state.items.find(item => item.uid === uid);
    let currentSeenState = mailboxItem.value.systemFlags.includes("seen");
    if (currentSeenState !== isSeen) {
        if (isSeen) {
            mailboxItem.value.systemFlags.push("seen");
        } else {
            let seenIndex = mailboxItem.value.systemFlags.indexOf("seen");
            mailboxItem.value.systemFlags.splice(seenIndex, 1);
        }
    }
}

export function setPartsToDisplay(state, partsToDisplay) {
    state.partsToDisplay = partsToDisplay;
}

export function setAttachments(state, attachments) {
    state.attachments = attachments.slice();
}

export function setSearchPattern(state, pattern) {
    Vue.set(state.search, 'pattern', pattern);
}

export function setSearchLoading(state, isLoading) {
    Vue.set(state.search, 'loading', isLoading);
}

export function setSearchError(state, hasError) {
    Vue.set(state.search, 'error', hasError);
}

export function remove(state, index) {
    state.items.splice(index, 1);
    state.count--;
}

export function shouldRemoveItem(state, mailUid) {
    state.shouldRemoveItem = mailUid;
}