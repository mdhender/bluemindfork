<script setup>
import { BmProgress } from "@bluemind/ui-components";
import { BmReadMore } from "@bluemind/ui-components";
import i18nInstance from "@bluemind/i18n";
import { Quota } from "@bluemind/quota";
import { computeUnit } from "@bluemind/file-utils";
import { WARNING, ERROR } from "@bluemind/alert.store";
import store from "@bluemind/store";
import { computed, onMounted, defineProps } from "vue";
const props = defineProps({
    quota: {
        type: Quota,
        required: true
    }
});
const displayedUsedQuota = computed(() => {
    return computeUnit(props.quota.used * 1000, i18nInstance, { precision: 3 });
});
const displayedTotalQuota = computed(() => {
    return computeUnit(props.quota.total * 1000, i18nInstance, { precision: 3 });
});
const variantColor = computed(() => {
    if (props.quota.isAboveDangerThreshold()) {
        return "danger";
    }
    if (props.quota.isAboveWarningThreshold()) {
        return "warning";
    }
    return "secondary";
});

onMounted(() => {
    sendAlertQuota();
});

function sendAlertQuota() {
    if (!props.quota.isAboveUnsafeThreshold()) {
        return;
    }
    if (props.quota.isAboveCriticalThreshold()) {
        store.dispatch(
            `alert/${ERROR}`,
            alertPayload(
                props.quota.isFullyUsed() ? "alert.mail.used_quota.error" : "alert.mail.almost_used_quota.error",
                "Error"
            )
        );
    } else {
        store.dispatch(`alert/${WARNING}`, alertPayload("alert.mail.almost_used_quota.warning", "Warn"));
    }
}

function alertPayload(message, alertType) {
    return {
        alert: {
            name: message,
            uid: "USED_QUOTA_PERCENTAGE",
            payload: {
                kind: alertType,
                closeable: true
            }
        },
        options: { area: "system-alert", renderer: "UsedQuotaAlert", icon: "exclamation-circle-fill" }
    };
}
</script>

<template>
    <div
        v-if="quota.total !== Infinity"
        class="mail-folder-quota-sidebar"
        :class="quota.isAboveUnsafeThreshold() ? 'critical-div' : ''"
    >
        <bm-progress
            v-if="!quota.isAboveUnsafeThreshold()"
            :value="quota.used"
            :max="quota.total"
            class="quota-progress-bar"
            :variant="variantColor"
        >
        </bm-progress>
        <div class="quota-display flex-fill px-5" :class="quota.isAboveUnsafeThreshold() ? 'bg-danger' : ''">
            <span :class="quota.isAboveUnsafeThreshold() ? 'caption-bold' : 'caption'" class="text-overflow flex-fill">
                <i18n path="mail.mailbox.quota.used">
                    <template #usedQuota>
                        <span class="caption-bold">{{ displayedUsedQuota }}</span>
                    </template>
                    <template #availableQuota>
                        {{ displayedTotalQuota }}
                    </template>
                    <template #usedQuotaPercentage>{{ quota.usedQuotaPercentage() }}</template>
                </i18n>
            </span>
            <div class="quota-more">
                <bm-read-more
                    href="https://doc.bluemind.net/release/5.0/guide_de_l_utilisateur/la_messagerie/classer_et_rechercher_les_messages/organiser_les_dossiers#quotas"
                />
            </div>
        </div>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "./variables";

.mail-folder-quota-sidebar {
    display: flex;
    flex-direction: column;
    height: $quota-height;
    background-color: $surface-hi1;
}
.quota-progress-bar {
    height: $progress-bar-height !important;
}
.quota-more {
    @include until-lg {
        display: flex;
    }
    @include from-lg {
        display: none;
    }
    position: relative;
    float: right;
    right: 0;
    top: 0;
}
.quota-display {
    display: inline-flex;
    padding-top: $sp-3;
    padding-bottom: $sp-3;
    gap: $sp-3;
}
.quota-display > span {
    display: block;
    padding-top: $sp-2 + $sp-1;
    height: 100%;
}
.quota-display:hover .quota-more {
    display: flex;
}

.critical-div,
.critical-div a {
    color: $fill-danger-fg !important;
}
.critical-div {
    border-top: $progress-bar-height solid $fill-danger-bg-hi1;
}
</style>
