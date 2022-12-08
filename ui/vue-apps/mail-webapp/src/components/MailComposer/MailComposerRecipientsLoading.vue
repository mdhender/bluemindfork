<template>
    <div class="mail-composer-recipients-loading">
        <bm-row class="align-items-center">
            <bm-col cols="11 d-flex recipient py-1">
                {{ $t("common.to") }}
                <bm-skeleton-avatar class="ml-2" />
                <bm-skeleton width="20%" />
            </bm-col>
        </bm-row>
        <hr class="m-0" />
        <bm-row class="align-items-center">
            <bm-col cols="11 d-flex recipient py-1">
                {{ $t("common.cc") }}
                <bm-skeleton-avatar class="ml-2" />
                <bm-skeleton width="30%" />
            </bm-col>
            <bm-col cols="1" class="text-right">
                <bm-button variant="text">
                    {{ $t("common.bcc") }}
                </bm-button>
            </bm-col>
        </bm-row>
    </div>
</template>

<script>
import { BmButton, BmCol, BmRow, BmSkeleton, BmSkeletonAvatar } from "@bluemind/ui-components";

export default {
    name: "MailVIewerLoadingHeader",
    components: {
        BmButton,
        BmCol,
        BmRow,
        BmSkeleton,
        BmSkeletonAvatar
    }
};
</script>

<style lang="scss">
.mail-composer-recipients-loading {
    .recipient {
        height: 3em;
    }
}
</style>
