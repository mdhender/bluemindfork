<template>
    <div class="mail-composer-recipients pr-1">
        <bm-row class="align-items-center">
            <bm-col :cols="displayedRecipientFields == recipientModes.TO ? 11 : 12">
                <bm-contact-input
                    ref="to"
                    :contacts.sync="to"
                    :autocomplete-results="autocompleteResultsTo"
                    :validate-address-fn="validateDnAndAddress"
                    @search="searchedPattern => onSearch('to', searchedPattern)"
                    @update:contacts="updateTo"
                    @expand="expandContact(to, $event, updateTo)"
                >
                    <span class="text-nowrap">{{ $t("common.to") }}</span>
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
        <hr class="m-0" />
        <bm-row v-if="displayedRecipientFields > recipientModes.TO" class="align-items-center">
            <bm-col :cols="displayedRecipientFields == (recipientModes.TO | recipientModes.CC) ? 11 : 12">
                <bm-contact-input
                    :contacts.sync="cc"
                    :autocomplete-results="autocompleteResultsCc"
                    :validate-address-fn="validateDnAndAddress"
                    class="w-100"
                    @search="searchedPattern => onSearch('cc', searchedPattern)"
                    @update:contacts="updateCc"
                    @expand="expandContact(cc, $event, updateCc)"
                >
                    <span class="text-nowrap">{{ $t("common.cc") }}</span>
                </bm-contact-input>
            </bm-col>
            <bm-col
                v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC)"
                cols="1"
                class="text-center"
            >
                <bm-button
                    variant="text"
                    class="my-2 mr-1 px-4 text-nowrap"
                    @click="displayedRecipientFields = recipientModes.TO | recipientModes.CC | recipientModes.BCC"
                    >{{ $t("common.bcc") }}
                </bm-button>
            </bm-col>
        </bm-row>
        <hr v-if="displayedRecipientFields > recipientModes.TO" class="m-0" />
        <bm-row
            v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC | recipientModes.BCC)"
            class="align-items-center"
        >
            <bm-col>
                <bm-contact-input
                    :contacts.sync="bcc"
                    :autocomplete-results="autocompleteResultsBcc"
                    :validate-address-fn="validateDnAndAddress"
                    @search="searchedPattern => onSearch('bcc', searchedPattern)"
                    @update:contacts="updateBcc"
                    @expand="expandContact(bcc, $event, updateBcc)"
                >
                    <span class="text-nowrap">{{ $t("common.bcc") }}</span>
                </bm-contact-input>
            </bm-col>
        </bm-row>
        <hr
            v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC | recipientModes.BCC)"
            class="m-0"
        />
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
.mail-composer-recipients {
    .bm-contact-input-label {
        min-width: 2rem;
    }
}
</style>
