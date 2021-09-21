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
        @ok="useTemplate"
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
            <templates-list v-model="selected" class="templates-list flex-fill" @ok="useTemplate" />
        </div>
    </bm-modal>
</template>
<script>
import debounce from "lodash.debounce";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { BmModal, BmFormInput } from "@bluemind/styleguide";
import { SET_MESSAGE_SUBJECT, SET_TEMPLATE_CHOOSER_VISIBLE, SET_TEMPLATE_LIST_SEARCH_PATTERN } from "~/mutations";
import { FETCH_TEMPLATES_KEYS } from "~/actions";
import { MY_TEMPLATES } from "~/getters";
import TemplatesList from "./TemplateChooser/TemplatesList";
import { ComposerInitMixin } from "~/mixins";

export default {
    name: "TemplateChooser",
    components: { BmModal, BmFormInput, TemplatesList },
    mixins: [ComposerInitMixin],
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
            loading: state => state.messageCompose.templateChooser.loading,
            target: state => state.messageCompose.templateChooser.target,
            conversationByKey: state => state.conversations.conversationByKey,
            messages: state => state.conversations.messages
        })
    },
    methods: {
        ...mapMutations("mail", {
            SET_MESSAGE_SUBJECT,
            SET_TEMPLATE_CHOOSER_VISIBLE,
            SET_TEMPLATE_LIST_SEARCH_PATTERN
        }),
        ...mapActions("mail", { FETCH_TEMPLATES_KEYS }),
        reset() {
            this.SET_TEMPLATE_LIST_SEARCH_PATTERN("");
            this.loadTemplates();
        },
        async loadTemplates() {
            this.selected = 0;
            await this.FETCH_TEMPLATES_KEYS(this.MY_TEMPLATES);
        },
        useTemplate() {
            const template = this.messages[this.conversationByKey[this.selected].messages[0]];
            const target = this.messages[this.target];
            if (!target.subject.trim()) {
                this.SET_MESSAGE_SUBJECT({ messageKey: target.key, subject: template.subject });
            }
            if (!(target.to.length || target.cc.length || target.bcc.length)) {
                target.to = template.to.slice();
                target.cc = template.cc.slice();
                target.bcc = template.bcc.slice();
            }
            this.mergeMessageContent(target, template);
            this.SET_TEMPLATE_CHOOSER_VISIBLE(false);
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
