<template>
    <div class="pref-manage-my-mailbox">
        <!-- FIXME when doing "The Wire" MVP: remove disabled prop and implement feature -->
        <bm-form-checkbox switch disabled>
            {{ $t("preferences.mail.my_mailbox.switch_offline_sync") }}
            <span class="available-soon h2 ml-2">{{ $t("common.available_soon") }}</span>
            <!-- FIXME when doing "The Wire" MVP: need to write a doc page here to explain mailbox offlineSync -->
            <!-- <a href="">{{ $t("common.read_more") }}</a> -->
        </bm-form-checkbox>
        <bm-button variant="outline-secondary" class="mt-3" @click="openShareModal">
            {{ $t("common.manage_my_shares") }}
        </bm-button>
        <manage-shares-modal ref="manage-shares" />
    </div>
</template>

<script>
import ManageSharesModal from "../ManageSharesModal/ManageSharesModal";
import { BmButton, BmFormCheckbox } from "@bluemind/styleguide";
import { mapState } from "vuex";

export default {
    name: "PrefManageMyMailbox",
    components: { BmButton, BmFormCheckbox, ManageSharesModal },
    computed: {
        ...mapState("preferences", ["myMailboxContainer"])
    },
    methods: {
        openShareModal() {
            this.$refs["manage-shares"].open(this.myMailboxContainer);
        }
    }
};
</script>
