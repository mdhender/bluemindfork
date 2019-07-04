import Message from "./Message.js";

export function messages(state) {
    return (state.items || []).map(item => {
        return new Message(item);
    });
}

export function currentMessage(state, getters) {
    if (state.current) {
        let message = getters.messageByUid(state.current);
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

export function messageByUid(state) {
    return uid => {
        const mailboxItem = state.items.find(item => item.uid == uid);
        if (mailboxItem) {
            return new Message(mailboxItem);
        }
    };
}

export function countUnreadMessages(state, getters) {
    return getters.messages.filter(message => message.states.includes("not-seen")).length;
}

export function getLastRecipients(state, getters, { max = 5 }) {
    let lastRecipients = [];
    const messages = getters.messages;
    for (let i = 0; i < messages.length; i++ ) {
        if (lastRecipients.length === max) {
            break;
        }
        const message = messages[i];
        const allRecipients = message.to
            .concat(message.cc)
            .concat(message.bcc)
            .map(recipient => ({ email: recipient.address, formattedName: recipient.formattedName }))
            .filter(recipient => !lastRecipients.some(r => r.email === recipient.email));
        lastRecipients = lastRecipients.concat(allRecipients.splice(0, max - lastRecipients.length));
    }
    return lastRecipients;
}