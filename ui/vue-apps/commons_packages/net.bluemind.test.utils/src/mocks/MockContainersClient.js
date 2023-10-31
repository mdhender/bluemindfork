import { ContainersClient } from "@bluemind/core.container.api";

const mockedContainersClient = jest.genMockFromModule("@bluemind/core.container.api").ContainersClient;

Object.getOwnPropertyNames(ContainersClient.prototype).forEach(property => {
    // every function of MailboxFoldersClient is mocked and return a Promise.resolve
    if (typeof ContainersClient.prototype[property] === "function") {
        mockedContainersClient.prototype[property] = jest.fn().mockReturnValue(Promise.resolve());
    }
});

export default mockedContainersClient;
