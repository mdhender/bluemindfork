export default {
    // if there are duplicate keys between both params, translation2 values overwrite translation1 values.
    mergeTranslations(translation1, translation2) {
        let res = {};
        for (let lang in translation1) {
            let tmp = translation1[lang];
            if (translation2[lang]) {
                tmp = { ...translation1[lang], ...translation2[lang] };
            }
            res[lang] = tmp;
        }
        for (let lang in translation2) {
            if (!res[lang]) {
                res[lang] = translation2[lang];
            }
        }
        return res;
    },
    loadTranslations(context) {
        let res = {};
        context.keys().forEach(key => {
            const adapted = {};
            adapted[key.substring(2, 4)] = { ...context(key) }; // extracting lang ("en" for example)
            res = this.mergeTranslations(res, adapted);
        });
        return res;
    }
};
