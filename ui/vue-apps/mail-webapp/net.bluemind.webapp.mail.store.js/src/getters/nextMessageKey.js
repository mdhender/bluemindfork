export function nextMessageKey(state, getters) {
    const count = getters["messages/count"];
    let selection = state.selectedMessageKeys.length ? state.selectedMessageKeys : [state.currentMessage.key];
    const otherMessageAvailable = selection.length < count;

    if (otherMessageAvailable) {
        selection = sortSelectionLikeItemKeys(state, selection);
        let nextIndex = retrieveIndexAfterSelection(getters, selection);
        let nextIndexIsOutOfBounds = nextIndex === count;

        if (nextIndexIsOutOfBounds) {
            nextIndex = retrieveIndexBeforeSelection(getters, selection);
            const nextIndexIsOutOfBounds = nextIndex < 0;

            if (nextIndexIsOutOfBounds) {
                nextIndex = retrieveFirstAvailableIndexInSelectionBounds(getters, selection);
            }
        }
        return state.messages.itemKeys[nextIndex];
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

function sortSelectionLikeItemKeys(state, selection) {
    return state.messages.itemKeys.filter(key => selection.includes(key));
}
