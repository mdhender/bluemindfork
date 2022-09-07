<template>
    <div class="mail-composer-recipients pr-1">
        <bm-row class="align-items-center">
            <bm-col :cols="displayedRecipientFields == recipientModes.TO ? 11 : 12">
                <bm-contact-input
                    ref="to"
                    variant="underline"
                    :contacts.sync="to"
                    :autocomplete-results="autocompleteResultsTo"
                    :validate-address-fn="validateDnAndAddress"
                    @search="searchedPattern => onSearch('to', searchedPattern)"
                    @update:contacts="updateTo"
                    @expand="expandContact(to, $event, updateTo)"
                >
                    {{ $t("common.to") }}
                </bm-contact-input>
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
                <bm-contact-input
                    variant="underline"
                    :contacts.sync="cc"
                    :autocomplete-results="autocompleteResultsCc"
                    :validate-address-fn="validateDnAndAddress"
                    class="w-100"
                    @search="searchedPattern => onSearch('cc', searchedPattern)"
                    @update:contacts="updateCc"
                    @expand="expandContact(cc, $event, updateCc)"
                >
                    {{ $t("common.cc") }}
                </bm-contact-input>
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
                <bm-contact-input
                    variant="underline"
                    :contacts.sync="bcc"
                    :autocomplete-results="autocompleteResultsBcc"
                    :validate-address-fn="validateDnAndAddress"
                    @search="searchedPattern => onSearch('bcc', searchedPattern)"
                    @update:contacts="updateBcc"
                    @expand="expandContact(bcc, $event, updateBcc)"
                >
                    {{ $t("common.bcc") }}
                </bm-contact-input>
            </bm-col>
        </bm-row>
    </div>
</template>

<script>
import { BmButton, BmIconButton, BmCol, BmContactInput, BmRow } from "@bluemind/styleguide";
import { ComposerActionsMixin, EditRecipientsMixin } from "~/mixins";

export default {
    name: "MailComposerRecipients",
    components: {
        BmButton,
        BmIconButton,
        BmCol,
        BmContactInput,
        BmRow
    },
    mixins: [ComposerActionsMixin, EditRecipientsMixin]
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

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
}
</style>
