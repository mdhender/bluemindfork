<template>
    <div class="mail-composer-recipients">
        <bm-row class="align-items-center">
            <bm-col cols="11">
                <bm-contact-input
                    ref="to"
                    :contacts="message.to"
                    :autocomplete-results="autocompleteResultsTo"
                    :validate-address-fn="validateDnAndAddress"
                    @search="searchedPattern => onSearch('to', searchedPattern)"
                    @update:contacts="updateTo"
                    @expand="expandContact(message.to, $event)"
                >
                    {{ $t("common.to") }}
                </bm-contact-input>
            </bm-col>
            <bm-col cols="1" class="text-center">
                <bm-button
                    v-if="displayedRecipientFields == recipientModes.TO"
                    variant="simple-neutral"
                    @click="displayedRecipientFields = recipientModes.TO | recipientModes.CC | recipientModes.BCC"
                >
                    <bm-icon icon="chevron" />
                </bm-button>
            </bm-col>
        </bm-row>
        <hr class="m-0" />

        <div v-if="displayedRecipientFields > recipientModes.TO" class="d-flex">
            <div class="d-flex flex-grow-1">
                <bm-contact-input
                    :contacts="message.cc"
                    :autocomplete-results="autocompleteResultsCc"
                    :validate-address-fn="validateDnAndAddress"
                    class="w-100"
                    @search="searchedPattern => onSearch('cc', searchedPattern)"
                    @update:contacts="updateCc"
                    @expand="expandContact(message.cc, $event)"
                >
                    {{ $t("common.cc") }}
                </bm-contact-input>
            </div>
            <bm-button
                v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC)"
                variant="simple-neutral"
                class="my-2 mr-1"
                @click="displayedRecipientFields = recipientModes.TO | recipientModes.CC | recipientModes.BCC"
            >
                {{ $t("common.bcc") }}
            </bm-button>
        </div>
        <hr v-if="displayedRecipientFields > recipientModes.TO" class="m-0" />

        <bm-contact-input
            v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC | recipientModes.BCC)"
            :contacts="message.bcc"
            :autocomplete-results="autocompleteResultsBcc"
            :validate-address-fn="validateDnAndAddress"
            @search="searchedPattern => onSearch('bcc', searchedPattern)"
            @update:contacts="updateBcc"
            @expand="expandContact(message.bcc, $event)"
        >
            {{ $t("common.bcc") }}
        </bm-contact-input>
        <hr
            v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC | recipientModes.BCC)"
            class="m-0"
        />
    </div>
</template>

<script>
import { BmButton, BmCol, BmContactInput, BmIcon, BmRow } from "@bluemind/styleguide";
import { ComposerActionsMixin, EditRecipientsMixin } from "~/mixins";

export default {
    name: "MailComposerRecipients",
    components: {
        BmButton,
        BmCol,
        BmContactInput,
        BmIcon,
        BmRow
    },
    mixins: [ComposerActionsMixin, EditRecipientsMixin]
};
</script>
