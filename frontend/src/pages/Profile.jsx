import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";

function Profile() {
  const navigate = useNavigate();
  const [name, setName] = useState(localStorage.getItem("name") || "");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [nameMsg, setNameMsg] = useState("");
  const [pwMsg, setPwMsg] = useState("");

  const updateName = async () => {
    setNameMsg("");
    try {
      const res = await api.put("/user/profile", { name });
      localStorage.setItem("name", res.data.name);
      setNameMsg("Profile updated successfully");
    } catch (err) {
      setNameMsg(err.response?.data?.message || "Update failed");
    }
  };

  const resetPassword = async () => {
    setPwMsg("");
    if (!currentPassword || !newPassword) {
      setPwMsg("Fill both password fields");
      return;
    }
    try {
      await api.put("/user/reset-password", { currentPassword, newPassword });
      setPwMsg("Password reset successfully");
      setCurrentPassword("");
      setNewPassword("");
    } catch (err) {
      setPwMsg(err.response?.data?.message || "Reset failed");
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-white">
      <nav className="flex items-center justify-between px-8 py-4 border-b border-slate-700">
        <h1 className="text-xl font-bold text-emerald-400">
          AI Code Review Assistant
        </h1>
        <button
          onClick={() => navigate("/dashboard")}
          className="bg-slate-700 hover:bg-slate-600 text-sm px-3 py-1.5 rounded-lg transition"
        >
          Back to Dashboard
        </button>
      </nav>

      <div className="max-w-lg mx-auto p-8 space-y-6">
        <div className="bg-slate-800 rounded-2xl border border-slate-700 p-6">
          <h2 className="text-lg font-semibold mb-4">Update Profile</h2>
          {nameMsg && (
            <div className="text-emerald-400 text-sm mb-3">{nameMsg}</div>
          )}
          <label className="block text-sm text-slate-300 mb-1">Name</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white mb-4 focus:outline-none focus:border-emerald-500"
          />
          <button
            onClick={updateName}
            className="bg-emerald-500 hover:bg-emerald-600 text-white text-sm font-medium rounded-lg py-2 px-4 transition"
          >
            Save Changes
          </button>
        </div>

        <div className="bg-slate-800 rounded-2xl border border-slate-700 p-6">
          <h2 className="text-lg font-semibold mb-4">Reset Password</h2>
          {pwMsg && <div className="text-emerald-400 text-sm mb-3">{pwMsg}</div>}
          <label className="block text-sm text-slate-300 mb-1">
            Current Password
          </label>
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white mb-4 focus:outline-none focus:border-emerald-500"
          />
          <label className="block text-sm text-slate-300 mb-1">
            New Password
          </label>
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white mb-4 focus:outline-none focus:border-emerald-500"
          />
          <button
            onClick={resetPassword}
            className="bg-emerald-500 hover:bg-emerald-600 text-white text-sm font-medium rounded-lg py-2 px-4 transition"
          >
            Reset Password
          </button>
        </div>
      </div>
    </div>
  );
}

export default Profile;