import { OwnerSubscriptionsClient } from "@bluemind/core.container.api";

const mockedOwnerSubscriptionsClient = jest.genMockFromModule("@bluemind/core.container.api").OwnerSubscriptionsClient;

Object.getOwnPropertyNames(OwnerSubscriptionsClient.prototype).forEach(property => {
    // every function of MailboxFoldersClient is mocked and return a Promise.resolve
    if (typeof OwnerSubscriptionsClient.prototype[property] === "function") {
        mockedOwnerSubscriptionsClient.prototype[property] = jest.fn().mockReturnValue(Promise.resolve());
    }
});

mockedOwnerSubscriptionsClient.prototype.list = jest.fn().mockResolvedValue([]);

export default mockedOwnerSubscriptionsClient;
