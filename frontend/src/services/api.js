import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8081/api";
const api = axios.create({
  baseURL: API_BASE_URL,
});

// Attach JWT token to every request automatically
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// If token expires or is invalid, kick back to login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 403) {
      localStorage.removeItem("token");
    }
    return Promise.reject(error);
  }
);

export default api;