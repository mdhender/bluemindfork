import { ContainerManagementClient } from "@bluemind/core.container.api";
const mockedContainerManagementClient =
    jest.genMockFromModule("@bluemind/core.container.api").ContainerManagementClient;

Object.getOwnPropertyNames(ContainerManagementClient.prototype).forEach(property => {
    // every function of MailboxFoldersClient is mocked and return a Promise.resolve
    if (typeof ContainerManagementClient.prototype[property] === "function") {
        mockedContainerManagementClient.prototype[property] = jest.fn().mockReturnValue(Promise.resolve());
    }
});

mockedContainerManagementClient.prototype.canAccess = jest.fn().mockResolvedValue(true);

export default mockedContainerManagementClient;
