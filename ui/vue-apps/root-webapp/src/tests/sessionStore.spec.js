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
            state: { settings: { local: {}, remote: {} } },
            commit: jest.fn()
        };
    });

    test("FETCH_ALL_SETTINGS action", async () => {
        const mockedSettings = { mySetting: "MY_SETTING" };
        userSettingsClient.get.mockReturnValue(mockedSettings);

        await sessionStore.actions.FETCH_ALL_SETTINGS(context);
        expect(userSettingsClient.get).toHaveBeenCalledWith(userId);
        expect(context.commit).toHaveBeenCalledWith("SET_SETTINGS", expect.anything());
    });

    test("FETCH_ALL_SETTINGS action set default settings if needed", async () => {
        const mockedSettings = { mySetting: "MY_SETTING", mail_message_list_style: "compact" };
        userSettingsClient.get.mockReturnValue(mockedSettings);

        await sessionStore.actions.FETCH_ALL_SETTINGS(context);
        expect(context.commit).toHaveBeenCalledWith("SET_SETTINGS", {
            always_show_from: "false",
            always_show_quota: "false",
            default_event_alert_mode: "Display",
            insert_signature: "true",
            logout_purge: "false",
            mySetting: "MY_SETTING",
            mail_message_list_style: "compact",
            mail_thread: "false",
            trust_every_remote_content: "false"
        });
    });

    test("SAVE_SETTINGS action", async () => {
        const settings = { mySetting: "MY_SETTING" };
        context.state.settings.local = settings;
        await sessionStore.actions.SAVE_SETTINGS(context);
        expect(userSettingsClient.set).toHaveBeenCalledWith(userId, settings);
        expect(context.commit).toHaveBeenCalledWith("SET_SETTINGS", settings);
    });

    test("SET_SETTINGS mutation", () => {
        const settings = { mySetting: "MY_SETTING" };
        sessionStore.mutations.SET_SETTINGS(context.state, settings);
        expect(context.state).toEqual({
            settings: { local: { mySetting: "MY_SETTING" }, remote: { mySetting: "MY_SETTING" } }
        });
    });

    test("SETTINGS_CHANGED getter", () => {
        context.state.settings.local = { settingOne: "blue" };
        context.state.settings.remote = { settingOne: "mind" };
        expect(sessionStore.getters.SETTINGS_CHANGED(context.state)).toBeTruthy();
        context.state.settings.remote = { settingOne: "blue" };
        expect(sessionStore.getters.SETTINGS_CHANGED(context.state)).toBeFalsy();
    });
});
