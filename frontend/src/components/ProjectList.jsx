function ProjectList({ projects, selectedId, onSelect, onDelete }) {
  if (projects.length === 0) {
    return (
      <div className="bg-slate-800 rounded-2xl border border-slate-700 p-6">
        <h2 className="text-lg font-semibold text-white mb-2">Your Projects</h2>
        <p className="text-slate-400 text-sm">
          No projects yet. Upload some code to get started.
        </p>
      </div>
    );
  }

  return (
    <div className="bg-slate-800 rounded-2xl border border-slate-700 p-6">
      <h2 className="text-lg font-semibold text-white mb-4">Your Projects</h2>
      <div className="space-y-2">
        {projects.map((p) => (
          <div
            key={p.id}
            onClick={() => onSelect(p)}
            className={`cursor-pointer rounded-lg px-4 py-3 border transition ${
              selectedId === p.id
                ? "bg-slate-700 border-emerald-500"
                : "bg-slate-900 border-slate-700 hover:border-slate-500"
            }`}
          >
            <div className="flex items-center justify-between">
              <div>
                <p className="text-white font-medium">{p.projectName}</p>
                <p className="text-slate-400 text-xs mt-1">
                  {p.uploadType} · {p.fileCount} file(s)
                </p>
              </div>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onDelete(p.id);
                }}
                className="text-red-400 hover:text-red-300 text-xs px-2 py-1 rounded"
              >
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default ProjectList;