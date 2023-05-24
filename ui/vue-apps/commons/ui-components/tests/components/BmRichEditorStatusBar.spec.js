import { mount } from "@vue/test-utils";
import { Editor } from "roosterjs-editor-core";
import { StatusBarPlugin } from "../../src/components/BmRichEditor/plugins";
import BmRichEditorStatusBar from "../../src/components/BmRichEditor/BmRichEditorStatusBar";

describe("STATUS_BAR_PLUGIN", () => {
    it("should show message dispatched by plugin", async () => {
        const plugin = new StatusBarPlugin();
        const wrapper = mount(BmRichEditorStatusBar, {
            propsData: {
                editor: new Editor(document.createElement("div"), { plugins: [plugin] })
            },
            mocks: {
                $t: () => "message dispatched"
            }
        });

        await plugin.dispatch({ type: "STATUS", message: "TEST" });

        expect(plugin.hasMessage).toBeTruthy();
        expect(wrapper.element).toHaveTextContent("message dispatched");
    });
});
