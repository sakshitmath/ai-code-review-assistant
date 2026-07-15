import { useNavigate } from "react-router-dom";

function Dashboard() {
  const navigate = useNavigate();
  const name = localStorage.getItem("name") || "User";

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("name");
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-slate-900 text-white">
      <nav className="flex items-center justify-between px-8 py-4 border-b border-slate-700">
        <h1 className="text-xl font-bold text-emerald-400">
          AI Code Review Assistant
        </h1>
        <div className="flex items-center gap-4">
          <span className="text-slate-300 text-sm">Hi, {name}</span>
          <button
            onClick={handleLogout}
            className="bg-slate-700 hover:bg-slate-600 text-sm px-3 py-1.5 rounded-lg transition"
          >
            Logout
          </button>
        </div>
      </nav>

      <div className="p-8">
        <h2 className="text-2xl font-semibold mb-2">Dashboard</h2>
        <p className="text-slate-400">
          Login works. We'll build the upload and review UI next.
        </p>
      </div>
    </div>
  );
}

export default Dashboard;