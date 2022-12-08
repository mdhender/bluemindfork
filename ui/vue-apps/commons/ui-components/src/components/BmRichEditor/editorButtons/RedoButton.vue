<script>
import CommandIconButton from "./CommandIconButton";

export default {
    name: "RedoButton",
    extends: CommandIconButton,
    data() {
        return { title: this.$t("common.redo"), icon: "redo" };
    },
    methods: {
        action() {
            this.editor.redo();
        }
    }
};
</script>
