/* eslint-disable @typescript-eslint/no-empty-function */
import { Editor } from "roosterjs-editor-core";
import { TabEventPlugin } from "../../src/components/BmRichEditor/plugins";
import { ObservablePlugin } from "../../src/components/BmRichEditor/plugins/StatusBarPlugin";
import { EditorPlugin, PluginEventType } from "roosterjs-editor-types";
describe("tabEventPLugin", () => {
    let pluginTest: ObservablePlugin & { spy: jest.Mock<any, any> };
    let plugin: Required<EditorPlugin>;
    beforeEach(() => {
        pluginTest = new SpyPlugin();
        plugin = new TestableTabEventPlugin();
        new Editor(document.createElement("div"), {
            plugins: [pluginTest, plugin]
        });
        (<TestableTabEventPlugin>plugin).replaceDispatcher = pluginTest;
    });

    it("should dispatch message to registered plugins when Tab key has been pressed", () => {
        triggerEventKey(plugin, ["Tab"]);

        expect(pluginTest.spy).toHaveBeenCalledTimes(1);
        expect(pluginTest.spy).toHaveBeenCalledWith({ type: "STATUS", message: "Tab" });
    });

    it("should not dispatch message if Escape was pressed before Tab (stoping propagation)", () => {
        const event = triggerEventKey(plugin, ["Escape", "Tab"]);

        expect(event.spy()).toHaveBeenCalledTimes(2);
        expect(pluginTest.spy).not.toHaveBeenCalled();
    });

    it("should bubbling event if Escape is pressed twice ", () => {
        const event = triggerEventKey(plugin, ["Escape", "Escape"]);

        expect(event.spy()).toHaveBeenCalledTimes(1);
    });
});

function triggerEventKey(plugin: Required<EditorPlugin>, keys: string[]) {
    const spy = jest.fn();
    keys.forEach(key => {
        plugin.onPluginEvent({
            eventType: PluginEventType.KeyDown,
            rawEvent: { key, stopPropagation: spy, preventDefault: spy } as any
        });
    });
    return {
        _spy: spy,
        spy() {
            return this._spy;
        }
    };
}

class SpyPlugin implements ObservablePlugin {
    private spyfn = jest.fn();
    get spy() {
        return this.spyfn;
    }
    dispatch(message: { type: string; message: string }): void {
        this.spyfn(message);
    }
    getName() {
        return "Spy";
    }
    initialize() {}
    dispose() {}
}

class TestableTabEventPlugin extends TabEventPlugin {
    set replaceDispatcher(plugin: ObservablePlugin) {
        this.statusBarDispatcher = plugin;
    }
}
