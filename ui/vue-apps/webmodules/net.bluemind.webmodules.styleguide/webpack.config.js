const path = require("path");
const VueLoaderPlugin = require("vue-loader/lib/plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

module.exports = {
    mode: "production",
    plugins: [new VueLoaderPlugin(), new MiniCssExtractPlugin({ filename: "css/styleguide.css" })],
    module: {
        rules: [
            {
                test: /\.vue$/,
                loader: "vue-loader"
            },
            {
                test: /\.css$/,
                use: [MiniCssExtractPlugin.loader, "css-loader"]
            },
            {
                test: /\.scss$/,
                use: [MiniCssExtractPlugin.loader, "css-loader", "sass-loader"]
            },
            {
                test: /\.svg$/,
                loader: "svg-inline-loader"
            },
            {
                test: /\.png$/,
                loader: "url-loader"
            },
            {
                test: /\.js$/,
                loader: "babel-loader"
            },
            {
                test: /\.ts$/,
                use: ["babel-loader", "ts-loader"],
                exclude: [/node_modules/, /target/]
            },
            {
                enforce: "pre",
                test: /\.(js|vue)$/,
                loader: "eslint-loader",
                exclude: /node_modules/
            }
        ]
    },
    externals: {
        vue: "Vue"
    },
    resolve: {
        extensions: ["*", ".js", ".vue", ".json", ".css", ".scss", ".ts"]
    },
    devtool: "source-map",
    entry: "./index.js",
    output: {
        path: path.resolve(__dirname, "web-resources"),
        filename: "js/ui-components.js",
        library: "BmUiComponents",
        libraryTarget: "umd",
        clean: true
    }
};
