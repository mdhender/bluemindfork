import { loadUserSettings } from "../../actions/loadUserSettings";

const mockedMessageListStyle = "full";
jest.mock("@bluemind/inject", () => {
    return {
        getProvider() {
            return {
                get: () => {
                    return { getOne: jest.fn().mockReturnValue(Promise.resolve(mockedMessageListStyle)) };
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

describe("[Mail-WebappStore][actions] : loadUserSettings", () => {
    test("load user settings and call mutation", async () => {
        await loadUserSettings(context);
        expect(context.commit).toHaveBeenCalledWith("setUserSettings", {
            mail_message_list_style: mockedMessageListStyle
        });
    });
});
