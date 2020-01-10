xdescribe('MailMessageContent', ()=>{    
    xtest('should display time in fr format with our custom i18n mixin', () => {
    //     Vue.mixin(InheritTranslationsMixin);
    
        //     const dateTimeFormats = {
        //         'fr': {
        //             short: {
        //                 hour: '2-digit', minute: '2-digit'
        //             }
        //         }
        //     };
        //     const ChildComponent  = {
        //         name: "ChildComponent",
        //         template: "<div><p>{{ $t('my.child.trad') }} - Date {{ $d(new Date(2019,1,1,1,0,0),'short') }}</p></div>",
        //     };
    
        //     const ParentComponent = {
        //         name: "ParentComponent",
        //         components: { ChildComponent },
        //         template: "<div><p>this is parent component</p><child-component /></div>"
        //     };
    
        //     const i18n = new VueI18N({
        //         locale: 'fr',
        //         fallbackLocale: 'en',
        //         dateTimeFormats,
        //         messages: {
        //             "fr": {
        //                 "my.child.trad": "mal"
        //             }
        //         }
        //     });
    
        //     const mountedComponent = mount(ParentComponent, { i18n });
    
    //     expect(mountedComponent.text()).toContain("mal");
    //     expect(mountedComponent.text()).toContain("01:00");
    //     expect(mountedComponent.text()).not.toContain("01:10");
    });
    
    // test('should display time in fr format with our custom i18n mixin', () => {
    //     Vue.mixin(InheritTranslationsMixin);
    
    //     const dateTimeFormats = {
    //         'fr': {
    //             short: {
    //                 hour: '2-digit', minute: '2-digit'
    //             }
    //         },
    //         'en': {
    //             short: {
    //                 hour: '2-digit', minute: '2-digit'
    //             }
    //         }
    //     };
    //     const ChildComponent  = {
    //         name: "ChildComponent",
    //         template: "<div><p>{{ $t('my.child.trad') }} - Date {{ $d(new Date(2019,1,1,1,0,0),'short') }}</p></div>",
    //         componentI18N: { messages: {
    //             "fr": {
    //                 "my.child.trad": "surcharger"
    //             }
    //         }},
    //     };
    
    //     const ParentComponent = {
    //         name: "ParentComponent",
    //         components: { ChildComponent },
    //         template: "<div><p>this is parent component</p><child-component /></div>"
    //     };
    
    //     const i18n = new VueI18N({
    //         locale: 'fr',
    //         fallbackLocale: 'en',
    //         dateTimeFormats,
    //         messages: {
    //             "fr": {
    //                 "my.child.trad": "mal"
    //             }
    //         }
    //     });
    
    //     const mountedComponent = mount(ParentComponent, { i18n });
    
    //     expect(mountedComponent.text()).toContain("surcharger");
    //     expect(mountedComponent.text()).toContain("01:00");
    //     expect(mountedComponent.text()).not.toContain("01:10");
    // });
});