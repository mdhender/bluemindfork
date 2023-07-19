<template>
    <bm-modal
        dialog-class="template-chooser"
        variant="advanced"
        scrollable
        :busy="loading"
        :cancel-title="$t('common.cancel')"
        :ok-disabled="!selected"
        :ok-title="$t('mail.actions.edit_from_template')"
        :visible="visible"
        size="md"
        height="lg"
        @hidden="SET_TEMPLATE_CHOOSER_VISIBLE(false)"
        @shown="SET_TEMPLATE_CHOOSER_VISIBLE(true)"
        @show="reset"
        @ok="useTemplate"
    >
        <template #modal-header="{ close }">
            <bm-modal-header
                :class="{ 'with-mobile-search-input': showMobileSearchInput }"
                :title="$t('mail.compose.template_chooser.title')"
                @close="close"
            >
                <bm-form-input
                    :value="pattern"
                    :placeholder="$t('common.search')"
                    class="desktop-search-input"
                    variant="underline"
                    icon="search"
                    resettable
                    left-icon
                    :aria-label="$t('common.search')"
                    autocomplete="off"
                    @reset="reset"
                    @input="search"
                />
                <bm-form-input
                    v-if="showMobileSearchInput"
                    :value="pattern"
                    :placeholder="$t('common.search')"
                    class="mobile-search-input"
                    icon="search"
                    resettable
                    left-icon
                    :aria-label="$t('common.search')"
                    autocomplete="off"
                    @reset="reset"
                    @input="search"
                />
                <bm-icon-button
                    v-else
                    class="d-lg-none search-button"
                    variant="compact-on-fill-primary"
                    size="lg"
                    icon="search"
                    @click="showMobileSearchInput = true"
                />
            </bm-modal-header>
        </template>
        <templates-list v-model="selected" class="templates-list flex-fill" @ok="useTemplate" />
    </bm-modal>
</template>
<script>
import debounce from "lodash.debounce";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { draftUtils } from "@bluemind/mail";
import { BmModal, BmModalHeader, BmFormInput, BmIconButton } from "@bluemind/ui-components";
import { SET_TEMPLATE_CHOOSER_VISIBLE, SET_TEMPLATE_LIST_SEARCH_PATTERN } from "~/mutations";
import { DEBOUNCED_SAVE_MESSAGE, FETCH_TEMPLATES_KEYS } from "~/actions";
import { MY_TEMPLATES } from "~/getters";
import TemplatesList from "./TemplateChooser/TemplatesList";
import { ComposerInitMixin } from "~/mixins";
const { isEditorContentEmpty, preserveFromOrDefault } = draftUtils;

export default {
    name: "TemplateChooser",
    components: { BmModal, BmModalHeader, BmFormInput, BmIconButton, TemplatesList },
    mixins: [ComposerInitMixin],
    data() {
        return {
            showMobileSearchInput: false,
            userPrefTextOnly: false, // FIXME: https://forge.bluemind.net/jira/browse/FEATWEBML-88,
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
            messages: state => state.conversations.messages,
            editorContent: state => state.messageCompose.editorContent
        }),
        ...mapState("root-app", ["identities"]),
        ...mapGetters("root-app", { identity: "DEFAULT_IDENTITY" })
    },
    methods: {
        ...mapMutations("mail", { SET_TEMPLATE_CHOOSER_VISIBLE, SET_TEMPLATE_LIST_SEARCH_PATTERN }),
        ...mapActions("mail", { DEBOUNCED_SAVE_MESSAGE, FETCH_TEMPLATES_KEYS }),
        reset() {
            this.SET_TEMPLATE_LIST_SEARCH_PATTERN("");
            this.loadTemplates();
        },
        async loadTemplates() {
            this.selected = 0;
            await this.FETCH_TEMPLATES_KEYS(this.MY_TEMPLATES);
        },
        async useTemplate() {
            this.SET_TEMPLATE_CHOOSER_VISIBLE(false);
            if (await this.canOverwriteContent()) {
                const template = this.messages[this.conversationByKey[this.selected].messages[0]];
                const target = this.messages[this.target];
                if (!target.subject.trim()) {
                    this.mergeSubject(target, template);
                }
                if (!(target.to.length || target.cc.length || target.bcc.length)) {
                    this.mergeRecipients(target, template);
                }
                target.from = preserveFromOrDefault(template.from, this.identities, this.identity);
                await this.mergeAttachments(target, template);
                await this.mergeBody(target, template);
                this.DEBOUNCED_SAVE_MESSAGE({
                    draft: target,
                    messageCompose: this.$store.state.mail.messageCompose,
                    files: target.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey])
                });
            }
        },
        async canOverwriteContent() {
            if (!isEditorContentEmpty(this.editorContent, this.userPrefTextOnly, this.identity?.signature)) {
                return this.$bvModal.msgBoxConfirm(this.$t("mail.compose.template_chooser.confirm_overwrite.message"), {
                    title: this.$tc("mail.compose.template_chooser.confirm_overwrite.title"),
                    okTitle: this.$t("mail.compose.template_chooser.confirm_overwrite.action"),
                    cancelTitle: this.$t("common.cancel")
                });
            }
            return true;
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.template-chooser.modal-dialog .modal-content {
    .modal-header {
        z-index: 1;
        border-bottom: none;

        .additional-content {
            padding: 0;
        }

        .desktop-search-input {
            @include until-lg {
                display: none !important;
            }
            $offset: $sp-6;
            & > .form-control {
                padding-left: $offset + map-get($icon-sizes, "md") + $sp-4 !important;
                padding-right: $offset !important;
            }
            .icon-wrapper,
            .reset-btn {
                left: $offset !important;
            }
        }

        @include until-lg {
            .with-mobile-search-input .bm-navbar-title {
                display: none;
            }
        }

        .mobile-search-input {
            @include from-lg {
                display: none !important;
            }
            background-color: $surface !important;
            margin: 0 $sp-4;
            flex: 1;
        }

        .search-button {
            margin: 0 $sp-3;
        }
    }

    .modal-body {
        padding: 0 !important;
        background-color: $backdrop;
        .templates-list {
            height: 100%;
        }
    }
}
</style>
