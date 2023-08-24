<script setup>
import { ref } from "vue";
import { acls, delegations, useDelegation } from "./delegation";
import { Verb } from "@bluemind/core.container.api";
import { BmButton } from "@bluemind/ui-components";
import PrefDelegatesModal from "./PrefDelegatesModal";
import PrefDelegatesTable from "./PrefDelegatesTable";

useDelegation();
const delegate = ref();
const selected = ref();
const showEditForm = ref(false);

const createDelegate = () => {
    delegate.value = undefined;
    showEditForm.value = true;
};

const editDelegate = userUid => {
    delegate.value = userUid;
    showEditForm.value = true;
};
</script>

<template>
    <div class="pref-delegates">
        <p>{{ $t("preferences.account.delegates.description") }}</p>
        <p v-if="!delegations.length">{{ $t("preferences.account.delegates.none") }}</p>
        <pref-delegates-table v-else @edit="editDelegate" />
        <bm-button icon="plus" variant="outline" size="lg" @click="createDelegate">
            {{ $t("preferences.account.delegates.create") }}
        </bm-button>
        <pref-delegates-modal :visible.sync="showEditForm" :delegate.sync="delegate" />
    </div>
</template>
