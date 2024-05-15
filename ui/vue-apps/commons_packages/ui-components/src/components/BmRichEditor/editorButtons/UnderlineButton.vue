<script>
import { toggleUnderline } from "roosterjs-editor-api";
import CommandIconButton from "./CommandIconButton";

export default {
    name: "UnderlineButton",
    extends: CommandIconButton,
    props: {
        isUnderline: {
            type: Boolean,
            required: true
        }
    },
    data() {
        return {
            title: this.$t("styleguide.rich_editor.underline.tooltip"),
            icon: "underlined"
        };
    },
    computed: {
        isActive() {
            return this.isUnderline;
        }
    },
    methods: {
        action() {
            toggleUnderline(this.editor);
        }
    }
};
</script>
