import { UserSettingsClient } from "@bluemind/user.api";

const mockedUserSettingsClient = jest.genMockFromModule("@bluemind/user.api").UserSettingsClient;

Object.getOwnPropertyNames(UserSettingsClient.prototype).forEach(property => {
    // every function of ItemsTransferClient is mocked and return a Promise.resolve
    if (typeof UserSettingsClient.prototype[property] === "function") {
        mockedUserSettingsClient.prototype[property] = jest.fn().mockReturnValue(Promise.resolve());
    }
});

export default mockedUserSettingsClient;
