import BmSettingsContent from "../components/settings/BmSettingsContent";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import { MountComponentUtils } from "@bluemind/test-utils";

describe("BmSettings", () => {
    let wrapper;
    let mockedStore;
    let props;

    beforeEach(() => {
        props = {
            selectedApp: null,
            status: "loading",
            applications: [{ href: "/mail/", icon: {} }, { href: "unknown" }]
        };
        mockedStore = MountComponentUtils.mockSessionStore();
    });

    test("if any local settings change, status is idle", async () => {
        wrapper = MountComponentUtils.createWrapper(BmSettingsContent, mockedStore, props);
        await wrapper.findComponent(BmSettingsContent).setData({ localUserSettings: "updated !" });
        expect(wrapper.emitted("changeStatus")[0][0]).toBe("idle");
    });

    test("if any local settings change, save and cancel can be clickable", async () => {
        wrapper = MountComponentUtils.createWrapper(BmSettingsContent, mockedStore, props);
        expect(wrapper.find("button[type='submit'][disabled='disabled'").exists()).toBe(true);
        expect(wrapper.find("button[type='reset'][disabled='disabled'").exists()).toBe(true);
        await wrapper.findComponent(BmSettingsContent).setData({ localUserSettings: "updated !" });
        expect(wrapper.find("button[type='reset'][disabled='disabled'").exists()).toBe(false);
        expect(wrapper.find("button[type='submit'][disabled='disabled'").exists()).toBe(false);
    });
});
