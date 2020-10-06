import { mount } from "@vue/test-utils";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import MailComposerPanel from "../src/components/MailComposer/MailComposerPanel";

describe("MailComposerPanelHeader", () => {
    let closeButtonSelector = ".close";
    let title = "My Panel Title";
    let bodyContent = "<span>My body is a temple.</span>";
    let footerContent = "<button>Submit</button>";

    function defaultMount() {
        return mount(MailComposerPanel, {
            propsData: {
                title: title
            },
            slots: {
                body: bodyContent,
                footer: footerContent
            },
            sync: false
        });
    }

    let regexCloseButton = new RegExp(/<button type="button".*class="close/);
    let regexBodyContent = new RegExp(/<span>\s*My body is a temple.\s*<\/span>/);
    let regexFooterContent = new RegExp(/<button>\s*Submit\s*<\/button>/);

    test("is a Vue instance", () => {
        expect(defaultMount().vm).toBeTruthy();
    });

    test("MailComposerPanel should match snapshot", () => {
        expect(defaultMount().vm.$el).toMatchSnapshot();
    });

    test("Simple MailComposerPanel display: a title, a body and a footer", () => {
        const wrapper = defaultMount();
        expect(wrapper.text()).toContain(title);
        expect(wrapper.html()).toMatch(regexCloseButton);
        expect(wrapper.html()).toMatch(regexBodyContent);
        expect(wrapper.html()).toMatch(regexFooterContent);
    });

    test("When clicking on close button, a remove event is fired", () => {
        const wrapper = defaultMount();
        wrapper.find(closeButtonSelector).trigger("click");
        expect(wrapper.emitted().remove).toBeTruthy();
    });

    test("No close button", () => {
        const wrapper = mount(MailComposerPanel, {
            propsData: {
                title: title,
                closeable: false
            }
        });
        expect(wrapper.text()).toContain(title);
        expect(wrapper.html()).not.toMatch(regexCloseButton);
    });
});
