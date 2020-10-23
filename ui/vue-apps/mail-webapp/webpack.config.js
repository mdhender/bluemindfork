const path = require("path");
const { merge } = require("webpack-merge");
const prod = require("./node_modules/@bluemind/conf/webpack.prod.js");
const dev = require("./node_modules/@bluemind/conf/webpack.dev.js");

const conf = {
    entry: {
        "service-worker": "./src/service-worker/service-worker.js",
        "js/net.bluemind.webapp.mail": "./src/run.js"
    },
    output: {
        path: path.resolve(__dirname, "./web-resources"),
        filename: "[name].js"
    },
    module: {
        rules: [{ test: /\.ts?$/, use: "ts-loader", exclude: /node_modules/ }]
    },
    resolve: {
        extensions: [".ts"]
    }
};

module.exports = mode => {
    if (mode === "production") {
        return merge(prod, conf);
    }
    return merge(dev, conf);
};
