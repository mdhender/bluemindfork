import MailToolbarSelectedMessages from "../src/components/MailToolbar/MailToolbarSelectedMessages";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import { createWrapper, createStore, messageKey } from "./testUtils";

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
                                        key: messageKey,
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
