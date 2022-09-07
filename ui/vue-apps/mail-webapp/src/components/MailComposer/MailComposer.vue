<template>
    <bm-form class="mail-composer m-lg-5 flex-grow-1 d-flex flex-column bg-surface" @keypress.ctrl.enter="send">
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
        <mail-composer-attachments
            class="m-4"
            :dragged-files-count="draggedFilesCount"
            :message="message"
            @files-count="draggedFilesCount = $event"
            @drop-files="$execute('add-attachments', { files: $event, message, maxSize })"
        />
        <mail-composer-content ref="content" :message="message" :is-signature-inserted.sync="isSignatureInserted" />
        <mail-composer-footer
            :message="message"
            :is-signature-inserted="isSignatureInserted"
            @toggle-signature="toggleSignature"
        />
        <template-chooser />
    </bm-form>
</template>

<script>
import { BmIconButton, BmFormInput, BmForm } from "@bluemind/styleguide";
import { ComposerActionsMixin, ComposerMixin } from "~/mixins";
import { AddAttachmentsCommand } from "~/commands";
import MessagePathParam from "~/router/MessagePathParam";
import MailComposerAttachments from "./MailComposerAttachments";
import MailComposerContent from "./MailComposerContent";
import MailComposerRecipients from "./MailComposerRecipients";
import MailComposerFooter from "./MailComposerFooter";
import MailComposerSender from "./MailComposerSender";
import TemplateChooser from "~/components/TemplateChooser";
import MailOpenInPopup from "../MailOpenInPopup";

export default {
    name: "MailComposer",
    components: {
        BmIconButton,
        BmFormInput,
        BmForm,
        MailComposerAttachments,
        MailComposerFooter,
        MailComposerContent,
        MailComposerSender,
        MailComposerRecipients,
        MailOpenInPopup,
        TemplateChooser
    },
    mixins: [AddAttachmentsCommand, ComposerActionsMixin, ComposerMixin],
    computed: {
        panelTitle() {
            return this.message.subject.trim() ? this.message.subject : this.$t("mail.main.new");
        },
        messagepath() {
            return MessagePathParam.build("", this.message);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-composer {
    word-break: break-word !important;

    .bm-contact-input input,
    textarea {
        border: none;
    }

    input:focus,
    textarea:focus {
        box-shadow: none;
    }

    .bm-file-drop-zone.attachments .bm-dropzone-active-content {
        min-height: 7em;
    }

    .bm-file-drop-zone.as-attachments.bm-dropzone-active,
    .bm-file-drop-zone.as-attachments.bm-dropzone-hover {
        background: url("~@bluemind/styleguide/assets/attachment.png") no-repeat center center;
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
