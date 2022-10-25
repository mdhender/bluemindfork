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
    overrides: [
        {
            files: ["**/*.ts", "**/*.tsx"],
            extends: [
                "plugin:@typescript-eslint/eslint-recommended",
                "plugin:@typescript-eslint/recommended",
                "eslint:recommended",
                "plugin:vue/recommended",
                "@vue/prettier"
            ]
        }
    ]
};
