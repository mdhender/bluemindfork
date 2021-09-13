<template>
    <bm-modal
        dialog-class="template-chooser"
        :scrollable="false"
        :busy="loading"
        :cancel-title="$t('common.cancel')"
        :ok-disabled="!selected"
        :ok-title="$t('mail.actions.edit_from_template')"
        :title="$t('mail.compose.template_chooser.title')"
        :visible="visible"
        centered
        size="lg"
        @hidden="SET_TEMPLATE_CHOOSER_VISIBLE(false)"
        @shown="SET_TEMPLATE_CHOOSER_VISIBLE(true)"
        @show="reset"
    >
        <div class="d-flex flex-column h-100">
            <bm-form-input
                :value="pattern"
                :placeholder="$t('common.search')"
                icon="search"
                resettable
                left-icon
                :aria-label="$t('common.search')"
                autocomplete="off"
                @reset="reset"
                @input="search"
            />
            <templates-list v-model="selected" class="templates-list flex-fill" />
        </div>
    </bm-modal>
</template>
<script>
import debounce from "lodash.debounce";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { BmModal, BmFormInput } from "@bluemind/styleguide";
import { SET_TEMPLATE_CHOOSER_VISIBLE, SET_TEMPLATE_LIST_SEARCH_PATTERN } from "~/mutations";
import { FETCH_TEMPLATES_KEYS } from "~/actions";
import { MY_TEMPLATES } from "~/getters";
import TemplatesList from "./TemplateChooser/TemplatesList";

export default {
    name: "TemplateChooser",
    components: { BmModal, BmFormInput, TemplatesList },
    data() {
        return {
            selected: 0,
            search: debounce(value => {
                this.SET_TEMPLATE_LIST_SEARCH_PATTERN(value);
                this.loadTemplates();
            }, 1000)
        };
    },
    computed: {
        ...mapGetters("mail", { MY_TEMPLATES }),
        ...mapState("mail", {
            visible: state => state.messageCompose.templateChooser.visible,
            pattern: state => state.messageCompose.templateChooser.pattern,
            loading: state => state.messageCompose.templateChooser.loading
        })
    },
    methods: {
        ...mapMutations("mail", { SET_TEMPLATE_CHOOSER_VISIBLE, SET_TEMPLATE_LIST_SEARCH_PATTERN }),
        ...mapActions("mail", { FETCH_TEMPLATES_KEYS }),
        reset() {
            this.SET_TEMPLATE_LIST_SEARCH_PATTERN("");
            this.loadTemplates();
        },
        async loadTemplates() {
            this.selected = 0;
            await this.FETCH_TEMPLATES_KEYS(this.MY_TEMPLATES);
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.template-chooser {
    &.modal-dialog {
        .modal-body {
            overflow: hidden !important;
        }
        .modal-content {
            height: calc(100vh - 3.5rem);
            max-height: calc(100vh - 3.5rem);
        }
    }
    .templates-list {
        background-color: $extra-light;
        border-color: $secondary;
        border-width: 0 1px 1px 1px;
        border-style: solid;
    }
}
</style>
