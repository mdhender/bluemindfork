import { EditorPlugin, IEditor, PluginDomEvent, PluginEvent, PluginEventType } from "roosterjs-editor-types";
import { ObservablePlugin } from "./StatusBarPlugin";

type Nullable<T> = T | null;

enum Keys {
    TAB = "Tab",
    ESCAPE = "Escape"
}

export class TabEventPlugin implements EditorPlugin {
    private _editor!: Nullable<IEditor>;
    protected statusBarDispatcher!: ObservablePlugin;
    private navigationEnable = false;

    getName() {
        return "TabEventPlugin";
    }

    initialize(editor: IEditor) {
        this._editor = editor;
        this.statusBarDispatcher = (<any>this._editor).core.plugins.find(
            (p: EditorPlugin) => p.getName() === "StatusBarPlugin"
        );
    }

    dispose() {
        this._editor = null;
    }

    willHandleEventExclusively(event: PluginEvent) {
        const rawEvent = this.extractKeyboardEvent(event);
        return Boolean(rawEvent?.key) && rawEvent.key === Keys.TAB && this.navigationEnable;
    }

    onPluginEvent(pluginEvent: PluginEvent) {
        if (this.shouldInterceptEvent(pluginEvent)) {
            this.handleKeyboardEvent(this.extractKeyboardEvent(pluginEvent));
        }
    }

    private shouldInterceptEvent(pluginEvent: PluginEvent) {
        return pluginEvent.eventType === PluginEventType.KeyDown;
    }

    private extractKeyboardEvent(event: PluginEvent): KeyboardEvent {
        const domEvent = <PluginDomEvent>event;
        const keyboardEvent = <KeyboardEvent>domEvent.rawEvent;
        return keyboardEvent;
    }

    private handleKeyboardEvent = (event: KeyboardEvent) => {
        switch (event.key) {
            case Keys.ESCAPE:
                if (!this.navigationEnable) {
                    event.stopPropagation();
                }
                break;
            case Keys.TAB:
                if (this.navigationEnable) {
                    event.stopPropagation();
                } else {
                    this.dispatchMessage(Keys.TAB);
                }
                break;
        }
        this.navigationEnable = event.key === Keys.ESCAPE;
    };

    private dispatchMessage(message: string) {
        this.statusBarDispatcher.dispatch({ type: "STATUS", message });
    }
}
