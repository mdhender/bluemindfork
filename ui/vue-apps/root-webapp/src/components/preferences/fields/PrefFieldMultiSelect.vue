<template>
    <bm-form-multi-select v-model="selected" class="pref-field-multi-select" v-bind="[...options]" @input="save" />
</template>

<script>
import { BmFormMultiSelect } from "@bluemind/styleguide";
import PrefFieldMixin from "../mixins/PrefFieldMixin";

export default {
    name: "PrefFieldMultiSelect",
    components: { BmFormMultiSelect },
    mixins: [PrefFieldMixin],
    data() {
        return {
            selected: this.fromSetting(this.localUserSettings[this.setting])
        };
    },
    created() {
        this.$watch(
            () => this.localUserSettings[this.setting],
            () => {
                this.selected = this.fromSetting(this.localUserSettings[this.setting]);
            }
        );
    },
    methods: {
        save() {
            this.localUserSettings[this.setting] = this.toSetting(this.selected);
        },
        fromSetting(settingValue) {
            return this.options.fromSetting
                ? this.options.fromSetting(settingValue)
                : settingValue.split(",").filter(Boolean);
        },
        toSetting(selected) {
            return this.options.toSetting ? this.options.toSetting(selected) : selected.join(",");
        }
    }
};
</script>
