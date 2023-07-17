module.exports = {
    env: {
        browser: true,
        es6: true,
        jest: true,
        node: true
    },
    extends: ["eslint:recommended", "plugin:vue/recommended", "@vue/prettier"],
    parserOptions: {
        parser: {
            ts: "@typescript-eslint/parser",
            js: "babel-eslint"
        }
    },
    ignorePatterns: ["**/target/", "**/generated/"],
    overrides: [
        {
            files: ["**/*.ts", "**/*.tsx"],
            extends: [
                "eslint:recommended",
                "plugin:@typescript-eslint/eslint-recommended",
                "plugin:@typescript-eslint/recommended",
                "plugin:vue/recommended",
                "@vue/prettier"
            ]
        },
        {
            files: ["**/*.vue"],
            rules: {
                "no-unused-vars": "off"
            }
        }
    ],
    rules: {
        "vue/multi-word-component-names": 0
    }
};
