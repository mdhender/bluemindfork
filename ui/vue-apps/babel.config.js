module.exports = function (api, plugins = []) {
    api.cache(true);

    const presets = ["@babel/preset-env", "@babel/preset-typescript"];

    if (process.env.NODE_ENV === "test") {
        plugins.push("require-context-hook");
    }

    return {
        presets,
        plugins
    };
};
