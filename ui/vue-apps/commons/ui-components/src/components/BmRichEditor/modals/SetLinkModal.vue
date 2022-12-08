<template>
    <bm-modal
        ref="set-link-modal"
        centered
        :title="$t('styleguide.rich_editor.link.tooltip')"
        :cancel-title="$t('common.cancel')"
        :ok-title="isNew ? $t('common.create') : $t('common.edit')"
        :ok-disabled="okDisabled"
        @ok="setLink"
        @hidden="$emit('close', { url, text, mustCreate })"
    >
        <bm-form @keydown.enter="setLinkAndClose">
            <bm-form-group :label="$t('styleguide.rich_editor.link.text')" label-for="displayAs">
                <bm-form-input id="displayAs" v-model="text" required />
            </bm-form-group>
            <bm-form-group :label="$t('styleguide.rich_editor.link.new')" label-for="url">
                <bm-form-input id="url" v-model="url" required />
            </bm-form-group>
        </bm-form>
    </bm-modal>
</template>

<script>
import BmForm from "../../form/BmForm";
import BmFormGroup from "../../form/BmFormGroup";
import BmFormInput from "../../form/BmFormInput";
import BmModal from "../../modals/BmModal";

export default {
    name: "SetLinkModal",
    components: { BmForm, BmFormGroup, BmFormInput, BmModal },
    props: {
        editor: {
            type: Object,
            required: true
        },
        initLink: {
            type: Object,
            default: () => ({ url: "", text: "" })
        },
        initText: {
            type: String,
            default: ""
        }
    },
    data() {
        return { url: "", text: "", isNew: true, mustCreate: false };
    },
    computed: {
        okDisabled() {
            return this.url.trim() === "";
        }
    },
    mounted() {
        this.url = this.initLink.url;
        this.text = this.initLink.text;
        this.isNew = this.url === "";
        this.$refs["set-link-modal"].show();
    },
    methods: {
        setLinkAndClose() {
            this.setLink();
            this.$refs["set-link-modal"].hide();
        },
        setLink() {
            if (!this.url.startsWith("http://") && !this.url.startsWith("https://")) {
                this.url = "http://" + this.url;
            }
            if (this.text.trim() === "") {
                this.text = this.url;
            }
            this.mustCreate = true;
        }
    }
};
</script>
