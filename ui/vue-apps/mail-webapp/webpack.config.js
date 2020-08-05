const path = require("path");
const merge = require("webpack-merge");
const prod = require("./node_modules/@bluemind/conf/webpack.prod.js");
const dev = require("./node_modules/@bluemind/conf/webpack.dev.js");
const { InjectManifest } = require("workbox-webpack-plugin");

const conf = {
    entry: {
        "net.bluemind.webapp.mail": "./src/run.js"
    },
    output: {
        path: path.resolve(__dirname, "./web-resources"),
        filename: "js/[name].js"
    },
    plugins: [
        new InjectManifest({
            swSrc: "./src/service-worker/service-worker.js",
            maximumFileSizeToCacheInBytes: 200000000
        })
    ],
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
