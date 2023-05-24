<script>
import BmRichEditorRegistry from "./BmRichEditorRegistry";

export default {
    functional: true,
    props: {
        editor: { type: [String, Object], required: true }
    },
    render(h, { props, scopedSlots }) {
        let richEditor;
        if (typeof props.editor === "string") {
            richEditor = BmRichEditorRegistry.get(props.editor);
        } else {
            richEditor = props.editor;
        }

        if (richEditor?.isReady) {
            return scopedSlots.default({
                richEditor,
                editor: richEditor.editor
            });
        }
        return "";
    }
};
</script>
