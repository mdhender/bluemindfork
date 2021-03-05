import Preferences from "../components/preferences/Preferences";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import { MountComponentUtils } from "@bluemind/test-utils";

import ServiceLocator from "@bluemind/inject";
ServiceLocator.register({
    provide: "i18n",
    factory: () => {
        return {
            t: () => ""
        };
    }
});

describe("Preferences", () => {
    let wrapper;
    let mockedStore;
    let props;

    beforeEach(() => {
        props = {
            applications: [{ href: "/mail/", icon: {} }, { href: "unknown" }],
            user: { displayname: "my name" }
        };
        mockedStore = MountComponentUtils.mockSessionStore();
    });

    test("can be mounted", () => {
        wrapper = MountComponentUtils.createWrapper(Preferences, mockedStore, props);
        expect(wrapper.findComponent(Preferences)).toBeDefined();
    });
});
