<template>
    <div :id="group.id" class="pref-group">
        <pref-entry-name :id="anchor(group)" :entry="group" />
        <div v-if="group.description">{{ group.description }}</div>
        <bm-form-group :aria-describedby="anchor(group)" :disabled="group.disabled">
            <template v-for="field in group.fields">
                <component
                    :is="field.component.name"
                    v-if="field.visible"
                    :id="field.id"
                    :key="field.id"
                    class="pref-field"
                    :disabled="field.disabled"
                    :collapsed="collapsed"
                    v-bind="field.component.options"
                />
            </template>
        </bm-form-group>
    </div>
</template>

<script>
import PrefFieldCheck from "./fields/PrefFieldCheck";
import PrefFieldChoice from "./fields/PrefFieldChoice";
import PrefFieldComboBox from "./fields/PrefFieldComboBox";
import PrefFieldInput from "./fields/PrefFieldInput";
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
import PrefDomainFilterRules from "./fields/customs/FilterRules/PrefDomainFilterRules";
import PrefMyFilterRules from "./fields/customs/FilterRules/PrefMyFilterRules";
import PrefExtAccountCreation from "./fields/customs/ExternalAccounts/PrefExtAccountCreation";
import PrefExtAccountList from "./fields/customs/ExternalAccounts/PrefExtAccountList";
import PrefIMSetPhonePresence from "./fields/customs/PrefIMSetPhonePresence";
import PrefManageIdentities from "./fields/customs/PrefManageIdentities";
import PrefManageMyAddressBooks from "./fields/customs/ContainersManagement/Contacts/PrefManageMyAddressBooks";
import PrefManageMyCalendars from "./fields/customs/ContainersManagement/Calendars/MyCalendars/PrefManageMyCalendars";
import PrefManageMyMailbox from "./fields/customs/ContainersManagement/Mailboxes/PrefManageMyMailbox";
import PrefManageMyTodoLists from "./fields/customs/ContainersManagement/Tasks/PrefManageMyTodoLists";
import PrefManageOtherAddressBooks from "./fields/customs/ContainersManagement/Contacts/PrefManageOtherAddressBooks";
import PrefManageOtherCalendars from "./fields/customs/ContainersManagement/Calendars/PrefManageOtherCalendars";
import PrefManageOtherMailboxes from "./fields/customs/ContainersManagement/Mailboxes/PrefManageOtherMailboxes";
import PrefManageOtherTodoLists from "./fields/customs/ContainersManagement/Tasks/PrefManageOtherTodoLists";
import PrefPassword from "./fields/customs/PrefPassword";
import PrefRemoteImage from "./fields/customs/PrefRemoteImage";
import PrefResetLocalData from "./fields/customs/PrefResetLocalData";
import PrefTags from "./fields/customs/Tags/PrefTags";
import PrefWorkHours from "./fields/customs/PrefWorkHours";
import PrefWorkingDays from "./fields/customs/PrefWorkingDays";
import PrefSwitchWebmail from "./fields/customs/PrefSwitchWebmail";
import PrefEntryName from "./PrefEntryName";

import { BmFormGroup } from "@bluemind/ui-components";
import Navigation from "./mixins/Navigation";

export default {
    name: "PrefGroup",
    components: {
        BmFormGroup,
        PrefEntryName,
        PrefFieldCheck,
        PrefFieldChoice,
        PrefFieldComboBox,
        PrefFieldInput,
        PrefFieldSelect,
        PrefFieldSwitch,
        PrefAllDayEventReminder,
        PrefAlwaysShowQuota,
        PrefAPIKey,
        PrefAutomaticReply,
        PrefDomainFilterRules,
        PrefDownloads,
        PrefEmailsForwarding,
        PrefEnableNotifications,
        PrefEventReminder,
        PrefExtAccountCreation,
        PrefExtAccountList,
        PrefIMSetPhonePresence,
        PrefManageIdentities,
        PrefManageMyAddressBooks,
        PrefManageMyCalendars,
        PrefManageMyMailbox,
        PrefManageMyTodoLists,
        PrefManageOtherAddressBooks,
        PrefManageOtherCalendars,
        PrefManageOtherMailboxes,
        PrefManageOtherTodoLists,
        PrefMyFilterRules,
        PrefPassword,
        PrefRemoteImage,
        PrefResetLocalData,
        PrefSwitchWebmail,
        PrefTags,
        PrefWorkHours,
        PrefWorkingDays
    },
    mixins: [Navigation],
    props: {
        group: {
            type: Object,
            required: true
        },
        collapsed: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "./_variables";
@import "~@bluemind/ui-components/src/css/variables";

.pref-group {
    .pref-field {
        margin-bottom: $sp-5;
    }
    .pref-entry-name {
        padding-top: $pref-entry-name-padding-top;
        padding-bottom: $pref-entry-name-padding-bottom;
    }

    .pref-field-combobox,
    .bm-form-time-picker,
    .pref-field-select,
    .pref-field-multi-select,
    .pref-field-input,
    .pref-filter,
    .pref-item-width {
        width: 24rem !important;
        max-width: 100%;
    }
}
</style>
