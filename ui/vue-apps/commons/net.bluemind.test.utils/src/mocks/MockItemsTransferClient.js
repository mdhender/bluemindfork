import { ItemsTransferClient } from "@bluemind/backend.mail.api";

const mockedItemsTransferClient = jest.genMockFromModule("@bluemind/backend.mail.api").ItemsTransferClient;

Object.getOwnPropertyNames(ItemsTransferClient.prototype).forEach(property => {
    // every function of ItemsTransferClient is mocked and return a Promise.resolve
    if (typeof ItemsTransferClient.prototype[property] === "function") {
        mockedItemsTransferClient.prototype[property] = jest.fn().mockReturnValue(Promise.resolve());
    }
});

export default mockedItemsTransferClient;
