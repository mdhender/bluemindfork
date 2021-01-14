import sessionStore from "../sessionStore";
import inject from "@bluemind/inject";
import { MockUserSettingsClient } from "@bluemind/test-utils";

const userId = "user:id";
const userSettingsClient = new MockUserSettingsClient();
inject.register({ provide: "UserSettingsPersistence", factory: () => userSettingsClient });
inject.register({ provide: "UserSession", factory: () => ({ userId }) });

describe("Store session", () => {
    let context;

    beforeEach(() => {
        context = {
            state: {},
            commit: jest.fn()
        };
    });

    test("FETCH_ALL_SETTINGS action", async () => {
        const mockedSettings = { mySetting: "MY_SETTING" };
        userSettingsClient.get.mockReturnValue(mockedSettings);

        await sessionStore.actions.FETCH_ALL_SETTINGS(context);
        expect(userSettingsClient.get).toHaveBeenCalledWith(userId);
        expect(context.commit).toHaveBeenCalledWith("SET_USER_SETTINGS", expect.anything());
    });

    test("FETCH_ALL_SETTINGS action set default settings if needed", async () => {
        const mockedSettings = { mySetting: "MY_SETTING", mail_message_list_style: "compact" };
        userSettingsClient.get.mockReturnValue(mockedSettings);

        await sessionStore.actions.FETCH_ALL_SETTINGS(context);
        expect(context.commit).toHaveBeenCalledWith("SET_USER_SETTINGS", {
            insert_signature: "true",
            mySetting: "MY_SETTING",
            mail_message_list_style: "compact",
            mail_thread: "false", 
            logout_purge: "false"
        });
    });

    test("UPDATE_ALL_SETTINGS action", async () => {
        const settings = { mySetting: "MY_SETTING" };

        await sessionStore.actions.UPDATE_ALL_SETTINGS(context, settings);
        expect(userSettingsClient.set).toHaveBeenCalledWith(userId, settings);
        expect(context.commit).toHaveBeenCalledWith("SET_USER_SETTINGS", settings);
    });

    test("SET_USER_SETTINGS mutation", async () => {
        const settings = { mySetting: "MY_SETTING" };

        await sessionStore.mutations.SET_USER_SETTINGS(context.state, settings);
        expect(context.state).toEqual({ userSettings: settings });
    });
});
