<template>
    <bm-form class="mail-composer flex-grow-1 d-flex flex-column bg-surface" @keypress.ctrl.enter="sendMessage">
        <bm-extension id="webapp.mail" path="composer.header" :message="message" />
        <mail-open-in-popup
            v-slot="action"
            :href="{ name: 'mail:popup:message', params: { messagepath } }"
            :next="$router.relative('mail:home')"
        >
            <div class="desktop-only d-flex card-header regular px-4 py-0 align-items-center">
                <span class="text-nowrap text-truncate">{{ panelTitle }}</span>
                <bm-icon-button
                    :title="action.label"
                    variant="compact-on-fill-primary"
                    size="sm"
                    class="ml-auto"
                    :icon="action.icon"
                    @click="saveAsap().then(action.execute)"
                />
            </div>
        </mail-open-in-popup>
        <mail-composer-sender
            v-if="isSenderShown"
            class="mx-4"
            label-class="ml-3 bold"
            :identity-id="identityId"
            @update="
                identity => {
                    setFrom(identity, message);
                    debouncedSave();
                }
            "
            @check-and-repair="checkAndRepairFrom"
        />
        <mail-composer-recipients ref="recipients" class="px-4" :message="message" />
        <bm-form-input
            :value="subject"
            variant="underline"
            class="mx-4"
            :placeholder="$t('mail.new.subject.placeholder')"
            :aria-label="$t('mail.new.subject.aria')"
            type="text"
            @input="updateSubject"
            @keypress.enter.prevent
        />
        <composer-top-frame :message="message" :attachments="attachments" />
        <bm-dropzone
            v-show="showConversationDropzone"
            class="h-100 my-2"
            :accept="['conversation']"
            :value="message"
            @dropactivate="showConversationDropzone = true"
            @dropdeactivate="showConversationDropzone = false"
        >
            <div class="bm-dropzone-show-dropzone justify-content-center align-items-center d-flex flex-column h-100">
                <mail-composer-attach-zone :text="$tc('mail.new.attachments.eml.drop.zone')" />
            </div>
        </bm-dropzone>
        <div v-show="!showConversationDropzone" class="h-100">
            <bm-file-drop-zone
                class="h-100 my-2"
                :should-activate-fn="shouldActivate"
                @files-count="draggedFilesCount = $event"
                @drop-files="execAddAttachments({ files: $event, message })"
            >
                <template #dropZone>
                    <mail-composer-attach-zone :text="$tc('mail.new.attachments.drop.zone', draggedFilesCount)" />
                </template>
                <mail-composer-attachments
                    class="mx-4"
                    :dragged-files-count="draggedFilesCount"
                    :message="message"
                    :attachments="attachments"
                    @files-count="draggedFilesCount = $event"
                    @drop-files="execAddAttachments({ files: $event, message })"
                />
                <mail-composer-content ref="content" :message="message" />
                <template-chooser />
            </bm-file-drop-zone>
        </div>

        <mail-composer-footer
            :message="message"
            :is-signature-inserted="signatureInserted"
            :is-delivery-status-requested.sync="isDeliveryStatusRequested"
            :is-disposition-notification-requested.sync="isDispositionNotificationRequested"
            @toggle-signature="toggleSignature"
            @toggle-delivery-status="toggleDeliveryStatus"
            @toggle-disposition-notification="toggleDispositionNotification"
        />
    </bm-form>
</template>

<script>
import { mapGetters } from "vuex";
import { BmExtension } from "@bluemind/extensions.vue";
import { BmDropzone, BmFileDropZone, BmIconButton, BmFormInput, BmForm } from "@bluemind/ui-components";
import { ComposerActionsMixin, FileDropzoneMixin } from "~/mixins";
import { useComposer } from "~/composables/composer/Composer";
import { setFrom } from "~/composables/composer/ComposerFrom";
import { MY_TEMPLATES, SIGNATURE } from "~/getters";
import { useAddAttachmentsCommand } from "~/commands";
import MessagePathParam from "~/router/MessagePathParam";
import MailComposerAttachments from "./MailComposerAttachments";
import MailComposerContent from "./MailComposerContent";
import MailComposerRecipients from "./MailComposerRecipients";
import MailComposerFooter from "./MailComposerFooter";
import MailComposerSender from "./MailComposerSender";
import TemplateChooser from "~/components/TemplateChooser";
import MailOpenInPopup from "../MailOpenInPopup";
import MailComposerAttachZone from "./MailComposerAttachZone";

import { computed, ref, watchEffect } from "vue";
import ComposerTopFrame from "./ComposerTopFrame/ComposerTopFrame";
import { messageUtils } from "@bluemind/mail";
const { computeParts } = messageUtils;

export default {
    name: "MailComposer",
    components: {
        BmDropzone,
        BmExtension,
        BmIconButton,
        BmFileDropZone,
        BmFormInput,
        BmForm,
        MailComposerAttachments,
        MailComposerAttachZone,
        MailComposerFooter,
        MailComposerContent,
        MailComposerSender,
        MailComposerRecipients,
        MailOpenInPopup,
        TemplateChooser,
        ComposerTopFrame
    },
    mixins: [ComposerActionsMixin, FileDropzoneMixin],
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    setup(props) {
        const content = ref(); // DOM ref="content"
        const { execAddAttachments } = useAddAttachmentsCommand();
        const {
            checkAndRepairFrom,
            draggedFilesCount,
            identityId,
            isDeliveryStatusRequested,
            isDispositionNotificationRequested,
            isSenderShown,
            messageCompose,
            toggleDeliveryStatus,
            toggleDispositionNotification,
            toggleSignature
        } = useComposer(
            computed(() => props.message),
            content
        );
        return {
            checkAndRepairFrom,
            content,
            draggedFilesCount,
            execAddAttachments,
            identityId,
            isDeliveryStatusRequested,
            isDispositionNotificationRequested,
            isSenderShown,
            messageCompose,
            setFrom,
            toggleDeliveryStatus,
            toggleDispositionNotification,
            toggleSignature
        };
    },
    data() {
        return { showConversationDropzone: false, computedParts: {} };
    },
    computed: {
        ...mapGetters("mail", { MY_TEMPLATES }),
        panelTitle() {
            return (
                this.subject ||
                (this.message.folderRef.key === this.MY_TEMPLATES.key
                    ? this.$t("mail.actions.new_template")
                    : this.$t("mail.main.new"))
            );
        },
        attachments() {
            return this.computedParts?.attachments || [];
        },
        messagepath() {
            return MessagePathParam.build("", this.message);
        },
        subject() {
            return this.message.subject?.trim() || "";
        },
        signatureInserted() {
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
    methods: {
        sendMessage() {
            if (this.isSendingDisabled) {
                return;
            }
            this.send();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-composer {
    @include from-lg {
        margin: $sp-5;
        margin-right: 0;
    }

    word-break: break-word !important;

    .contact-input input,
    textarea {
        border: none;
    }

    input:focus,
    textarea:focus {
        box-shadow: none;
    }

    .files-header {
        display: flex;
        flex: 1 1 auto;
        .progress {
            display: flex;
            flex: 1 1 auto;
        }
    }

    .mail-composer-footer {
        position: sticky;
        bottom: 0;
        z-index: $zindex-sticky;
    }
}
</style>
