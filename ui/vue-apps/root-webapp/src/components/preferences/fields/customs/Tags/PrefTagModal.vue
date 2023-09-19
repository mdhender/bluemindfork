<template>
    <bm-modal
        :id="$attrs['id']"
        ref="pref-tag-modal-bm-modal"
        content-class="pref-tag-modal"
        size="sm"
        :title="tag.id ? $t('preferences.general.tags.edit') : $t('preferences.general.tags.create')"
        :cancel-title="$t('common.cancel')"
        :ok-title="tag.id ? $t('common.edit') : $t('common.create')"
        :ok-disabled="okDisabled"
        @ok="save"
        @shown="init"
    >
        <bm-form class="mt-4" @submit.prevent="submit">
            <bm-form-group id="label-group" :label="$t('preferences.general.tags.modal.label')" label-for="label">
                <bm-form-input id="label" ref="label-input" v-model="tag_.label" required />
            </bm-form-group>
            <bm-form-group id="color-group" label-for="color" :label="$t('preferences.general.tags.modal.color')">
                <bm-form-color-picker
                    id="color"
                    v-model="tag_.color"
                    type="text"
                    required
                    pick-default
                    :colors="tagColors"
                />
            </bm-form-group>
        </bm-form>
    </bm-modal>
</template>

<script>
import { BmForm, BmFormColorPicker, BmFormGroup, BmFormInput, BmModal } from "@bluemind/ui-components";
import tagColors from "./tagColors";

export default {
    name: "PrefTagModal",
    components: { BmForm, BmFormColorPicker, BmFormGroup, BmFormInput, BmModal },
    props: {
        tag: {
            type: Object,
            default: () => ({ id: "", label: "", color: "" })
        }
    },
    data() {
        return { tag_: {}, tagColors };
    },
    computed: {
        okDisabled() {
            return (
                !this.tag_.label?.trim() ||
                !this.tag_.color ||
                (this.tag_.label === this.tag.label && this.tag_.color === this.tag.color)
            );
        }
    },
    watch: {
        tag: {
            handler(value) {
                this.tag_ = { ...value };
            },
            immediate: true
        }
    },
    methods: {
        init() {
            this.$refs["label-input"].focus();
        },
        show() {
            this.$refs["pref-tag-modal-bm-modal"].show();
        },
        hide() {
            this.$refs["pref-tag-modal-bm-modal"].hide();
        },
        save() {
            this.$emit("update:tag", this.tag_);
        },
        submit() {
            if (!this.okDisabled) {
                this.save();
                this.hide();
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-tag-modal {
    #color {
        border: 1px solid $neutral-fg-lo3;
    }
}
</style>
