<template>
    <div>
        <div>***********</div>
        <div class="mb-2 text-alternate-light">
            {{ $tc("common.created_at", 0, { date: $d(userPasswordLastChange, "full_date_time") }) }}
        </div>
        <bm-button variant="outline-secondary" @click="openModal">{{ $t("common.edit") }}</bm-button>
        <change-password-modal ref="change-password" />
    </div>
</template>

<script>
import { BmButton } from "@bluemind/styleguide";
import { mapState } from "vuex";
import ChangePasswordModal from "./ChangePasswordModal";

export default {
    name: "PrefPassword",
    components: { BmButton, ChangePasswordModal },
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
