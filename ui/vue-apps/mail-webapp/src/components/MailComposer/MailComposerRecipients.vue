<template>
    <div class="mail-composer-recipients pr-1">
        <bm-row class="align-items-center">
            <bm-col :cols="displayedRecipientFields == recipientModes.TO ? 11 : 12">
                <mail-composer-recipient :message="message" recipient-type="to" />
            </bm-col>
            <bm-col v-if="displayedRecipientFields == recipientModes.TO" cols="1" class="text-center">
                <bm-icon-button
                    variant="compact"
                    icon="chevron"
                    @click="displayedRecipientFields = recipientModes.TO | recipientModes.CC | recipientModes.BCC"
                />
            </bm-col>
        </bm-row>
        <div v-if="displayedRecipientFields > recipientModes.TO" class="d-flex align-items-center">
            <div class="cc-contact-input">
                <mail-composer-recipient :message="message" recipient-type="cc" />
                <bm-button
                    v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC)"
                    variant="text"
                    class="bcc-button text-nowrap"
                    @click="displayedRecipientFields = recipientModes.TO | recipientModes.CC | recipientModes.BCC"
                >
                    {{ $t("common.bcc") }}
                </bm-button>
            </div>
        </div>
        <bm-row
            v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC | recipientModes.BCC)"
            class="align-items-center"
        >
            <bm-col>
                <mail-composer-recipient :message="message" recipient-type="bcc" />
            </bm-col>
        </bm-row>
    </div>
</template>

<script>
import { BmButton, BmIconButton, BmCol, BmRow } from "@bluemind/ui-components";
import { ComposerActionsMixin, EditRecipientsMixin } from "~/mixins";
import MailComposerRecipient from "./MailComposerRecipient";

export default {
    name: "MailComposerRecipients",
    components: {
        BmButton,
        BmIconButton,
        BmCol,
        BmRow,
        MailComposerRecipient
    },
    mixins: [ComposerActionsMixin, EditRecipientsMixin]
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.mail-composer-recipients {
    .bm-contact-input-label {
        flex: none;
    }

    .cc-contact-input {
        $bcc-button-width: base-px-to-rem(24);

        flex: 1;
        min-width: 0;
        position: relative;

        .bm-contact-input {
            padding-right: $bcc-button-width;
        }

        .bcc-button {
            position: absolute;
            width: $bcc-button-width;
            right: 0;
            bottom: base-px-to-rem(3);
        }
    }

    .bm-contact-input {
        &.expanded-search .delete-autocomplete {
            visibility: hidden;
        }
        .bm-contact-input-autocomplete-extra {
            width: 18rem;
        }
    }
}
</style>
