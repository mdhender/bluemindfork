<script>
import { toggleBlockQuote } from "roosterjs-editor-api";
import CommandIconButton from "./CommandIconButton";

export default {
    name: "BlockQuoteButton",
    extends: CommandIconButton,
    props: {
        isBlockQuote: {
            type: Boolean,
            required: true
        }
    },
    data() {
        return { title: this.$t("styleguide.rich_editor.block_quote.tooltip"), icon: "quote" };
    },
    computed: {
        isActive() {
            return this.isBlockQuote;
        }
    },
    methods: {
        action() {
            toggleBlockQuote(this.editor, element => {
                element.style.marginLeft = "1rem";
                element.style.borderLeft = "2px solid var(--neutral-fg)";
                element.style.paddingLeft = "1rem";
            });
        }
    }
};
</script>
