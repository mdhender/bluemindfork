import MailToolbarSelectedMessages from "../src/MailToolbar/MailToolbarSelectedMessages";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import { createWrapper, createStore } from "./testUtils";

describe("MailToolbarSelectedMessages", () => {
    test("is a Vue instance", () => {
        const wrapper = createWrapper(MailToolbarSelectedMessages);
        expect(wrapper.isVueInstance()).toBeTruthy();
    });

    test("should match snapshot", () => {
        const wrapper = createWrapper(MailToolbarSelectedMessages);
        expect(wrapper.element).toMatchSnapshot();
    });

    test("should display 'mark unread' button if the message is read", () => {
        const wrapper = createWrapper(MailToolbarSelectedMessages);
        expect(wrapper.find(".btn.read").isVisible()).toBe(true);
        expect(wrapper.find(".btn.unread").isVisible()).toBe(false);
    });

    test("should display 'mark read' button if the message is unread", () => {
        const storeWithReadMessage = createStore({
            modules: {
                "mail-webapp": {
                    modules: {
                        currentMessage: {
                            getters: {
                                message: jest.fn(() => {
                                    return {
                                        key:
                                            "WyIxNWUwZjNjYS01M2E2LTRiYmItYWQ0NS02MTgwNjcyYmE4ZWMiLCIzNUU1MTJCOC0xRDVBLTRENkQtQUMzOC01QzY4OENDQzlBMDUiXQ==",
                                        states: ["not-seen"],
                                        flags: []
                                    };
                                })
                            }
                        }
                    }
                }
            }
        });

        const wrapper = createWrapper(MailToolbarSelectedMessages, { store: storeWithReadMessage });
        expect(wrapper.find(".btn.unread").isVisible()).toBe(true);
        expect(wrapper.find(".btn.read").isVisible()).toBe(false);
    });
});
