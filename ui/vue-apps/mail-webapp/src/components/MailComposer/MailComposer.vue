<template>
    <bm-form class="mail-composer m-lg-3 flex-grow-1 d-flex flex-column bg-white">
        <h3 class="d-none d-lg-flex text-nowrap text-truncate card-header px-2 py-1">
            {{ panelTitle }}
        </h3>
        <mail-composer-recipients
            ref="recipients"
            class="pl-3"
            :message="message"
            :is-reply-or-forward="!!messageCompose.collapsedContent"
        />
        <bm-form-input
            :value="message.subject.trim()"
            class="mail-composer-subject pl-3 d-flex align-items-center"
            :placeholder="$t('mail.new.subject.placeholder')"
            :aria-label="$t('mail.new.subject.aria')"
            type="text"
            @input="updateSubject"
            @keydown.enter.native.prevent
        />
        <hr class="mail-composer-splitter m-0" />
        <mail-composer-attachments
            :dragged-files-count="draggedFilesCount"
            :message="message"
            @files-count="draggedFilesCount = $event"
            @drop-files="addAttachments($event)"
        />
        <mail-composer-content
            ref="content"
            :user-pref-is-menu-bar-opened="userPrefIsMenuBarOpened"
            :message="message"
        />
        <mail-composer-footer
            :message="message"
            :user-pref-is-menu-bar-opened="userPrefIsMenuBarOpened"
            :signature="signature"
            :is-signature-inserted="isSignatureInserted"
            @toggle-text-format="userPrefIsMenuBarOpened = !userPrefIsMenuBarOpened"
            @toggle-signature="toggleSignature"
        />
    </bm-form>
</template>

<script>
import { mapGetters } from "vuex";

import { BmFormInput, BmForm } from "@bluemind/styleguide";

import { ComposerActionsMixin, ComposerMixin } from "~/mixins";
import MailComposerAttachments from "./MailComposerAttachments";
import MailComposerContent from "./MailComposerContent";
import MailComposerRecipients from "./MailComposerRecipients";
import MailComposerFooter from "./MailComposerFooter";

export default {
    name: "MailComposer",
    components: {
        BmFormInput,
        BmForm,
        MailComposerAttachments,
        MailComposerFooter,
        MailComposerContent,
        MailComposerRecipients
    },
    mixins: [ComposerActionsMixin, ComposerMixin],
    computed: {
        ...mapGetters("root-app", ["DEFAULT_IDENTITY"]),
        panelTitle() {
            return this.message.subject.trim() ? this.message.subject : this.$t("mail.main.new");
        }
    },
    mounted() {
        this.focus();
    },
    methods: {
        async focus() {
            await this.$nextTick();
            if (this.message.to.length > 0) {
                this.$refs.content.focus();
            } else {
                this.$refs.recipients.focus();
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-composer {
    word-break: break-word !important;

    .mail-composer-splitter {
        border-top-color: $alternate-light;
    }

    .mail-composer-subject input,
    .bm-contact-input input,
    textarea {
        border: none;
    }

    input:focus,
    textarea:focus {
        box-shadow: none;
    }

    .mail-composer-subject {
        min-height: 2.5rem;
    }

    .bm-file-drop-zone.attachments .bm-dropzone-active-content {
        min-height: 7em;
    }

    .bm-file-drop-zone.as-attachments.bm-dropzone-active,
    .bm-file-drop-zone.as-attachments.bm-dropzone-hover {
        background: url("~@bluemind/styleguide/assets/attachment.png") no-repeat center center;
    }
}
</style>
