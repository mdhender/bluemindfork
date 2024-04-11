import {
    USED_QUOTA_PERCENTAGE_WARNING,
    USED_QUOTA_PERCENTAGE_DANGER,
    USED_QUOTA_PERCENTAGE_UNSAFE,
    USED_QUOTA_PERCENTAGE_CRITICAL
} from "./percentageThresholds";

type Octet = number;

type QuotaType = { used: Octet; total: Octet };
export default class Quota {
    constructor(private quota: QuotaType) {
        if (!Number.isInteger(quota.used)) {
            this.quota.used = 0;
        }
        if (!quota.total || !Number.isInteger(quota.total)) {
            this.quota.total = Infinity;
        }
    }

    get used(): number {
        return this.quota.used;
    }

    get total(): number {
        return this.quota.total;
    }

    usedQuotaPercentage(): number {
        return Math.round((this.quota.used / this.quota.total) * 100);
    }

    isFullyUsed(): boolean {
        return this.usedQuotaPercentage() >= 100;
    }

    isAboveCriticalThreshold(): boolean {
        return this.usedQuotaPercentage() >= USED_QUOTA_PERCENTAGE_CRITICAL;
    }

    isAboveUnsafeThreshold(): boolean {
        return this.usedQuotaPercentage() >= USED_QUOTA_PERCENTAGE_UNSAFE;
    }

    isAboveDangerThreshold(): boolean {
        return this.usedQuotaPercentage() >= USED_QUOTA_PERCENTAGE_DANGER;
    }

    isAboveWarningThreshold(): boolean {
        return this.usedQuotaPercentage() >= USED_QUOTA_PERCENTAGE_WARNING;
    }
}
