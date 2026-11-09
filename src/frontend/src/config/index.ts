const runtimeConfig = window.ENV_CONFIG ?? {};

export const API_URL: string =
  runtimeConfig.REACT_APP_API_URL || import.meta.env.VITE_API_URL || 'http://localhost:8080';
