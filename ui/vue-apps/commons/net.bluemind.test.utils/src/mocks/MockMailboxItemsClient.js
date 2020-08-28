import { MailboxItemsClient } from "@bluemind/backend.mail.api";

const mockedMailboxItemClient = jest.genMockFromModule("@bluemind/backend.mail.api").MailboxItemsClient;

Object.getOwnPropertyNames(MailboxItemsClient.prototype).forEach(property => {
    // every function of MailboxItemsClients is mocked and return a Promise.resolve
    if (typeof MailboxItemsClient.prototype[property] === "function") {
        mockedMailboxItemClient.prototype[property] = jest.fn().mockReturnValue(Promise.resolve());
    }
});

mockedMailboxItemClient.prototype.mockFetch = function (text) {
    mockedMailboxItemClient.prototype.fetch.mockImplementation(() => {
        return Promise.resolve(new Blob([text], { type: "text/plain" }));
    });
};

mockedMailboxItemClient.prototype.getPerUserUnread.mockReturnValue({ count: 0 });

export default mockedMailboxItemClient;
