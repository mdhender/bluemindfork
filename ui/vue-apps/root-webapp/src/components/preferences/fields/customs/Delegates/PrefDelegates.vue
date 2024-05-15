<script setup>
import { ref, watchEffect } from "vue";
import { ERROR, SUCCESS } from "@bluemind/alert.store";
import store from "@bluemind/store";
import { useDelegation } from "./delegation";
import { Verb } from "@bluemind/core.container.api";
import { BmButton, BmFormGroup, BmFormRadioGroup, BmFormRadio, BmIcon, BmReadMore } from "@bluemind/ui-components";
import PrefDelegatesModal from "./PrefDelegatesModal";
import PrefDelegatesTable from "./PrefDelegatesTable";
import { SAVE_ALERT } from "../../../Alerts/defaultAlerts";

const {
    countDelegatesHavingTheCopyImipRule,
    delegates,
    receiveImipOptions,
    computeReceiveImipOption,
    updateReceiveImipOption
} = useDelegation();

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
const receiveImipOption = ref(receiveImipOptions.BOTH);
watchEffect(() => {
    delegatesWithCopyImipRuleCount.value = countDelegatesHavingTheCopyImipRule();
    receiveImipOption.value = computeReceiveImipOption();
});

const updateFilter = async () => {
    try {
        await updateReceiveImipOption(receiveImipOption.value);
        store.dispatch(`alert/${SUCCESS}`, SAVE_ALERT);
    } catch {
        store.dispatch(`alert/${ERROR}`, SAVE_ALERT);
    }
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
        <p v-if="!Object.keys(delegates).length">{{ $t("preferences.account.delegates.none") }}</p>
        <pref-delegates-table v-else @edit="editDelegate" />
        <bm-button icon="plus" variant="outline" size="lg" class="mb-5" @click="createDelegate">
            {{ $t("preferences.account.delegates.create") }}
        </bm-button>
        <template v-if="Object.keys(delegates).length">
            <div class="d-flex align-items-center pt-4 regular">
                <bm-icon icon="mail-title" class="mr-4 align-self-start" />
                <span>
                    <span class="mr-5 align-top">
                        {{
                            delegatesWithCopyImipRuleCount > 0
                                ? $t("preferences.account.delegates.receive_imip", {
                                      count: delegatesWithCopyImipRuleCount
                                  })
                                : $t("preferences.account.delegates.receive_imip.none")
                        }}
                    </span>
                    <bm-read-more
                        class="d-inline-flex"
                        href="https://doc.bluemind.net/release/5.1/category/guide_de_l_utilisateur/parametrer_le_compte_utilisateur#manage-my-invitations"
                    />
                </span>
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
                    v-model="receiveImipOption"
                    class="py-4"
                    :aria-describedby="ariaDescribedby"
                    @change="updateFilter"
                >
                    <bm-form-radio :value="receiveImipOptions.ONLY_DELEGATE" class="receive-imip-option">
                        {{ $t("preferences.account.delegates.receive_imip.choice.only_delegate") }}
                    </bm-form-radio>
                    <bm-form-radio :value="receiveImipOptions.COPY" class="receive-imip-option">
                        {{ $t("preferences.account.delegates.receive_imip.choice.copy") }}
                    </bm-form-radio>
                    <bm-form-radio :value="receiveImipOptions.BOTH" class="receive-imip-option">
                        {{ $t("preferences.account.delegates.receive_imip.choice.both") }}
                    </bm-form-radio>
                </bm-form-radio-group>
            </bm-form-group>
        </template>
        <pref-delegates-modal
            :visible.sync="showEditForm"
            :delegate.sync="delegate"
            :receive-imip-option="receiveImipOption"
        />
    </div>
</template>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/variables";

.pref-delegates {
    .receive-imip-option {
        margin-left: map-get($icon-sizes, "md") + $sp-4;
    }
}
</style>
