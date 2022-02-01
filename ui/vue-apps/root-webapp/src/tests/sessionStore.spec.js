import settingsStore from "../settingsStore";
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

        await settingsStore.actions.FETCH_ALL_SETTINGS(context);
        expect(userSettingsClient.get).toHaveBeenCalledWith(userId);
        expect(context.commit).toHaveBeenCalledWith("SET_SETTINGS", expect.anything());
    });

    test("FETCH_ALL_SETTINGS action set default settings if needed", async () => {
        const mockedSettings = { mySetting: "MY_SETTING", mail_message_list_style: "compact" };
        userSettingsClient.get.mockReturnValue(mockedSettings);

        await settingsStore.actions.FETCH_ALL_SETTINGS(context);
        expect(context.commit).toHaveBeenCalledWith("SET_SETTINGS", {
            always_show_from: "false",
            always_show_quota: "false",
            auto_select_from: "never",
            default_event_alert_mode: "Display",
            insert_signature: "true",
            logout_purge: "false",
            mySetting: "MY_SETTING",
            mail_message_list_style: "compact",
            mail_thread: "false",
            trust_every_remote_content: "false"
        });
    });

    test("SET_SETTINGS mutation", () => {
        const settings = { mySetting: "MY_SETTING" };
        settingsStore.mutations.SET_SETTINGS(context.state, settings);
        expect(context.state).toEqual({ mySetting: "MY_SETTING" });
    });
});
