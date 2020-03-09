<template>
    <bm-col cols="12" md="8" lg="7" xl="7" class=" d-none d-md-flex px-0 h-100 flex-column overflow-auto">
        <mail-multiple-selection-actions v-if="selectedMessageKeys.length > 1" />
        <mail-message-starter v-else />
    </bm-col>
</template>

<script>
import MailMessageStarter from "./MailMessageStarter";
import MailMultipleSelectionActions from "./MailMultipleSelectionActions";
import { mapState } from "vuex";
import { BmCol } from "@bluemind/styleguide";

export default {
    name: "MailActionsPanel",
    components: {
        BmCol,
        MailMessageStarter,
        MailMultipleSelectionActions
    },
    computed: {
        ...mapState("mail-webapp", ["selectedMessageKeys"])
    }
};
</script>
