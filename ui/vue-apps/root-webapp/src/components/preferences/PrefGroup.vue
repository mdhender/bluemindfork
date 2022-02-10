<template>
    <div :id="group.id" class="pref-group">
        <pref-entry-name :id="anchor(group)" class="pt-4 pb-2" :entry="group" />
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
import PrefSwitchWebmail from "./fields/customs/PrefSwitchWebmail";
import PrefEntryName from "./PrefEntryName";

import { BmFormGroup } from "@bluemind/styleguide";
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
        PrefFieldMultiSelect,
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
        PrefWorkHours
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
@import "~@bluemind/styleguide/css/_variables";

.pref-group {
    padding-right: 4rem;

    .pref-field {
        margin-bottom: 1rem;
    }

    .pref-field-combobox,
    .bm-form-time-picker,
    .bm-form-select,
    .bm-form-multi-select,
    .bm-form-input,
    .bm-rich-editor,
    .pref-item-width {
        width: 24rem !important;
    }

    .b-calendar .bm-form-select {
        width: unset !important;
    }
}
</style>
