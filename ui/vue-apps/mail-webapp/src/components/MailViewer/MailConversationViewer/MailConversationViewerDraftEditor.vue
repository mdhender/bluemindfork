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
                    :identity-id="identityId"
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
                <mail-composer-recipients ref="recipients" :message="message">
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
        </template>
        <template slot="subhead">
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
                        :attachments="attachments"
                        :dragged-files-count="draggedFilesCount"
                        :message="message"
                        @files-count="draggedFilesCount = $event"
                        @drop-files="$execute('add-attachments', { files: $event, message })"
                    />
                </div>
            </div>
        </template>
        <template slot="content">
            <div v-show="!showConversationDropzone" class="w-100">
                <bm-file-drop-zone
                    :should-activate-fn="shouldActivate"
                    @files-count="draggedFilesCount = $event"
                    @drop-files="$execute('add-attachments', { files: $event, message })"
                >
                    <template #dropZone>
                        <mail-composer-attach-zone :text="$tc('mail.new.attachments.drop.zone', draggedFilesCount)" />
                    </template>
                    <mail-composer-content ref="content" :message="message" />
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
import { BmIconButton, BmDropzone, BmFileDropZone, KeyNavGroup } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import { ComposerActionsMixin, EditRecipientsMixin, FileDropzoneMixin } from "~/mixins";
import { setFrom } from "~/composables/composer/ComposerFrom";
import { AddAttachmentsCommand } from "~/commands";
import { SIGNATURE_INSERTED, SIGNATURE } from "~/getters";
import { REMOVE_MESSAGES, SET_MESSAGE_COMPOSING } from "~/mutations";
import { TOGGLE_SIGNATURE } from "~/actions";
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
import { computed, ref, watchEffect } from "vue";

const { MessageStatus, computeParts } = messageUtils;

export default {
    name: "MailConversationViewerDraftEditor",
    components: {
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
            identityId,
            isSenderShown,
            isDeliveryStatusRequested,
            isDispositionNotificationRequested,
            toggleDeliveryStatus,
            toggleDispositionNotification,
            checkAndRepairFrom
        } = useComposer(
            computed(() => props.message),
            content
        );

        return {
            draggedFilesCount,
            identityId,
            isSenderShown,
            isDeliveryStatusRequested,
            isDispositionNotificationRequested,
            toggleDeliveryStatus,
            toggleDispositionNotification,
            checkAndRepairFrom,
            setFrom,
            content
        };
    },
    data() {
        return {
            showConversationDropzone: false,
            computedParts: {}
        };
    },

    computed: {
        ...mapState("mail", ["folders"]),
        route() {
            return this.$router.relative({
                name: "mail:message",
                params: { messagepath: MessagePathParam.build("", this.message) }
            });
        },
        attachments() {
            return this.computedParts?.attachments || [];
        },
        isSignatureInserted() {
            return Boolean(this.$store.getters[`mail/${SIGNATURE}`]);
        }
    },
    watch: {
        "message.structure": {
            handler() {
                this.computedParts = computeParts(this.message.structure);
            },
            immediate: true
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
        toggleSignature() {
            this.$store.dispatch(`mail/${TOGGLE_SIGNATURE}`);
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
