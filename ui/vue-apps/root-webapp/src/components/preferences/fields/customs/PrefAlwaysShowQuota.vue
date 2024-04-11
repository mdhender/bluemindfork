<template>
    <div class="mt-4 d-flex align-items-center">
        <bm-progress
            circular
            :value="quota.used"
            :max="quota.total"
            class="d-inline-block mr-3"
            :variant="quota.isAboveWarningThreshold() ? 'danger' : 'secondary'"
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
import { BaseField } from "@bluemind/preferences";
import { computeUnit } from "@bluemind/file-utils";
import { BmProgress } from "@bluemind/ui-components";
import { Quota } from "@bluemind/quota";
import i18n from "@bluemind/i18n";

export default {
    name: "PrefAlwaysShowQuota",
    components: { BmProgress },
    mixins: [BaseField],
    computed: {
        hasNoQuota() {
            return this.quota && this.quota.total === Infinity;
        },
        quota() {
            return new Quota(this.$store.state["root-app"].quota);
        },
        displayedUsedQuota() {
            return computeUnit(this.quota.used * 1000, i18n, { precision: 3 });
        },
        displayedTotalQuota() {
            return computeUnit(this.quota.total * 1000, i18n, { precision: 3 });
        }
    }
};
</script>
