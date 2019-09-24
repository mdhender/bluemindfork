import Vue from "vue";
import DraftStatus from "./DraftStatus";

export function setItems(state, items) {
    items.forEach(item => {
        Vue.set(state.items, item.internalId, item);
    });
}

export function setSortedIds(state, ids) {
    state.sortedIds = ids;
}

export function setCurrent(state, id) {
    state.current = parseInt(id);
}

export function updateSeen(state, { id, isSeen }) {
    let mailboxItem = state.items[id];
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
    Vue.set(state.search, "pattern", pattern);
}

export function setSearchLoading(state, isLoading) {
    Vue.set(state.search, "loading", isLoading);
}

export function setSearchError(state, hasError) {
    Vue.set(state.search, "error", hasError);
}

export function remove(state, index) {
    const id = state.sortedIds.splice(index, 1);
    Vue.delete(state.items, id);
    if (state.current === id || state.sortedIds.length === 0) {
        state.current = null;
    }
}

export function shouldRemoveItem(state, mailUid) {
    state.shouldRemoveItem = mailUid;
}

export function updateDraft(state, draft) {
    Object.assign(state.draft, draft);
}

/** Update or reset the draft in the store. */
export function setDraft(state, { draft, isNew }) {
    const draftCopy = Object.assign({}, draft);
    if (isNew) {
        draftCopy.status = DraftStatus.NEW;
        draftCopy.id = null;
        draftCopy.saveDate = null;
    }
    updateDraft(state, draftCopy);
}

export function setUnreadCount(state, count) {
    state.unreadCount = count;
}
