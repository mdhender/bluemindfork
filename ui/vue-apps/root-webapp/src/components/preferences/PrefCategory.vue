<template>
    <div :id="categoryId(section.code, category.code)" class="pref-category">
        <div v-for="group in filteredFieldGroups" :key="group.title" class="pref-category">
            <div class="pt-4 pb-2 d-flex align-items-center">
                <span class="h2" :class="{ 'text-alternate-light': group.readOnly }">{{ group.title }}</span>
                <span v-if="group.notAvailable(localUserSettings)" class="available-soon h2">
                    {{ $t("common.available_soon") }}
                </span>
                <bm-label-icon v-if="group.readOnly" icon="exclamation-circle" class="text-warning ml-2">
                    {{ $t("preferences.role.missing.warning") }}
                </bm-label-icon>
            </div>
            <bm-form-group
                :aria-label="group.title"
                :disabled="group.notAvailable(localUserSettings) || group.readOnly"
            >
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
    </div>
</template>

<script>
import PrefFieldCheck from "./fields/PrefFieldCheck";
import PrefFieldChoice from "./fields/PrefFieldChoice";
import PrefFieldComboBox from "./fields/PrefFieldComboBox";
import PrefFieldSelect from "./fields/PrefFieldSelect";
import PrefFieldSwitch from "./fields/PrefFieldSwitch";

import PrefAllDayEventReminder from "./fields/customs/PrefAllDayEventReminder";
import PrefAlwaysShowQuota from "./fields/customs/PrefAlwaysShowQuota";
import PrefAutomaticReply from "./fields/customs/PrefAutomaticReply";
import PrefEmailsForwarding from "./fields/customs/PrefEmailsForwarding";
import PrefEnableNotifications from "./fields/customs/PrefEnableNotifications";
import PrefEventReminder from "./fields/customs/PrefEventReminder";
import PrefIMSetPhonePresence from "./fields/customs/PrefIMSetPhonePresence";
import PrefManageIdentities from "./fields/customs/PrefManageIdentities";
import PrefManageMyCalendars from "./fields/customs/MyCalendars/PrefManageMyCalendars";
import PrefPassword from "./fields/customs/PrefPassword";
import PrefRemoteImage from "./fields/customs/PrefRemoteImage";
import PrefResetLocalData from "./fields/customs/PrefResetLocalData";
import PrefWorkHours from "./fields/customs/PrefWorkHours";

import PrefMixin from "./mixins/PrefMixin";

import { BmFormGroup, BmLabelIcon } from "@bluemind/styleguide";

export default {
    name: "PrefCategory",
    components: {
        BmFormGroup,
        BmLabelIcon,
        PrefFieldCheck,
        PrefFieldChoice,
        PrefFieldComboBox,
        PrefFieldSelect,
        PrefFieldSwitch,
        PrefAllDayEventReminder,
        PrefAlwaysShowQuota,
        PrefAutomaticReply,
        PrefEmailsForwarding,
        PrefEnableNotifications,
        PrefEventReminder,
        PrefIMSetPhonePresence,
        PrefManageIdentities,
        PrefManageMyCalendars,
        PrefPassword,
        PrefRemoteImage,
        PrefResetLocalData,
        PrefWorkHours
    },
    mixins: [PrefMixin],
    props: {
        section: {
            type: Object,
            required: true
        },
        category: {
            type: Object,
            required: true
        },
        localUserSettings: {
            type: Object,
            required: true
        }
    },
    computed: {
        filteredFieldGroups() {
            return this.category.groups.filter(
                group => !Object.prototype.hasOwnProperty.call(group, "condition") || group.condition
            );
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-category {
    .pref-category {
        padding-left: 4rem;
        padding-right: 4rem;
        border-bottom: 1px solid $light;
    }
    .available-soon {
        color: white;
        background-color: $yellow;
        &::before,
        &::after {
            content: "\00a0";
        }
    }

    .pref-field-combobox,
    .bm-form-time-picker,
    .bm-form-select,
    .bm-form-input,
    .bm-rich-editor {
        width: 24rem !important;
    }

    .b-calendar .bm-form-select {
        width: unset !important;
    }
}
</style>
