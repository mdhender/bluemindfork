<template>
    <bm-modal
        :id="$attrs['id']"
        ref="pref-tag-modal-bm-modal"
        class="pref-tag-modal"
        centered
        :title="tag.id ? $t('preferences.general.tags.edit') : $t('preferences.general.tags.create')"
        :cancel-title="$t('common.cancel')"
        :ok-title="tag.id ? $t('common.edit') : $t('common.create')"
        :ok-disabled="okDisabled"
        @ok="save"
        @shown="init"
    >
        <bm-form class="mt-4" @submit.prevent="submit">
            <bm-form-group id="label-group" :label="$t('preferences.general.tags.modal.label')" label-for="label">
                <bm-form-input id="label" ref="label-input" v-model="tag.label" required />
            </bm-form-group>
            <bm-form-group id="color-group" label-for="color" :label="$t('preferences.general.tags.modal.color')">
                <bm-form-color-picker
                    id="color"
                    v-model="tag.color"
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
import { BmForm, BmFormColorPicker, BmFormGroup, BmFormInput, BmModal } from "@bluemind/styleguide";
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
        return { tagValueOnShow: {}, tagColors };
    },
    computed: {
        okDisabled() {
            return (
                !this.tag.label ||
                this.tag.label.trim() === "" ||
                !this.tag.color ||
                this.tag.color === "" ||
                (this.tag.label === this.tagValueOnShow.label && this.tag.color === this.tagValueOnShow.color)
            );
        }
    },
    methods: {
        init() {
            this.tagValueOnShow = { ...this.tag };
            this.$refs["label-input"].focus();
        },
        show() {
            this.$refs["pref-tag-modal-bm-modal"].show();
        },
        hide() {
            this.$refs["pref-tag-modal-bm-modal"].hide();
        },
        save() {
            this.$emit("updateTag", { id: this.tag.id, label: this.tag.label, color: this.tag.color });
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
