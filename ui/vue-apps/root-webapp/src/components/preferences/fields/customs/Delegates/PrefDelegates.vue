<script setup>
import { ref, watchEffect } from "vue";
import { acls, delegates, delegations, hasCopyImipMailboxRuleAction, useDelegation } from "./delegation";
import { Verb } from "@bluemind/core.container.api";
import { BmButton, BmIcon } from "@bluemind/ui-components";
import PrefDelegatesModal from "./PrefDelegatesModal";
import PrefDelegatesTable from "./PrefDelegatesTable";

useDelegation();
const delegate = ref();
const showEditForm = ref(false);

const createDelegate = () => {
    delegate.value = undefined;
    showEditForm.value = true;
};

const editDelegate = userUid => {
    delegate.value = userUid;
    showEditForm.value = true;
};

const noDelegatesReceiveImip = ref();
watchEffect(async () => {
    noDelegatesReceiveImip.value = !(await hasCopyImipMailboxRuleAction(...Object.keys(delegates.value)));
});
watchEffect(() => {
    if (showEditForm.value == false) {
        delegate.value = undefined;
    }
});
</script>

<template>
    <div class="pref-delegates">
        <p>{{ $t("preferences.account.delegates.description") }}</p>
        <p v-if="!delegations.length">{{ $t("preferences.account.delegates.none") }}</p>
        <pref-delegates-table v-else @edit="editDelegate" />
        <bm-button icon="plus" variant="outline" size="lg" @click="createDelegate">
            {{ $t("preferences.account.delegates.create") }}
        </bm-button>
        <template v-if="noDelegatesReceiveImip">
            <div class="d-flex align-items-center pt-4">
                <bm-icon icon="open-envelope" class="mr-4" />{{ $t("preferences.account.delegates.receive_imip.none") }}
            </div>
            <span class="bold">{{ $t("preferences.account.delegates.receive_imip.none.notice") }}</span>
        </template>
        <pref-delegates-modal :visible.sync="showEditForm" :delegate.sync="delegate" />
    </div>
</template>
