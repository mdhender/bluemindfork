<template>
    <div v-if="!collapsed" class="mt-4 d-flex align-items-center">
        <bm-progress
            circular
            :value="quota.used"
            :max="quota.total"
            class="d-inline-block mr-3"
            :variant="usedQuotaPercentage > USED_QUOTA_PERCENTAGE_WARNING ? 'danger' : 'secondary'"
            show-progress
        >
            <template v-if="hasNoQuota">{{ $t("preferences.mail.quota.unlimited") }}</template>
        </bm-progress>
        <i18n v-if="!hasNoQuota" path="preferences.mail.quota.used">
            <template #used>
                <strong>{{ displayedUsedQuota }}</strong>
            </template>
            <template #total>{{ displayedTotalQuota }}</template>
        </i18n>
    </div>
</template>

<script>
import { mapState } from "vuex";

import { USED_QUOTA_PERCENTAGE_WARNING } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { computeUnit } from "@bluemind/file-utils";
import { BmProgress } from "@bluemind/styleguide";

import BaseField from "../../mixins/BaseField";

export default {
    name: "PrefAlwaysShowQuota",
    components: { BmProgress },
    mixins: [BaseField],
    data() {
        return { USED_QUOTA_PERCENTAGE_WARNING };
    },
    computed: {
        ...mapState("root-app", ["quota"]),
        hasNoQuota() {
            return this.quota && !this.quota.total;
        },
        displayedUsedQuota() {
            return computeUnit(this.quota.used * 1000, inject("i18n"));
        },
        displayedTotalQuota() {
            return computeUnit(this.quota.total * 1000, inject("i18n"));
        },
        usedQuotaPercentage() {
            return (this.quota.used / this.quota.total) * 100;
        }
    }
};
</script>
