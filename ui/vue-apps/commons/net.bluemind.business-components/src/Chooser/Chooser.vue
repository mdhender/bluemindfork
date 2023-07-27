<template>
    <bm-modal v-model="show" content-class="chooser" size="xl" height="lg" variant="advanced" scrollable>
        <template #modal-header>
            <chooser-header @close="resetChooser" />
        </template>
        <chooser-main />
        <template #modal-footer>
            <chooser-footer :max-attachments-size="maxAttachmentsSize" @insert="insert" @cancel="resetChooser" />
        </template>
    </bm-modal>
</template>

<script>
import { mapState } from "vuex";
import { BmModal } from "@bluemind/ui-components";
import ChooserFooter from "./ChooserFooter/ChooserFooter";
import ChooserHeader from "./ChooserHeader/ChooserHeader";
import ChooserMain from "./ChooserMain/ChooserMain";
import { RESET_CHOOSER } from "./store/actions";

export default {
    name: "Chooser",
    components: { BmModal, ChooserFooter, ChooserHeader, ChooserMain },
    props: {
        maxAttachmentsSize: {
            type: Number,
            required: true
        }
    },
    data() {
        return {
            show: false
        };
    },
    computed: {
        ...mapState("chooser", ["insertAsLink", "selectedFiles"])
    },
    beforeMount() {
        this.resetChooser();
    },
    methods: {
        open() {
            this.show = true;
        },
        hide() {
            this.show = false;
        },
        insert() {
            this.$emit("insert", this.selectedFiles, this.insertAsLink);
            this.$store.dispatch(`chooser/${RESET_CHOOSER}`);
        },
        resetChooser() {
            this.$store.dispatch(`chooser/${RESET_CHOOSER}`);
            this.hide();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.chooser {
    .modal-header {
        z-index: 1;
        border-bottom: none !important;
    }
    .modal-body {
        padding: 0 !important;
        background-color: $backdrop;
    }
    .modal-footer {
        padding-right: $sp-5 !important;
    }
}
</style>
