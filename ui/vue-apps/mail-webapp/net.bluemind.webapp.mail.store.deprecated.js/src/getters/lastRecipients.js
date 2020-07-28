//FIXME This does not work correctly. It should either check mails in send folder
// Or store recipients when sending a mail
export function lastRecipients(unused, getters, { max = 5 }) {
    let lastRecipients = [];
    const messages = getters["messages/messages"];
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
