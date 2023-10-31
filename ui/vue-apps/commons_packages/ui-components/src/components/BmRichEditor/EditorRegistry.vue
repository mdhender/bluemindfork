<script>
import BmRichEditorRegistry from "./BmRichEditorRegistry";

export default {
    functional: true,
    props: {
        editor: { type: [String, Object], required: true }
    },
    render(h, { props, scopedSlots }) {
        let editorComponent;
        if (typeof props.editor === "string") {
            editorComponent = BmRichEditorRegistry.get(props.editor);
        } else {
            editorComponent = props.editor;
        }

        if (editorComponent?.isReady) {
            return scopedSlots.default({
                editorComponent,
                editor: editorComponent.editor
            });
        }
        return "";
    }
};
</script>
