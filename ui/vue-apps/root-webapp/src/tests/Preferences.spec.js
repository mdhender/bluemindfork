import Preferences from "../components/preferences/Preferences";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import { MountComponentUtils } from "@bluemind/test-utils";

import ServiceLocator from "@bluemind/inject";
ServiceLocator.register({
    provide: "i18n",
    factory: () => ({
        t: () => ""
    })
});
ServiceLocator.register({
    provide: "UserSession",
    factory: () => ({ roles: "" })
});

describe("Preferences", () => {
    let wrapper;
    let mockedStore;
    let props;

    beforeEach(() => {
        props = {
            applications: [{ href: "/mail/", icon: {} }, { href: "unknown" }, { href: "/cal/", icon: {} }],
            user: { displayname: "my name" }
        };
        mockedStore = MountComponentUtils.mockSettingsStore();
        window.bmExtensions_ = [];
    });

    test("can be mounted", () => {
        wrapper = MountComponentUtils.createWrapper(Preferences, mockedStore, props);
        expect(wrapper.findComponent(Preferences)).toBeDefined();
    });
});
