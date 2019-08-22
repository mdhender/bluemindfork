<template>
    <bm-list-group-item class="mail-message-list-header bg-transparent align-with-items pb-2 pt-3 pl-3">
        <bm-row>
            <bm-col cols="1">
                <bm-check />
            </bm-col>
            <bm-col class="d-none d-sm-block d-md-none d-xl-block pl-0">
                <span class="text-nowrap">
                    {{ $t("common.filter") }} 
                    <span class="fake-select">{{ $t("common.all") }} <bm-icon icon="caret-down" /></span>
                </span>
            </bm-col>
            <bm-col class="d-none d-sm-block d-md-none d-xl-block text-right pr-0">
                <span class="text-nowrap">
                    {{ $t("common.sort_by") }} 
                    <span class="fake-select">{{ $t("common.date") }} <bm-icon icon="caret-down" /></span>
                </span>
            </bm-col>
        </bm-row>
    </bm-list-group-item>
</template>

<script>
import { BmCheck, BmCol, BmListGroupItem, BmIcon, BmRow } from "@bluemind/styleguide";

export default {
    name: "MailMessageListHeader",
    components: {
        BmCheck,
        BmCol,
        BmListGroupItem,
        BmIcon,
        BmRow
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.fake-select {
    color: $info-dark;
    font-weight: $font-weight-bold;
    cursor: pointer;
}

div.align-with-items {
    margin-left: 6px; //fixme
    border-bottom: 0!important;
    border-left: 5px solid transparent!important;
}
</style>
