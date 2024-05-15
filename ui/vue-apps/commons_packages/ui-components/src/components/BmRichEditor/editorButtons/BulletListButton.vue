<script>
import { toggleBullet } from "roosterjs-editor-api";
import CommandIconButton from "./CommandIconButton";

export default {
    name: "BulletListButton",
    extends: CommandIconButton,
    props: {
        isBullet: {
            type: Boolean,
            required: true
        }
    },
    data() {
        return {
            title: this.$t("styleguide.rich_editor.list.tooltip"),
            icon: "list-bulleted"
        };
    },
    computed: {
        isActive() {
            return this.isBullet;
        }
    },
    methods: {
        action() {
            toggleBullet(this.editor);
        }
    }
};
</script>
