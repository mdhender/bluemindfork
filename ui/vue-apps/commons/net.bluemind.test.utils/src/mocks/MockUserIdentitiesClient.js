import { UserMailIdentitiesClient } from "@bluemind/user.api";

const mockedUserMailIdentitiesClient = jest.genMockFromModule("@bluemind/user.api").UserMailIdentitiesClient;

Object.getOwnPropertyNames(UserMailIdentitiesClient.prototype).forEach(property => {
    if (typeof UserMailIdentitiesClient.prototype[property] === "function") {
        mockedUserMailIdentitiesClient.prototype[property] = jest.fn().mockReturnValue(Promise.resolve());
    }
});

export default mockedUserMailIdentitiesClient;
