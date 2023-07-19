<template>
    <div>
        <p>{{ $t("preferences.mail.advanced.recipient_autocomplete") }}</p>
        <bm-label-icon v-if="deleted" class="text-success" icon="check-circle">
            {{ $t("preferences.mail.advanced.recipient_autocomplete.confirm_deletion.success") }}
        </bm-label-icon>
        <bm-button
            v-else
            variant="outline-danger"
            icon="broom"
            size="lg"
            :disabled="disabled"
            @click="resetAddressWeights"
        >
            {{ $t("common.action.reset") }}
        </bm-button>
    </div>
</template>

<script>
import { BmButton, BmLabelIcon } from "@bluemind/ui-components";
export default {
    name: "PrefDeleteRecipientPriorities",
    components: { BmButton, BmLabelIcon },
    data() {
        return { deleted: false };
    },
    computed: {
        disabled() {
            const addressWeights = this.$store.state.mail.addressAutocomplete.addressWeights;
            return !addressWeights || Object.keys(addressWeights)?.length === 0;
        }
    },
    methods: {
        async resetAddressWeights() {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$t("preferences.mail.advanced.recipient_autocomplete.confirm_deletion"),
                {
                    title: this.$t("preferences.mail.advanced.recipient_autocomplete.confirm_deletion.title"),
                    okTitle: this.$t("common.action.reset"),
                    cancelTitle: this.$t("common.cancel")
                }
            );
            if (confirm) {
                this.$store.commit("mail/DELETE_ADDRESS_WEIGHTS");
                this.deleted = true;
            }
        }
    }
};
</script>
