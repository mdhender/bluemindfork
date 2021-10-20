<template>
    <div :id="categoryId(section.code, category.code)" class="pref-category">
        <pref-group
            v-for="group in filteredFieldGroups"
            :key="group.title"
            :group="group"
            :local-user-settings="localUserSettings"
            @requestSave="$emit('requestSave')"
        />
    </div>
</template>

<script>
import PrefMixin from "./mixins/PrefMixin";
import PrefGroup from "./PrefGroup";

export default {
    name: "PrefCategory",
    components: { PrefGroup },
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
