import { useState } from "react";
import api from "../services/api";

function UploadPanel({ onUploaded }) {
  const [mode, setMode] = useState("file"); // file | zip | snippet
  const [projectName, setProjectName] = useState("");
  const [files, setFiles] = useState(null);
  const [zipFile, setZipFile] = useState(null);
  const [fileName, setFileName] = useState("");
  const [code, setCode] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const reset = () => {
    setProjectName("");
    setFiles(null);
    setZipFile(null);
    setFileName("");
    setCode("");
  };

  const handleUpload = async () => {
    setError("");

    if (mode !== "snippet" && !projectName) {
      setError("Enter a project name");
      return;
    }

    setLoading(true);
    try {
      if (mode === "file") {
        if (!files || files.length === 0) {
          setError("Select at least one .java file");
          setLoading(false);
          return;
        }
        const form = new FormData();
        form.append("projectName", projectName);
        for (const f of files) form.append("files", f);
        await api.post("/projects/upload/files", form);
      } else if (mode === "zip") {
        if (!zipFile) {
          setError("Select a .zip file");
          setLoading(false);
          return;
        }
        const form = new FormData();
        form.append("projectName", projectName);
        form.append("file", zipFile);
        await api.post("/projects/upload/zip", form);
      } else {
        if (!projectName || !fileName || !code) {
          setError("Fill project name, file name, and code");
          setLoading(false);
          return;
        }
        await api.post("/projects/snippet", { projectName, fileName, code });
      }
      reset();
      onUploaded();
    } catch (err) {
      setError(err.response?.data?.message || "Upload failed");
    } finally {
      setLoading(false);
    }
  };

  const tabClass = (m) =>
    `px-4 py-2 text-sm rounded-lg transition ${
      mode === m
        ? "bg-emerald-500 text-white"
        : "bg-slate-700 text-slate-300 hover:bg-slate-600"
    }`;

  return (
    <div className="bg-slate-800 rounded-2xl border border-slate-700 p-6">
      <h2 className="text-lg font-semibold text-white mb-4">Submit Code</h2>

      <div className="flex gap-2 mb-5">
        <button className={tabClass("file")} onClick={() => setMode("file")}>
          Java Files
        </button>
        <button className={tabClass("zip")} onClick={() => setMode("zip")}>
          ZIP Project
        </button>
        <button className={tabClass("snippet")} onClick={() => setMode("snippet")}>
          Paste Snippet
        </button>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/30 text-red-400 text-sm rounded-lg px-4 py-2 mb-4">
          {error}
        </div>
      )}

      {mode !== "snippet" && (
        <input
          type="text"
          value={projectName}
          onChange={(e) => setProjectName(e.target.value)}
          placeholder="Project name"
          className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white mb-4 focus:outline-none focus:border-emerald-500"
        />
      )}

      {mode === "file" && (
        <input
          type="file"
          accept=".java"
          multiple
          onChange={(e) => setFiles(e.target.files)}
          className="w-full text-slate-300 text-sm mb-4 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:bg-slate-700 file:text-white"
        />
      )}

      {mode === "zip" && (
        <input
          type="file"
          accept=".zip"
          onChange={(e) => setZipFile(e.target.files[0])}
          className="w-full text-slate-300 text-sm mb-4 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:bg-slate-700 file:text-white"
        />
      )}

      {mode === "snippet" && (
        <>
          <input
            type="text"
            value={projectName}
            onChange={(e) => setProjectName(e.target.value)}
            placeholder="Project name"
            className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white mb-3 focus:outline-none focus:border-emerald-500"
          />
          <input
            type="text"
            value={fileName}
            onChange={(e) => setFileName(e.target.value)}
            placeholder="File name (e.g. Main.java)"
            className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white mb-3 focus:outline-none focus:border-emerald-500"
          />
          <textarea
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder="Paste your Java code here..."
            rows={8}
            className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-white font-mono text-sm mb-4 focus:outline-none focus:border-emerald-500"
          />
        </>
      )}

      <button
        onClick={handleUpload}
        disabled={loading}
        className="w-full bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-white font-medium rounded-lg py-2 transition"
      >
        {loading ? "Uploading..." : "Upload & Save"}
      </button>
    </div>
  );
}

export default UploadPanel;