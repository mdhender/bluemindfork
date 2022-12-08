<template>
    <div class="chooser">
        <chooser-header />
        <chooser-main />
        <chooser-footer :max-attachments-size="maxAttachmentsSize" @insert="insert" @cancel="resetChooser" />
    </div>
</template>

<script>
import { mapState } from "vuex";
import ChooserL10N from "../l10n";
import ChooserFooter from "./ChooserFooter/ChooserFooter";
import ChooserHeader from "./ChooserHeader/ChooserHeader";
import ChooserMain from "./ChooserMain/ChooserMain";
import { RESET_CHOOSER } from "./store/actions";

export default {
    name: "Chooser",
    components: { ChooserFooter, ChooserHeader, ChooserMain },
    props: {
        maxAttachmentsSize: {
            type: Number,
            required: true
        }
    },
    componentI18N: { messages: ChooserL10N },
    computed: {
        ...mapState("chooser", ["insertAsLink", "selectedFiles"])
    },
    beforeMount() {
        this.resetChooser();
    },
    methods: {
        insert() {
            this.$emit("insert", this.selectedFiles, this.insertAsLink);
            this.$store.dispatch(`chooser/${RESET_CHOOSER}`);
        },
        resetChooser() {
            this.$store.dispatch(`chooser/${RESET_CHOOSER}`);
            this.$bvModal.hide("chooser-modal");
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.chooser {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    .chooser-header {
        padding: $sp-4 $sp-7 $sp-1 $sp-7;
    }
    .chooser-main {
        overflow: auto;
        height: 80%;
    }
    .chooser-footer {
        box-shadow: $sp-5 $sp-2 $sp-4 $shadow-color;
    }
}
</style>
