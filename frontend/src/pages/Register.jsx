import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import api from "../services/api";

function Register() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleRegister = async () => {
    setError("");
    if (!name || !email || !password) {
      setError("Please fill all fields");
      return;
    }
    if (password.length < 6) {
      setError("Password must be at least 6 characters");
      return;
    }
    setLoading(true);
    try {
      const res = await api.post("/auth/register", { name, email, password });
      localStorage.setItem("token", res.data.token);
      localStorage.setItem("name", res.data.name);
      navigate("/dashboard");
    } catch (err) {
      setError(err.response?.data?.message || "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-slate-800 rounded-2xl shadow-xl p-8 border border-slate-700">
        <h1 className="text-2xl font-bold text-white mb-1">Create account</h1>
        <p className="text-slate-400 text-sm mb-6">
          Start reviewing your Java code
        </p>

        {error && (
          <div className="bg-red-500/10 border border-red-500/30 text-red-400 text-sm rounded-lg px-4 py-2 mb-4">
            {error}
          </div>
        )}

        <label className="block text-sm text-slate-300 mb-1">Name</label>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white mb-4 focus:outline-none focus:border-emerald-500"
          placeholder="Sakshi"
        />

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
          placeholder="At least 6 characters"
        />

        <button
          onClick={handleRegister}
          disabled={loading}
          className="w-full bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-white font-medium rounded-lg py-2 transition"
        >
          {loading ? "Creating..." : "Create account"}
        </button>

        <p className="text-slate-400 text-sm text-center mt-6">
          Already have an account?{" "}
          <Link to="/login" className="text-emerald-400 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}

export default Register;