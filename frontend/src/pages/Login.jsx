import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import api from "../services/api";

function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async () => {
    setError("");
    if (!email || !password) {
      setError("Please enter email and password");
      return;
    }
    setLoading(true);
    try {
      const res = await api.post("/auth/login", { email, password });
      localStorage.setItem("token", res.data.token);
      localStorage.setItem("name", res.data.name);
      navigate("/dashboard");
    } catch (err) {
      setError(err.response?.data?.message || "Login failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-slate-800 rounded-2xl shadow-xl p-8 border border-slate-700">
        <h1 className="text-2xl font-bold text-white mb-1">Welcome back</h1>
        <p className="text-slate-400 text-sm mb-6">
          Sign in to AI Code Review Assistant
        </p>

        {error && (
          <div className="bg-red-500/10 border border-red-500/30 text-red-400 text-sm rounded-lg px-4 py-2 mb-4">
            {error}
          </div>
        )}

        <label className="block text-sm text-slate-300 mb-1">Email</label>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white mb-4 focus:outline-none focus:border-emerald-500"
          placeholder="you@example.com"
        />

        <label className="block text-sm text-slate-300 mb-1">Password</label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white mb-6 focus:outline-none focus:border-emerald-500"
          placeholder="••••••••"
        />

        <button
          onClick={handleLogin}
          disabled={loading}
          className="w-full bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-white font-medium rounded-lg py-2 transition"
        >
          {loading ? "Signing in..." : "Sign in"}
        </button>

        <p className="text-slate-400 text-sm text-center mt-6">
          Don't have an account?{" "}
          <Link to="/register" className="text-emerald-400 hover:underline">
            Register
          </Link>
        </p>
      </div>
    </div>
  );
}

export default Login;