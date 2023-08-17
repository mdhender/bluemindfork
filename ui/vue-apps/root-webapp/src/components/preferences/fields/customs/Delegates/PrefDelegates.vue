<script setup>
import { computed, ref } from "vue";
import { acls, delegations, useDelegation } from "./delegation";
import { Verb } from "@bluemind/core.container.api";
import { BmButton } from "@bluemind/ui-components";
import PrefDelegatesModal from "./PrefDelegatesModal";

useDelegation();
const delegate = ref();
const selected = ref();
const showEditForm = ref(false);
</script>

<template>
    <div class="pref-delegates">
        <p>{{ $t("preferences.account.delegates.description") }}</p>
        <p v-if="!delegations.length">{{ $t("preferences.account.delegates.none") }}</p>
        <p v-else>You have some delegates... Table coming soon ;)</p>
        <bm-button icon="plus" variant="outline" @click="showEditForm = true">
            {{ $t("preferences.account.delegates.create") }}
        </bm-button>
        <pref-delegates-modal :visible.sync="showEditForm" :delegate="delegate" />
    </div>
</template>
