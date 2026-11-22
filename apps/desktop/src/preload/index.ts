import { contextBridge } from "electron";

// API mínima expuesta al renderer; ampliar con ipcRenderer según se necesite.
contextBridge.exposeInMainWorld("solidaria", {
  platform: process.platform,
});
