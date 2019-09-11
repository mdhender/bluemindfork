import Message from "./Message.js";

export function messages(state) {
    return (state.sortedIds || []).map(id => {
        return state.items[id] && new Message(state.items[id]);
    });
}

export function count(state) {
    return state.sortedIds.length;
}

export function indexOf(state) {
    return id => state.sortedIds.indexOf(id);
}
export function currentMessage(state) {
    if (state.current) {
        let message = new Message(state.items[state.current]);
        message.flags.push("hasAttachment");
        return message;
    }
}

export function currentParts(state) {
    if (state.current) {
        return state.partsToDisplay;
    }
}

export function attachments(state) {
    if (state.current) {
        return state.attachments;
    }
}

export function getLastRecipients(state, getters, { max = 5 }) {
    let lastRecipients = [];
    const messages = getters.messages;
    for (let i = 0; i < messages.length; i++) {
        if (lastRecipients.length === max) {
            break;
        }
        const message = messages[i];
        if (!message) {
            continue;
        }
        const allRecipients = message.to
            .concat(message.cc)
            .concat(message.bcc)
            .map(recipient => ({ email: recipient.address, formattedName: recipient.formattedName }))
            .filter(recipient => !lastRecipients.some(r => r.email === recipient.email));
        lastRecipients = lastRecipients.concat(allRecipients.splice(0, max - lastRecipients.length));
    }
    return lastRecipients;
}
