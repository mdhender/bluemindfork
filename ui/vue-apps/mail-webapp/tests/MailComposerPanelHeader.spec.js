import { mount } from "@vue/test-utils";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import MailComposerPanelHeader from "../src/components/MailComposer/MailComposerPanel/MailComposerPanelHeader";

describe("MailComposerPanelHeader", () => {
    let closeButtonSelector = ".close";
    let example = "My Panel Title";

    function defaultMount() {
        return mount(MailComposerPanelHeader, {
            propsData: {
                title: example
            },
            sync: false
        });
    }

    let regexCloseButton = new RegExp(/<button type="button.*class="close/);

    test("is a Vue instance", () => {
        expect(defaultMount().vm).toBeTruthy();
    });

    test("MailComposerPanelHeader should match snapshot", () => {
        expect(defaultMount().vm.$el).toMatchSnapshot();
    });

    test("Simple MailComposerPanelHeader display: a title and a close button", () => {
        const wrapper = defaultMount();
        expect(wrapper.text()).toContain(example);
        expect(wrapper.html()).toMatch(regexCloseButton);
    });

    test("When clicking on close button, a remove event is fired", () => {
        const wrapper = defaultMount();
        wrapper.find(closeButtonSelector).trigger("click");
        expect(wrapper.emitted().remove).toBeTruthy();
    });

    test("No close button", () => {
        const wrapper = mount(MailComposerPanelHeader, {
            propsData: {
                title: example,
                closeable: false
            }
        });
        expect(wrapper.text()).toContain(example);
        expect(wrapper.html()).not.toMatch(regexCloseButton);
    });
});
