import InheritTranslationsMixin from "../src/InheritTranslationsMixin";
import VueI18N from "vue-i18n";
import { mount, createLocalVue } from "@vue/test-utils";

describe("InheritTranslationsMixin", () => {
    let localVue, ChildComponent, ParentComponent;
    const parentMessages = {
        en: {
            "my.parent.trad": "good",
            "same.key": "parent"
        },
        fr: {
            "my.parent.trad": "bien"
        }
    };
    const childMessages = {
        en: {
            "my.child.trad": "bad",
            "same.key": "child"
        },
        fr: {
            "my.child.trad": "mal"
        }
    };
    beforeEach(() => {
        localVue = createLocalVue();
        localVue.use(VueI18N);
        localVue.mixin(InheritTranslationsMixin);
        ChildComponent = {
            name: "ChildComponent",
            template: "<p>this is child component - {{ $t('my.child.trad') }} - {{ $t('same.key') }}</p>"
        };
        ParentComponent = {
            name: "ParentComponent",
            components: { ChildComponent },
            template: "<div><p>this is parent component {{ $t('my.parent.trad') }}</p><child-component /></div>",
            i18n: {
                locale: "en",
                fallbackLocale: "en"
            }
        };
    });

    function mountAndGetChild() {
        // why should we use sync: false -->https://github.com/vuejs/vue-test-utils/issues/1130
        return mount(ParentComponent, { localVue, sync: false }).findComponent(ChildComponent).vm;
    }

    test("child component does not define its i18n messages but still inherits i18n messages from its parent", () => {
        ParentComponent.i18n.messages = parentMessages;
        const childComp = mountAndGetChild();

        expect(childComp.$i18n.messages["en"]).toMatchObject(parentMessages["en"]);
        expect(childComp.$i18n.locale).toEqual("en");
        expect(childComp.$i18n.fallbackLocale).toEqual("en");
    });

    test("child component defines its i18n messages and inherits its parent i18n messages", () => {
        ParentComponent.i18n.messages = parentMessages;
        ChildComponent.componentI18N = { messages: childMessages };
        const parentWrapper = mount(ParentComponent, { localVue, sync: false });
        const childComp = parentWrapper.findComponent(ChildComponent).vm;

        expect(childComp.$i18n.messages["en"]).toMatchObject({ "my.parent.trad": "good" });
        expect(childComp.$i18n.locale).toEqual("en");
        expect(childComp.$i18n.fallbackLocale).toEqual("en");
        expect(parentWrapper.text()).toContain("bad - child");
        expect(parentWrapper.text()).toContain("good");
    });

    test("child component defining its own i18n (no messages from parents)", () => {
        ChildComponent.componentI18N = { messages: childMessages };
        const parentWrapper = mount(ParentComponent, { localVue, sync: false });
        expect(parentWrapper.text()).toContain("bad - child");
    });

    test("child component i18n messages overrides parent messages if a same key for a same language is defined", () => {
        ChildComponent["componentI18N"] = { messages: childMessages };
        const childComp = mountAndGetChild();
        expect(childComp.$i18n.messages["en"]["same.key"]).toEqual("child");
    });

    test("3 levels of components : root with no messages, parent with messages, child with messages", () => {
        ChildComponent.componentI18N = { messages: childMessages };

        const ParentComponent2 = {
            name: "ParentComponent2",
            components: { ChildComponent },
            template: "<div><p>this is parent component {{ $t('my.parent.trad') }}</p><child-component /></div>",
            componentI18N: {
                messages: parentMessages
            }
        };

        const RootComponent = {
            name: "RootComponent",
            components: { ParentComponent2 },
            template: "<div><p>this is root component</p><parent-component-2 /></div>",
            i18n: {
                locale: "en",
                fallbackLocale: "en"
            }
        };

        const rootComp = mount(RootComponent, { localVue, sync: false });

        expect(rootComp.text()).toContain("bad - child");
        expect(rootComp.text()).toContain("good");
    });

    test("Child component inherits right locale", () => {
        ChildComponent.componentI18N = { messages: childMessages };
        ParentComponent.i18n.locale = "fr";
        const parentComp = mount(ParentComponent, { localVue, sync: false });
        expect(parentComp.text()).toContain("mal");
    });

    test("Child component with an empty key object", () => {
        ChildComponent.componentI18N = { messages: { en: {} } };
        const childComp = mount(ParentComponent, { localVue, sync: false }).findComponent(ChildComponent);
        expect(childComp).toBeDefined();
    });
});
