import MailRouterMixin from "../src/MailRouterMixin";
import { mount } from "@vue/test-utils";

describe("MailRouterMixin", () => {
    const ExampleComponent = {
        name: "ExampleComponent",
        template: "<p>whatever</p>",
        mixins: [MailRouterMixin]
    };

    function mountWithRoute(routeOptions) {
        return mount(ExampleComponent, {
            mocks: {
                $route: routeOptions
            }
        });
    }

    test("compute message route when a message is already displayed", () => {
        const previousMessageKey = "previousMessageKey";
        const wrapper = mountWithRoute({
            path: "/mail/untruc/" + previousMessageKey,
            params: {
                mail: previousMessageKey
            }
        });
        expect(wrapper.vm.computeMessageRoute("folderKey", "messageKey", "")).toEqual("/mail/untruc/messageKey");
    });

    test("compute message route when route is application home or composer ", () => {
        let wrapper = mountWithRoute({
            path: "/mail/new",
            params: {}
        });
        expect(wrapper.vm.computeMessageRoute("folderKey", "messageKey", "exampleFilter")).toEqual(
            "/mail/folderKey/messageKey?filter=exampleFilter"
        );

        wrapper = mountWithRoute({
            path: "/mail/",
            params: {}
        });
        expect(wrapper.vm.computeMessageRoute("folderKey", "messageKey", "")).toEqual("/mail/folderKey/messageKey");
    });

    test("compute message route when a list is already displayed (search or folder)", () => {
        const wrapper = mountWithRoute({
            path: "/mail/un/path/quelconque/",
            params: {}
        });
        expect(wrapper.vm.computeMessageRoute("folderKey", "messageKey", "")).toEqual(
            "/mail/un/path/quelconque/messageKey"
        );
    });
});
