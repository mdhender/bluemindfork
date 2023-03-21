<script>
import Vue from "vue";
import { inject } from "@bluemind/inject";
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
    const EmlTemplate = Vue.extend({ extends: template, i18n: inject("i18n") });
    const emlTemplate = new EmlTemplate({ propsData: { parameters } });
    const element = document.createElement("root");
    emlTemplate.$mount(element);
    return emlTemplate.structure();
}
</script>
