import { loadMailboxConfig } from "../../actions/loadMailboxConfig";

const mockedMessageMaxSize = 666;
jest.mock("@bluemind/inject", () => {
    return {
        getProvider() {
            return {
                get: () => {
                    return {
                        getMailboxConfig: jest
                            .fn()
                            .mockReturnValue(Promise.resolve({ messageMaxSize: mockedMessageMaxSize }))
                    };
                }
            };
        }
    };
});

const context = {
    dispatch: jest.fn(),
    commit: jest.fn(),
    getters: {
        my: {
            uid: "uid"
        }
    }
};

describe("[Mail-WebappStore][actions] : loadMailboxConfig", () => {
    test("set max message size", async () => {
        await loadMailboxConfig(context);
        expect(context.commit).toHaveBeenCalledWith("setMaxMessageSize", mockedMessageMaxSize);
    });
});
