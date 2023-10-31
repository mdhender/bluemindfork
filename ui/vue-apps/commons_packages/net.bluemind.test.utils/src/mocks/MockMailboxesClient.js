import { MailboxesClient } from "@bluemind/mailbox.api";

const mockedMailboxesClient = jest.genMockFromModule("@bluemind/mailbox.api").MailboxesClient;

Object.getOwnPropertyNames(MailboxesClient.prototype).forEach(property => {
    if (typeof MailboxesClient.prototype[property] === "function") {
        mockedMailboxesClient.prototype[property] = jest.fn().mockReturnValue(Promise.resolve());
    }
});

export default mockedMailboxesClient;
