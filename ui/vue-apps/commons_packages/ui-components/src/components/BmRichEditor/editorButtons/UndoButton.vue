<script>
import CommandIconButton from "./CommandIconButton";

export default {
    name: "UndoButton",
    extends: CommandIconButton,
    data() {
        return { title: this.$t("common.undo"), icon: "undo" };
    },
    methods: {
        action() {
            this.editor.undo();
        }
    }
};
</script>
