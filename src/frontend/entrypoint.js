"use strict";

const dotenv = require("dotenv");
const fs = require("fs");

let envFilePath = ".env"
let configRoot = "ENV_CONFIG"
let outputFile = "./public/env-config.js"

for (let i = 2; i < process.argv.length; i++) {
  switch (process.argv[i]) {
    case "-e":
      envFilePath = process.argv[++i]
      break;
    case "-o":
      outputFile = process.argv[++i]
      break;
    case "-c":
      configRoot = process.argv[++i]
      break;
    default:
      throw Error(`unknown option ${process.argv[i]}`)
  }
}

if (fs.existsSync(envFilePath)) {
  console.log(`Loading environment file from '${envFilePath}'`)
  
  dotenv.config({
    path: envFilePath
  })    
}

console.log(`Generating JS configuration output to: ${outputFile}`)

if (!/^[A-Za-z_$][A-Za-z0-9_$]*$/.test(configRoot)) {
  throw Error("Configuration object name must be a JavaScript identifier");
}

// Explicitly allow only public browser settings. Never export arbitrary
// REACT_APP_* variables, model keys, or database credentials.
const publicConfig = {};
if (process.env.REACT_APP_API_URL) {
  publicConfig.REACT_APP_API_URL = process.env.REACT_APP_API_URL;
}
fs.writeFileSync(outputFile, `window.${configRoot} = ${JSON.stringify(publicConfig)};\n`);
