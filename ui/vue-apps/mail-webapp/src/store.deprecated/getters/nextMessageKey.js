export function nextMessageKey(state, getters, rootState) {
    const messageListKeys = rootState.mail.messageList.messageKeys;
    const count = getters["messages/count"];
    let selection = rootState.mail.selection.length ? rootState.mail.selection : [state.currentMessage.key];
    const otherMessageAvailable = selection.length < count;

    if (otherMessageAvailable) {
        selection = sortSelectionLikeItemKeys(messageListKeys, selection);
        let nextIndex = retrieveIndexAfterSelection(getters, selection);
        let nextIndexIsOutOfBounds = nextIndex === count;

        if (nextIndexIsOutOfBounds) {
            nextIndex = retrieveIndexBeforeSelection(getters, selection);
            const nextIndexIsOutOfBounds = nextIndex < 0;

            if (nextIndexIsOutOfBounds) {
                nextIndex = retrieveFirstAvailableIndexInSelectionBounds(getters, selection);
            }
        }
        return messageListKeys[nextIndex];
    }
}

function retrieveIndexAfterSelection(getters, selection) {
    return getters["messages/indexOf"](selection[selection.length - 1]) + 1;
}

function retrieveIndexBeforeSelection(getters, selection) {
    return getters["messages/indexOf"](selection[0]) - 1;
}

function retrieveFirstAvailableIndexInSelectionBounds(getters, selection) {
    const selectionIndexes = selection.map(key => getters["messages/indexOf"](key));
    let previousIndex = 0;
    selectionIndexes.find(index => {
        if (index - previousIndex > 1) {
            return true;
        }
        previousIndex = index;
        return false;
    });
    return previousIndex + 1;
}

function sortSelectionLikeItemKeys(messageListKeys, selection) {
    return messageListKeys.filter(key => selection.includes(key));
}
