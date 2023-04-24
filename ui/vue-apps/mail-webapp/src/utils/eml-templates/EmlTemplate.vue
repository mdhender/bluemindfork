<script>
import Vue from "vue";
import i18n from "@bluemind/i18n";
import Part from "./Part";

export default {
    components: { Part },
    props: { parameters: { type: Object, default: () => {} } },
    methods: {
        structure() {
            return this.$children?.length ? this.$children[0]?.structure() : undefined;
        }
    }
};

export function buildStructure(template, parameters = {}) {
    const EmlTemplate = Vue.extend({ extends: template, i18n });
    const emlTemplate = new EmlTemplate({ propsData: { parameters } });
    const element = document.createElement("div");
    emlTemplate.$mount(element);
    return emlTemplate.structure();
}
</script>
