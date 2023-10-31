<script>
import { toggleNumbering } from "roosterjs-editor-api";
import CommandIconButton from "./CommandIconButton";

export default {
    name: "NumberListButton",
    extends: CommandIconButton,
    props: {
        isNumbering: {
            type: Boolean,
            required: true
        }
    },
    data() {
        return { title: this.$t("styleguide.rich_editor.numbered_list.tooltip"), icon: "rich-editor-numbered-list" };
    },
    computed: {
        isActive() {
            return this.isNumbering;
        }
    },
    methods: {
        action() {
            toggleNumbering(this.editor);
        }
    }
};
</script>
