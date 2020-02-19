module.exports = {
    env: {
        browser: true,
        es6: true,
        jest: true,
        node: true
    },
    extends: ["eslint:recommended", "plugin:vue/recommended", "@vue/prettier"],
    parserOptions: {
        parser: "babel-eslint"
    },
    rules: {
        eqeqeq: "warn",
        "no-console": ["warn"]
    }
};
