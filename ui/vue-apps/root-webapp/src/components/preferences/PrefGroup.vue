<template>
    <div class="pref-group">
        <div class="pt-4 pb-2 d-flex align-items-center">
            <span class="h2" :class="{ 'text-alternate-light': group.readOnly }">{{ group.title }}</span>
            <span v-if="group.notAvailable(localUserSettings)" class="available-soon h2">
                {{ $t("common.available_soon") }}
            </span>
            <bm-label-icon v-if="group.readOnly" icon="exclamation-circle" class="text-warning ml-2">
                {{ $t("preferences.role.missing.warning") }}
            </bm-label-icon>
        </div>
        <bm-form-group :aria-label="group.title" :disabled="group.notAvailable(localUserSettings) || group.readOnly">
            <template v-for="field in group.fields">
                <bm-form-group :key="field.name" :aria-label="field.name" :label="field.name">
                    <component
                        :is="field.component"
                        v-bind="{
                            'local-user-settings': localUserSettings,
                            setting: field.setting,
                            name: field.name,
                            options: field.options
                        }"
                        @requestSave="$emit('requestSave')"
                    />
                </bm-form-group>
                <template v-if="field.options && field.options.additional_component">
                    <component :is="field.options.additional_component" :key="field.name" />
                </template>
            </template>
        </bm-form-group>
    </div>
</template>

<script>
import PrefFieldCheck from "./fields/PrefFieldCheck";
import PrefFieldChoice from "./fields/PrefFieldChoice";
import PrefFieldComboBox from "./fields/PrefFieldComboBox";
import PrefFieldMultiSelect from "./fields/PrefFieldMultiSelect";
import PrefFieldSelect from "./fields/PrefFieldSelect";
import PrefFieldSwitch from "./fields/PrefFieldSwitch";

import PrefAllDayEventReminder from "./fields/customs/PrefAllDayEventReminder";
import PrefAlwaysShowQuota from "./fields/customs/PrefAlwaysShowQuota";
import PrefAPIKey from "./fields/customs/PrefAPIKey";
import PrefAutomaticReply from "./fields/customs/PrefAutomaticReply";
import PrefDownloads from "./fields/customs/PrefDownloads";
import PrefEmailsForwarding from "./fields/customs/PrefEmailsForwarding";
import PrefEnableNotifications from "./fields/customs/PrefEnableNotifications";
import PrefEventReminder from "./fields/customs/PrefEventReminder";
import PrefIMSetPhonePresence from "./fields/customs/PrefIMSetPhonePresence";
import PrefManageIdentities from "./fields/customs/PrefManageIdentities";
import PrefManageMyCalendars from "./fields/customs/Calendars/MyCalendars/PrefManageMyCalendars";
import PrefManageMyMailbox from "./fields/customs/Mailboxes/PrefManageMyMailbox";
import PrefManageOtherCalendars from "./fields/customs/Calendars/OtherCalendars/PrefManageOtherCalendars";
import PrefPassword from "./fields/customs/PrefPassword";
import PrefRemoteImage from "./fields/customs/PrefRemoteImage";
import PrefResetLocalData from "./fields/customs/PrefResetLocalData";
import PrefWorkHours from "./fields/customs/PrefWorkHours";

import { BmFormGroup, BmLabelIcon } from "@bluemind/styleguide";

export default {
    name: "PrefGroup",
    components: {
        BmFormGroup,
        BmLabelIcon,
        PrefFieldCheck,
        PrefFieldChoice,
        PrefFieldComboBox,
        PrefFieldMultiSelect,
        PrefFieldSelect,
        PrefFieldSwitch,
        PrefAllDayEventReminder,
        PrefAlwaysShowQuota,
        PrefAPIKey,
        PrefAutomaticReply,
        PrefDownloads,
        PrefEmailsForwarding,
        PrefEnableNotifications,
        PrefEventReminder,
        PrefIMSetPhonePresence,
        PrefManageIdentities,
        PrefManageMyCalendars,
        PrefManageMyMailbox,
        PrefManageOtherCalendars,
        PrefPassword,
        PrefRemoteImage,
        PrefResetLocalData,
        PrefWorkHours
    },
    props: {
        group: {
            type: Object,
            required: true
        },
        localUserSettings: {
            type: Object,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-group {
    padding-left: 4rem;
    padding-right: 4rem;
}
</style>
