<template>
    <div>
        <i18n path="alert.mail.draft_exceeds_max_message_size.reason" class="font-weight-bold">
            <template #size>{{ maxMessageSize }}</template>
        </i18n>
        <div>{{ $t("alert.mail.draft_exceeds_max_message_size.advice") }}</div>
    </div>
</template>

<script>
import { AlertMixin } from "@bluemind/alert.store";
import { computeUnit } from "@bluemind/file-utils";

export default {
    name: "DraftExceedsMaxMessageSizeAlert",
    mixins: [AlertMixin],
    computed: {
        maxMessageSize() {
            return computeUnit(this.$store.state.mail.messageCompose.maxMessageSize, this.$i18n);
        }
    }
};
</script>
