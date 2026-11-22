// Expo SDK 52+ detects monorepos automatically (watchFolders / nodeModulesPaths).
const { getDefaultConfig } = require("expo/metro-config");

module.exports = getDefaultConfig(__dirname);
