<template>
    <bm-form class="mail-composer flex-grow-1 d-flex flex-column bg-surface" @keypress.ctrl.enter="send">
        <mail-open-in-popup
            v-slot="action"
            :href="{ name: 'mail:popup:message', params: { messagepath } }"
            :next="$router.relative('mail:home')"
        >
            <div class="d-none d-lg-flex card-header regular px-4 py-0 align-items-center">
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
            :message="message"
            @update="identity => setFrom(identity, message)"
            @check-and-repair="checkAndRepairFrom"
        />
        <mail-composer-recipients
            ref="recipients"
            class="px-4"
            :message="message"
            :is-reply-or-forward="!!messageCompose.collapsedContent"
        />
        <bm-form-input
            :value="message.subject.trim()"
            variant="underline"
            class="mx-4"
            :placeholder="$t('mail.new.subject.placeholder')"
            :aria-label="$t('mail.new.subject.aria')"
            type="text"
            @input="updateSubject"
            @keypress.enter.prevent
        />
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
        <div v-show="!showConversationDropzone">
            <bm-file-drop-zone
                class="h-100 my-2"
                :should-activate-fn="shouldActivate"
                @files-count="draggedFilesCount = $event"
                @drop-files="$execute('add-attachments', { files: $event, message, maxSize })"
            >
                <template #dropZone>
                    <mail-composer-attach-zone :text="$tc('mail.new.attachments.drop.zone', draggedFilesCount)" />
                </template>
                <mail-composer-attachments
                    class="m-4"
                    :dragged-files-count="draggedFilesCount"
                    :message="message"
                    @files-count="draggedFilesCount = $event"
                    @drop-files="$execute('add-attachments', { files: $event, message, maxSize })"
                />
                <mail-composer-content
                    ref="content"
                    class="m-4"
                    :message="message"
                    :is-signature-inserted.sync="isSignatureInserted"
                />
                <template-chooser />
            </bm-file-drop-zone>
        </div>

        <mail-composer-footer
            :message="message"
            :is-signature-inserted="isSignatureInserted"
            @toggle-signature="toggleSignature"
        />
    </bm-form>
</template>

<script>
import { mapGetters } from "vuex";
import { BmDropzone, BmFileDropZone, BmIconButton, BmFormInput, BmForm } from "@bluemind/styleguide";
import { ComposerActionsMixin, ComposerMixin, FileDropzoneMixin } from "~/mixins";
import { MY_TEMPLATES } from "~/getters";
import { AddAttachmentsCommand } from "~/commands";
import MessagePathParam from "~/router/MessagePathParam";
import MailComposerAttachments from "./MailComposerAttachments";
import MailComposerContent from "./MailComposerContent";
import MailComposerRecipients from "./MailComposerRecipients";
import MailComposerFooter from "./MailComposerFooter";
import MailComposerSender from "./MailComposerSender";
import TemplateChooser from "~/components/TemplateChooser";
import MailOpenInPopup from "../MailOpenInPopup";
import MailComposerAttachZone from "./MailComposerAttachZone";

export default {
    name: "MailComposer",
    components: {
        BmDropzone,
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
        TemplateChooser
    },
    mixins: [AddAttachmentsCommand, ComposerActionsMixin, ComposerMixin, FileDropzoneMixin],
    data() {
        return { showConversationDropzone: false };
    },
    computed: {
        ...mapGetters("mail", { MY_TEMPLATES }),
        panelTitle() {
            return this.message.subject.trim()
                ? this.message.subject
                : this.message.folderRef.key === this.MY_TEMPLATES.key
                ? this.$t("mail.actions.new_template")
                : this.$t("mail.main.new");
        },
        messagepath() {
            return MessagePathParam.build("", this.message);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/mixins/_responsiveness";
@import "~@bluemind/styleguide/css/_variables";

.mail-composer {
    @include from-lg {
        margin: $sp-5;
        margin-right: 0;
    }

    word-break: break-word !important;

    .bm-contact-input input,
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
