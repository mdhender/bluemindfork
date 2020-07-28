import { setUserSettings } from "../../src/mutations/setUserSettings";

describe("[Mail-WebappStore][mutations] : setUserSettings", () => {
    const userSettingsData = { myKey: "myValue" };

    test("set user settings", () => {
        const state = { userSettings: {} };
        setUserSettings(state, userSettingsData);
        expect(state.userSettings).toEqual(userSettingsData);
    });
});
