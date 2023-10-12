<template>
    <div :id="group.id" class="pref-group">
        <h3 v-show="!noHeading" :id="anchor(group)" :class="{ 'group-disabled': group.disabled }">
            {{ group.name }}
        </h3>
        <bm-label-icon v-if="group.disabled" icon="exclamation-circle" class="text-warning ml-2 mb-5">
            {{ $t("preferences.role.missing.warning") }}
        </bm-label-icon>
        <div v-show="!collapsed" class="group-body">
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
                        v-bind="field.component.options"
                    />
                </template>
            </bm-form-group>
        </div>
    </div>
</template>

<script>
import { mapGetters } from "vuex";

import PrefFieldCheck from "./fields/PrefFieldCheck";
import PrefFieldChoice from "./fields/PrefFieldChoice";
import PrefFieldComboBox from "./fields/PrefFieldComboBox";
import PrefFieldInfo from "./fields/PrefFieldInfo";
import PrefFieldInput from "./fields/PrefFieldInput";
import PrefFieldLabel from "./fields/PrefFieldLabel";
import PrefFieldSelect from "./fields/PrefFieldSelect";
import PrefFieldSwitch from "./fields/PrefFieldSwitch";

import PrefAllDayEventReminder from "./fields/customs/PrefAllDayEventReminder";
import PrefAlwaysShowQuota from "./fields/customs/PrefAlwaysShowQuota";
import PrefAPIKey from "./fields/customs/PrefAPIKey";
import PrefAutomaticReply from "./fields/customs/PrefAutomaticReply";
import PrefComposerDefaultFont from "./fields/customs/PrefComposerDefaultFont";
import PrefDelegates from "./fields/customs/Delegates/PrefDelegates";
import PrefDeleteRecipientPriorities from "./fields/customs/PrefDeleteRecipientPriorities";
import PrefDomainFilterRules from "./fields/customs/FilterRules/PrefDomainFilterRules";
import PrefDownloads from "./fields/customs/PrefDownloads";
import PrefEmailsForwarding from "./fields/customs/PrefEmailsForwarding";
import PrefEnableNotifications from "./fields/customs/PrefEnableNotifications";
import PrefEventReminder from "./fields/customs/PrefEventReminder";
import PrefExtAccountCreation from "./fields/customs/ExternalAccounts/PrefExtAccountCreation";
import PrefExtAccountList from "./fields/customs/ExternalAccounts/PrefExtAccountList";
import PrefIMSetPhonePresence from "./fields/customs/PrefIMSetPhonePresence";
import PrefMailtoLinks from "./fields/customs/PrefMailtoLinks";
import PrefManageIdentities from "./fields/customs/PrefManageIdentities";
import PrefManageMyAddressBooks from "./fields/customs/ContainersManagement/Contacts/PrefManageMyAddressBooks";
import PrefManageMyCalendars from "./fields/customs/ContainersManagement/Calendars/MyCalendars/PrefManageMyCalendars";
import PrefManageMyMailbox from "./fields/customs/ContainersManagement/Mailboxes/PrefManageMyMailbox";
import PrefManageMyTodoLists from "./fields/customs/ContainersManagement/Tasks/PrefManageMyTodoLists";
import PrefManageOtherAddressBooks from "./fields/customs/ContainersManagement/Contacts/PrefManageOtherAddressBooks";
import PrefManageOtherCalendars from "./fields/customs/ContainersManagement/Calendars/PrefManageOtherCalendars";
import PrefManageOtherMailboxes from "./fields/customs/ContainersManagement/Mailboxes/PrefManageOtherMailboxes";
import PrefManageOtherTodoLists from "./fields/customs/ContainersManagement/Tasks/PrefManageOtherTodoLists";
import PrefMyFilterRules from "./fields/customs/FilterRules/PrefMyFilterRules";
import PrefPassword from "./fields/customs/PrefPassword";
import PrefRemoteImage from "./fields/customs/PrefRemoteImage";
import PrefResetLocalData from "./fields/customs/PrefResetLocalData";
import PrefSwitchWebmail from "./fields/customs/PrefSwitchWebmail";
import PrefTags from "./fields/customs/Tags/PrefTags";
import PrefWorkHours from "./fields/customs/PrefWorkHours";
import PrefWorkingDays from "./fields/customs/PrefWorkingDays";

import { BmFormGroup, BmLabelIcon } from "@bluemind/ui-components";
import Navigation from "./mixins/Navigation";

export default {
    name: "PrefGroup",
    components: {
        BmFormGroup,
        BmLabelIcon,
        PrefAllDayEventReminder,
        PrefAlwaysShowQuota,
        PrefAPIKey,
        PrefAutomaticReply,
        PrefComposerDefaultFont,
        PrefDelegates,
        PrefDeleteRecipientPriorities,
        PrefDomainFilterRules,
        PrefDownloads,
        PrefEmailsForwarding,
        PrefEnableNotifications,
        PrefEventReminder,
        PrefExtAccountCreation,
        PrefExtAccountList,
        PrefFieldCheck,
        PrefFieldChoice,
        PrefFieldComboBox,
        PrefFieldInfo,
        PrefFieldInput,
        PrefFieldLabel,
        PrefFieldSelect,
        PrefFieldSwitch,
        PrefIMSetPhonePresence,
        PrefMailtoLinks,
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
        },
        noHeading: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        ...mapGetters("preferences", ["SEARCH_PATTERN"])
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-group {
    padding-bottom: $sp-5;

    .form-group {
        margin-bottom: 0;
    }
    .pref-field {
        margin-bottom: $sp-5;
    }

    > h3 {
        padding-bottom: $sp-6;
        margin: 0;
        color: $neutral-fg-hi1;
        &.group-disabled {
            color: $neutral-fg-disabled;
        }
    }

    .group-body {
        padding-bottom: $sp-6;
        @include from-lg {
            padding-right: $sp-7;
        }
    }

    .pref-field-combobox,
    .bm-form-time-picker,
    .pref-field-select,
    .pref-field-multi-select,
    .pref-field-input,
    .pref-filter,
    .pref-item-width {
        width: base-px-to-rem(400) !important;
        max-width: 100%;
        @include from-lg {
            width: base-px-to-rem(328) !important;
        }
    }
}
</style>
