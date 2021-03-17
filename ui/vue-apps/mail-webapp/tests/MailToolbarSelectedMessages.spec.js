import MailToolbarSelectedMessages from "../src/components/MailToolbar/MailToolbarSelectedMessages";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import { createWrapper } from "./testUtils";

describe("MailToolbarSelectedMessages", () => {
    test("is a Vue instance", () => {
        const wrapper = createWrapper(MailToolbarSelectedMessages);
        expect(wrapper.vm).toBeTruthy();
    });

    test("should match snapshot", () => {
        const wrapper = createWrapper(MailToolbarSelectedMessages);
        expect(wrapper.element).toMatchSnapshot();
    });
});
