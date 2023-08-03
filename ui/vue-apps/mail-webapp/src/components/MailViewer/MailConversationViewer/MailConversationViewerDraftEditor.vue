<template>
    <mail-conversation-viewer-item
        class="mail-conversation-viewer-draft-editor draft"
        v-bind="$props"
        :is-draft="true"
        :sticky-bottom="true"
        v-on="$listeners"
    >
        <template slot="head">
            <div class="d-flex flex-column flex-fill">
                <mail-composer-sender
                    v-if="isSenderShown"
                    label-class="font-weight-bold text-neutral"
                    :message="message"
                    @update="
                        identity => {
                            setFrom(identity, message);
                            debouncedSave();
                        }
                    "
                    @check-and-repair="checkAndRepairFrom"
                />
                <mail-conversation-viewer-vertical-line
                    v-if="isSenderShown"
                    :index="index"
                    :max-index="maxIndex"
                    after-avatar
                />
                <div class="to-contact-input">
                    <mail-composer-recipients ref="recipients" class="px-4" :message="message">
                        <mail-open-in-popup-with-shift v-slot="action" :href="route" :next="consult">
                            <bm-icon-button
                                variant="compact"
                                class="expand-button"
                                :title="action.label($t('mail.actions.extend'))"
                                :disabled="anyAttachmentInError"
                                :icon="action.icon('extend')"
                                @click="saveAsap().then(() => action.execute(() => $router.navigate(route), $event))"
                            />
                        </mail-open-in-popup-with-shift>
                    </mail-composer-recipients>
                </div>
            </div>
        </template>
        <template slot="subhead">
            <template v-if="showCc">
                <div class="d-flex conversation-viewer-row flex-nowrap">
                    <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                    <div class="cc-contact-input">
                        <mail-composer-recipient ref="ccField" :message="message" recipient-type="cc">
                            <div class="end-buttons">
                                <bm-button
                                    v-if="!showBcc"
                                    v-key-nav-group:recipient-button
                                    variant="text"
                                    tabindex="-1"
                                    @click="showAndFocusRecipientField('bcc')"
                                    @keydown.tab.prevent="focusRecipientField('to')"
                                >
                                    {{ $t("common.bcc") }}
                                </bm-button>
                            </div>
                        </mail-composer-recipient>
                    </div>
                </div>
            </template>
            <template v-if="showBcc">
                <div class="d-flex conversation-viewer-row flex-nowrap">
                    <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                    <mail-composer-recipient ref="bccField" :message="message" recipient-type="bcc" />
                </div>
            </template>
            <div class="d-flex conversation-viewer-row flex-nowrap">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <div class="w-100">
                    <bm-dropzone
                        v-show="showConversationDropzone"
                        class="my-2"
                        :accept="['conversation']"
                        :value="message"
                        @dropactivate="showConversationDropzone = true"
                        @dropdeactivate="showConversationDropzone = false"
                    >
                        <div
                            class="bm-dropzone-show-dropzone justify-content-center align-items-center d-flex flex-column"
                        >
                            <mail-composer-attach-zone :text="$tc('mail.new.attachments.eml.drop.zone')" />
                        </div>
                    </bm-dropzone>
                    <mail-composer-attachments
                        v-if="!showConversationDropzone"
                        class="my-4"
                        :dragged-files-count="draggedFilesCount"
                        :message="message"
                        @files-count="draggedFilesCount = $event"
                        @drop-files="$execute('add-attachments', { files: $event, message, maxSize })"
                    />
                </div>
            </div>
        </template>
        <template slot="content">
            <div v-show="!showConversationDropzone" class="w-100">
                <bm-file-drop-zone
                    class="my-2"
                    :should-activate-fn="shouldActivate"
                    @files-count="draggedFilesCount = $event"
                    @drop-files="$execute('add-attachments', { files: $event, message, maxSize })"
                >
                    <template #dropZone>
                        <mail-composer-attach-zone :text="$tc('mail.new.attachments.drop.zone', draggedFilesCount)" />
                    </template>
                    <mail-composer-content
                        ref="content"
                        :message="message"
                        :is-signature-inserted.sync="isSignatureInserted"
                    />
                </bm-file-drop-zone>
            </div>
        </template>
        <template slot="bottom">
            <div class="flex-fill">
                <div class="row">
                    <mail-composer-footer
                        class="col"
                        :message="message"
                        :is-signature-inserted="isSignatureInserted"
                        :is-delivery-status-requested.sync="isDeliveryStatusRequested"
                        :is-disposition-notification-requested.sync="isDispositionNotificationRequested"
                        @toggle-signature="toggleSignature"
                        @toggle-delivery-status="toggleDeliveryStatus"
                        @toggle-disposition-notification="toggleDispositionNotification"
                    />
                </div>
            </div>
        </template>
    </mail-conversation-viewer-item>
</template>

<script>
import capitalize from "lodash.capitalize";
import { mapMutations, mapState } from "vuex";
import { BmIconButton, BmButton, BmDropzone, BmFileDropZone, KeyNavGroup } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import { ComposerActionsMixin, EditRecipientsMixin, FileDropzoneMixin } from "~/mixins";
import { setFrom } from "~/composables/composer/ComposerFrom";
import { AddAttachmentsCommand } from "~/commands";
import { REMOVE_MESSAGES, SET_MESSAGE_COMPOSING } from "~/mutations";
import { useComposer } from "~/composables/composer/Composer";
import MailComposerAttachments from "../../MailComposer/MailComposerAttachments";
import MailComposerAttachZone from "../../MailComposer/MailComposerAttachZone";
import MailComposerContent from "../../MailComposer/MailComposerContent";
import MailComposerFooter from "../../MailComposer/MailComposerFooter";
import MailComposerRecipients from "../../MailComposer/MailComposerRecipients";
import MailComposerSender from "../../MailComposer/MailComposerSender";
import MailConversationViewerItem from "./MailConversationViewerItem";
import MailConversationViewerItemMixin from "./MailConversationViewerItemMixin";
import MailConversationViewerVerticalLine from "./MailConversationViewerVerticalLine";
import MailOpenInPopupWithShift from "../../MailOpenInPopupWithShift";
import MessagePathParam from "~/router/MessagePathParam";
import { computed, ref } from "vue";

const { MessageStatus } = messageUtils;

export default {
    name: "MailConversationViewerDraftEditor",
    components: {
        BmButton,
        BmDropzone,
        BmFileDropZone,
        BmIconButton,
        MailComposerAttachments,
        MailComposerAttachZone,
        MailComposerContent,
        MailComposerFooter,
        MailComposerRecipients,
        MailComposerSender,
        MailConversationViewerItem,
        MailConversationViewerVerticalLine,
        MailOpenInPopupWithShift
    },
    directives: { KeyNavGroup },
    mixins: [
        AddAttachmentsCommand,
        ComposerActionsMixin,
        EditRecipientsMixin,
        FileDropzoneMixin,
        MailConversationViewerItemMixin
    ],
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    setup(props) {
        const content = ref(); // DOM ref="content"
        const {
            draggedFilesCount,
            isSignatureInserted,
            isSenderShown,
            isDeliveryStatusRequested,
            isDispositionNotificationRequested,
            toggleSignature,
            toggleDeliveryStatus,
            toggleDispositionNotification,
            checkAndRepairFrom
        } = useComposer(
            computed(() => props.message),
            content
        );
        return {
            draggedFilesCount,
            isSignatureInserted,
            isSenderShown,
            isDeliveryStatusRequested,
            isDispositionNotificationRequested,
            toggleSignature,
            toggleDeliveryStatus,
            toggleDispositionNotification,
            checkAndRepairFrom,
            setFrom
        };
    },
    data() {
        return { showConversationDropzone: false };
    },

    computed: {
        ...mapState("mail", ["folders"]),
        route() {
            return this.$router.relative({
                name: "mail:message",
                params: { messagepath: MessagePathParam.build("", this.message) }
            });
        }
    },
    mounted() {
        this.$nextTick(() => this.$el.scrollIntoView());
    },
    destroyed() {
        // clean up unsaved new message from conversation
        if (this.message.status === MessageStatus.NEW) {
            this.REMOVE_MESSAGES({ messages: [this.message] });
        }
    },
    methods: {
        ...mapMutations("mail", { REMOVE_MESSAGES }),
        consult() {
            this.$store.commit(`mail/${SET_MESSAGE_COMPOSING}`, { messageKey: this.message.key, composing: false });
        },
        async showAndFocusRecipientField(recipientType) {
            this[`show${capitalize(recipientType)}`] = true;
            await this.$nextTick();
            this.focusRecipientField(recipientType);
        },
        focusRecipientField(recipientType) {
            this.$refs[`${recipientType}Field`]?.$el.querySelector("input").focus();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-conversation-viewer-draft-editor {
    padding-right: $sp-6;

    .to-contact-input,
    .cc-contact-input {
        flex: 1;
        min-width: 0;
    }

    .end-buttons {
        display: flex;
        gap: $sp-4;
        align-items: flex-start;
        flex: none;
        order: 12;
    }

    .mail-composer-content .bm-rich-editor {
        padding-left: 0;
        padding-right: 0;
    }

    .mail-composer-footer {
        border-top: unset !important;
    }
    .bm-rich-editor .ProseMirror {
        padding: $sp-2 0 $sp-2 $sp-3;
    }

    .contact-input {
        &.expanded-search .delete-autocomplete {
            visibility: hidden;
        }
        .suggestions {
            min-width: base-px-to-rem(288);
        }
    }
}
</style>
