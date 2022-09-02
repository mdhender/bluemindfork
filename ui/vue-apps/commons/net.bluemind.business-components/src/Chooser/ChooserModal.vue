<template>
    <bm-modal id="chooser-modal" centered size="fluid" :scrollable="false" footer-class="shadow" @hide="resetChooser">
        <template #modal-title>
            <chooser-header />
        </template>
        <chooser-main />
        <template #modal-footer>
            <chooser-footer :max-attachments-size="maxAttachmentsSize" @insert="insert" @cancel="resetChooser" />
        </template>
    </bm-modal>
</template>

<script>
import { mapState } from "vuex";
import { BmModal } from "@bluemind/styleguide";
import ChooserL10N from "../l10n";
import ChooserFooter from "./ChooserFooter/ChooserFooter";
import ChooserHeader from "./ChooserHeader/ChooserHeader";
import ChooserMain from "./ChooserMain/ChooserMain";
import { RESET_CHOOSER } from "./store/actions";

export default {
    name: "ChooserModal",
    components: { BmModal, ChooserFooter, ChooserHeader, ChooserMain },
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
@import "~@bluemind/styleguide/css/_variables";
@import "~@bluemind/styleguide/css/mixins/_responsiveness";

#chooser-modal {
    .modal-content {
        height: 80vh;
    }
    header {
        background-color: $neutral-bg-lo1;

        .modal-title {
            font-size: $font-size-base;
            width: 100%;
        }
    }

    .modal-body {
        padding: 0;
        overflow-y: auto;
    }
    .modal-footer {
        padding-top: 0 !important;
        border-top: 1px solid $border-color;
    }
}
</style>
