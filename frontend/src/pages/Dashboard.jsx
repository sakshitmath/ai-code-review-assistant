import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";
import UploadPanel from "../components/UploadPanel";
import ProjectList from "../components/ProjectList";
import AnalysisPanel from "../components/AnalysisPanel";

function Dashboard() {
  const navigate = useNavigate();
  const name = localStorage.getItem("name") || "User";

  const [projects, setProjects] = useState([]);
  const [selected, setSelected] = useState(null);
  const [search, setSearch] = useState("");

  const loadProjects = useCallback(async () => {
    try {
      const res = await api.get("/projects");
      setProjects(res.data);
    } catch {
      setProjects([]);
    }
  }, []);

  useEffect(() => {
    loadProjects();
  }, [loadProjects]);

  const handleSearch = async () => {
    if (!search.trim()) {
      loadProjects();
      return;
    }
    try {
      const res = await api.get(`/projects/search?keyword=${encodeURIComponent(search)}`);
      setProjects(res.data);
    } catch {
      setProjects([]);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Delete this project?")) return;
    try {
      await api.delete(`/projects/${id}`);
      if (selected?.id === id) setSelected(null);
      loadProjects();
    } catch {
      // ignore
    }
  };

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

      <div className="max-w-6xl mx-auto p-8 grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="space-y-6">
          <UploadPanel onUploaded={loadProjects} />

          <div className="flex gap-2">
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSearch()}
              placeholder="Search projects..."
              className="flex-1 bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-emerald-500"
            />
            <button
              onClick={handleSearch}
              className="bg-slate-700 hover:bg-slate-600 text-sm px-4 rounded-lg transition"
            >
              Search
            </button>
          </div>

          <ProjectList
            projects={projects}
            selectedId={selected?.id}
            onSelect={setSelected}
            onDelete={handleDelete}
          />
        </div>

       <div>
          {selected ? (
            <AnalysisPanel key={selected.id} project={selected} />
          ) : (
            <div className="bg-slate-800 rounded-2xl border border-slate-700 p-6">
              <p className="text-slate-400 text-sm">
                Select a project to run analysis.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default Dashboard;