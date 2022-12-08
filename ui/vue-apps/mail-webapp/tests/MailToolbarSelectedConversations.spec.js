import MailToolbarSelectedConversations from "../src/components/MailToolbar/MailToolbarSelectedConversations";
jest.mock("@bluemind/ui-components/src/css/_variables.scss", () => ({ iconsColors: "" }));
import { createWrapper } from "./testUtils";
jest.mock("@bluemind/ui-components/src/css/exports/avatar.scss", () => ({ 1: "#007bff" }));
jest.mock("~/store/api/apiConversations");
jest.mock("postal-mime", () => ({ TextEncoder: jest.fn() }));

describe("MailToolbarSelectedConversations", () => {
    test("is a Vue instance", () => {
        const wrapper = createWrapper(MailToolbarSelectedConversations);
        expect(wrapper.vm).toBeTruthy();
    });

    test("should match snapshot", () => {
        const wrapper = createWrapper(MailToolbarSelectedConversations);
        expect(wrapper.element).toMatchSnapshot();
    });
});
