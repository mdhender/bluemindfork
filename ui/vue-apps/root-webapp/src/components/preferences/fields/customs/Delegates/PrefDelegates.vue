<script setup>
import { ref, watchEffect } from "vue";
import {
    acls,
    countDelegatesHavingTheCopyImipRule,
    delegates,
    delegations,
    hasCopyImipMailboxRuleKeepCopy,
    updateCopyImipMailboxRule,
    useDelegation
} from "./delegation";
import { Verb } from "@bluemind/core.container.api";
import { BmButton, BmFormGroup, BmFormRadioGroup, BmFormRadio, BmIcon, BmReadMore } from "@bluemind/ui-components";
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

const delegatesWithCopyImipRuleCount = ref();
const receiveImipChoices = { ONLY_DELEGATE: 0, BOTH: 1, COPY: 2 };
const receiveImipChoice = ref();
watchEffect(async () => {
    delegatesWithCopyImipRuleCount.value = await countDelegatesHavingTheCopyImipRule(...Object.keys(delegates.value));
    const copy = true; // TODO next PR
    const both = await hasCopyImipMailboxRuleKeepCopy();
    receiveImipChoice.value = copy
        ? receiveImipChoices.COPY
        : both
        ? receiveImipChoices.BOTH
        : receiveImipChoices.ONLY_DELEGATE;
});

const updateFilter = () => {
    updateCopyImipMailboxRule({ keepCopy: receiveImipChoice.value === receiveImipChoices.BOTH });
};

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
        <template v-if="delegations.length">
            <div class="d-flex align-items-center pt-4">
                <bm-icon icon="open-envelope" class="mr-4" />
                {{
                    delegatesWithCopyImipRuleCount > 0
                        ? $t("preferences.account.delegates.receive_imip", { count: delegatesWithCopyImipRuleCount })
                        : $t("preferences.account.delegates.receive_imip.none")
                }}
                <bm-read-more class="ml-5" href="https://doc.bluemind.net/release/5.0/category/guide-de-lutilisateur" />
            </div>
            <span v-if="delegatesWithCopyImipRuleCount == 0" class="bold">
                {{ $t("preferences.account.delegates.receive_imip.none.notice") }}
            </span>
            <bm-form-group
                v-else
                v-slot="{ ariaDescribedby }"
                :label="$t('preferences.account.delegates.receive_imip.choice')"
                class="mt-4 font-weight-bold"
            >
                <bm-form-radio-group
                    v-model="receiveImipChoice"
                    class="py-4"
                    :aria-describedby="ariaDescribedby"
                    @change="updateFilter"
                >
                    <bm-form-radio :value="receiveImipChoices.ONLY_DELEGATE" class="ml-6">
                        {{ $t("preferences.account.delegates.receive_imip.choice.only_delegate") }}
                    </bm-form-radio>
                    <bm-form-radio :value="receiveImipChoices.COPY" class="ml-6">
                        {{ $t("preferences.account.delegates.receive_imip.choice.copy") }}
                    </bm-form-radio>
                    <bm-form-radio :value="receiveImipChoices.BOTH" class="ml-6">
                        {{ $t("preferences.account.delegates.receive_imip.choice.both") }}
                    </bm-form-radio>
                </bm-form-radio-group>
            </bm-form-group>
        </template>
        <pref-delegates-modal
            :visible.sync="showEditForm"
            :delegate.sync="delegate"
            :only-delegate-receives-imip="receiveImipChoice === receiveImipChoices.BOTH"
        />
    </div>
</template>
