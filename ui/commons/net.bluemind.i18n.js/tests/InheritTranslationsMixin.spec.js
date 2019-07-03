import InheritTranslationsMixin from "../src/InheritTranslationsMixin";
import VueI18N from "vue-i18n";
import Vue from "vue";
import { mount } from "@vue/test-utils";

Vue.use(VueI18N);
Vue.mixin(InheritTranslationsMixin);

describe('InheritTranslationsMixin', () => {
    const ChildComponent  = {
        name: "ChildComponent",
        template: "<p>this is child component - $t('my.child.trad') - $t('same.key')</p>",
    };
    const ParentComponent = {
        name: "ParentComponent",
        components: { ChildComponent },
        template: "<div><p>this is parent component {{ $t('my.parent.trad') }}</p><child-component /></div>"
    };

    const parentMessages = {
        "en": {
            "my.parent.trad": "good",
            "same.key": "parent"
        },
        "fr": {
            "my.parent.trad": "bien"
        }
    };
    const parentI18n = new VueI18N({
        locale: 'en',
        fallbackLocale: 'en',
        messages: parentMessages
    });
    const childI18n = new VueI18N({
        locale: 'en',
        fallbackLocale: 'en',
        messages: {
            "en": {
                "my.child.trad": "bad",
                "same.key": "child"
            },
            "fr": {
                "my.child.trad": "mal"
            }
        }
    });

    function mountAndGetChild() {
        return mount(ParentComponent, { i18n: parentI18n }).vm.$children[0];
    }


    test('child component dont defining its i18n still inherits from parent i18n', () => {
        const childComp = mountAndGetChild();
        
        expect(childComp.$i18n.messages["en"]).toMatchObject(parentMessages["en"]);
        expect(childComp.$i18n.locale).toEqual('en');
        expect(childComp.$i18n.fallbackLocale).toEqual('en');
    });

    test('child component defining its i18n still inherits from parent i18n', () => {
        ChildComponent["i18n"] = childI18n;
        const childComp = mountAndGetChild();
        
        expect(childComp.$i18n.messages["en"]).toMatchObject({"my.parent.trad": "good"});
        expect(childComp.$i18n.locale).toEqual('en');
        expect(childComp.$i18n.fallbackLocale).toEqual('en');
    });

    test('if a same key for a same language is defined, child value is used', () => {
        ChildComponent["i18n"] = childI18n;
        const childComp = mountAndGetChild();
        
        expect(childComp.$i18n.messages["en"]["same.key"]).toEqual("child");
    });


});
