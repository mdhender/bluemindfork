<template>
    <div class="pref-password">
        <div>***********</div>
        <div class="creation-date mb-2">
            {{ $tc("common.created_at", 0, { date: $d(userPasswordLastChange, "full_date_time") }) }}
        </div>
        <bm-button variant="outline" size="lg" icon="pencil" @click="openModal">{{ $t("common.edit") }}</bm-button>
        <change-password-modal ref="change-password" />
    </div>
</template>

<script>
import { BmButton } from "@bluemind/ui-components";
import { mapState } from "vuex";
import ChangePasswordModal from "./ChangePasswordModal";
import BaseField from "../../mixins/BaseField";

export default {
    name: "PrefPassword",
    components: { BmButton, ChangePasswordModal },
    mixins: [BaseField],
    computed: {
        ...mapState("preferences", ["userPasswordLastChange"])
    },
    methods: {
        openModal() {
            this.$refs["change-password"].open();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.pref-password {
    .creation-date {
        color: $neutral-fg-lo1;
    }
}
</style>
