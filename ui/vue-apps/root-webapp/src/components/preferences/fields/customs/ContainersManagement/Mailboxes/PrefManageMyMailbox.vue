<template>
    <div class="pref-manage-my-mailbox">
        <!-- FIXME when doing "The Wire" MVP: remove disabled prop and implement feature -->
        <bm-form-checkbox switch disabled>
            <span class="mr-5">{{ $t("preferences.mail.my_mailbox.switch_offline_sync") }}</span>
            <span class="available-soon bold">{{ $t("common.available_soon") }}</span>
            <!-- FIXME when doing "The Wire" MVP: need to write a doc page here to explain mailbox offlineSync -->
            <!-- <a href="">{{ $t("common.read_more") }}</a> -->
        </bm-form-checkbox>
        <bm-button variant="outline" size="lg" class="mt-5" icon="share" @click="openShareModal">
            {{ $t("common.manage_my_shares") }}
        </bm-button>
        <manage-container-shares-modal ref="manage-shares" />
    </div>
</template>

<script>
import { ContainerHelper, ContainerType } from "../container";
import ManageContainerSharesModal from "../ManageContainerSharesModal/ManageContainerSharesModal";
import MailboxHelper from "./helper";
import { BmButton, BmFormCheckbox } from "@bluemind/ui-components";
import { mapState } from "vuex";

ContainerHelper.register(ContainerType.MAILBOX, MailboxHelper);

export default {
    name: "PrefManageMyMailbox",
    components: { BmButton, BmFormCheckbox, ManageContainerSharesModal },
    computed: {
        ...mapState("preferences", { myMailboxContainer: state => state.containers.myMailboxContainer })
    },
    methods: {
        openShareModal() {
            this.$refs["manage-shares"].open(this.myMailboxContainer);
        }
    }
};
</script>
