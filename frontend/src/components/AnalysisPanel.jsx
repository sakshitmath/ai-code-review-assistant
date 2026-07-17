import { useState, useEffect } from "react";
import api from "../services/api";
import { Doughnut } from "react-chartjs-2";
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
} from "chart.js";

ChartJS.register(ArcElement, Tooltip, Legend);

function AnalysisPanel({ project }) {
  const [tab, setTab] = useState("static");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [review, setReview] = useState(null);
  const [complexity, setComplexity] = useState(null);
  const [docs, setDocs] = useState(null);
  const [history, setHistory] = useState([]);
  const [severityFilter, setSeverityFilter] = useState("ALL");

  const loadHistory = async () => {
    try {
      const res = await api.get(`/reviews/project/${project.id}`);
      setHistory(res.data);
    } catch {
      setHistory([]);
    }
  };

  useEffect(() => {
    loadHistory();
  }, [project.id]);

  const clearAll = () => {
    setReview(null);
    setComplexity(null);
    setDocs(null);
    setError("");
  };

  const runStatic = async () => {
    clearAll();
    setTab("static");
    setLoading(true);
    try {
      const res = await api.post(`/reviews/static/${project.id}`);
      setReview(res.data);
      loadHistory();
    } catch (err) {
      setError(err.response?.data?.message || "Static analysis failed");
    } finally {
      setLoading(false);
    }
  };

  const runAi = async () => {
    clearAll();
    setTab("ai");
    setLoading(true);
    try {
      const res = await api.post(`/reviews/ai/${project.id}`);
      setReview(res.data);
    } catch (err) {
      setError(err.response?.data?.message || "AI review failed");
    } finally {
      setLoading(false);
    }
  };

  const runComplexity = async () => {
    clearAll();
    setTab("complexity");
    setLoading(true);
    try {
      const res = await api.get(`/complexity/${project.id}`);
      setComplexity(res.data);
    } catch (err) {
      setError(err.response?.data?.message || "Complexity analysis failed");
    } finally {
      setLoading(false);
    }
  };

  const runDocs = async () => {
    clearAll();
    setTab("docs");
    setLoading(true);
    try {
      const res = await api.post(`/documentation/${project.id}`);
      setDocs(res.data.documentation);
    } catch (err) {
      setError(err.response?.data?.message || "Documentation failed");
    } finally {
      setLoading(false);
    }
  };
  const filteredFindings = review
    ? review.findings.filter(
        (f) => severityFilter === "ALL" || f.severity === severityFilter
      )
    : [];

  const severityColor = (sev) => {
    if (sev === "HIGH") return "text-red-400 bg-red-500/10 border-red-500/30";
    if (sev === "MEDIUM") return "text-amber-400 bg-amber-500/10 border-amber-500/30";
    return "text-sky-400 bg-sky-500/10 border-sky-500/30";
  };

  const btn = (label, fn, active) =>
    `px-3 py-2 text-sm rounded-lg transition ${
      active
        ? "bg-emerald-500 text-white"
        : "bg-slate-700 text-slate-300 hover:bg-slate-600"
    }`;

  const chartData = review && {
    labels: ["High", "Medium", "Low"],
    datasets: [
      {
        data: [
          review.issuesBySeverity?.HIGH || 0,
          review.issuesBySeverity?.MEDIUM || 0,
          review.issuesBySeverity?.LOW || 0,
        ],
        backgroundColor: ["#f87171", "#fbbf24", "#38bdf8"],
        borderColor: "#0f172a",
        borderWidth: 2,
      },
    ],
  };

  return (
    <div className="bg-slate-800 rounded-2xl border border-slate-700 p-6">
      <h2 className="text-lg font-semibold text-white mb-1">
        {project.projectName}
      </h2>
      <p className="text-slate-400 text-xs mb-4">
        {project.uploadType} · {project.fileCount} file(s)
      </p>

      <div className="grid grid-cols-2 gap-2 mb-5">
        <button className={btn("s", runStatic, tab === "static")} onClick={runStatic}>
          Static Analysis
        </button>
        <button className={btn("a", runAi, tab === "ai")} onClick={runAi}>
          AI Review
        </button>
        <button className={btn("c", runComplexity, tab === "complexity")} onClick={runComplexity}>
          Complexity
        </button>
        <button className={btn("d", runDocs, tab === "docs")} onClick={runDocs}>
          Generate Docs
        </button>
      </div>

      {loading && (
        <div className="text-slate-400 text-sm py-8 text-center">
          Running analysis... this may take a few seconds.
        </div>
      )}

      {error && (
        <div className="bg-red-500/10 border border-red-500/30 text-red-400 text-sm rounded-lg px-4 py-2 mb-4">
          {error}
        </div>
      )}

      {/* REVIEW RESULTS (static or AI) */}
      {!loading && review && (
        <div>
          <div className="flex items-center gap-6 mb-5">
            <div className="w-28 h-28">
              <Doughnut
                data={chartData}
                options={{ plugins: { legend: { display: false } } }}
              />
            </div>
            <div>
              <p className="text-3xl font-bold text-white">
                {review.reviewScore}
                <span className="text-base text-slate-400">/100</span>
              </p>
              <p className="text-slate-400 text-sm">Quality Score</p>
              <p className="text-slate-300 text-sm mt-1">
                {review.totalIssues} issue(s)
              </p>
            </div>
          </div>

          {review.summary && (
            <p className="text-slate-300 text-sm bg-slate-900 rounded-lg p-3 mb-4">
              {review.summary}
            </p>
          )}

          <div className="flex items-center gap-2 mb-3">
            <span className="text-slate-400 text-xs">Filter:</span>
            <select
              value={severityFilter}
              onChange={(e) => setSeverityFilter(e.target.value)}
              className="bg-slate-900 border border-slate-700 rounded-lg px-2 py-1 text-white text-xs focus:outline-none focus:border-emerald-500"
            >
              <option value="ALL">All severities</option>
              <option value="HIGH">High only</option>
              <option value="MEDIUM">Medium only</option>
              <option value="LOW">Low only</option>
            </select>
            <span className="text-slate-500 text-xs">
              ({filteredFindings.length} shown)
            </span>
          </div>

          <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
            {filteredFindings.map((f) => (
              <div
                key={f.id}
                className="bg-slate-900 rounded-lg p-3 border border-slate-700"
              >
                <div className="flex items-center justify-between mb-1">
                  <span
                    className={`text-xs px-2 py-0.5 rounded border ${severityColor(
                      f.severity
                    )}`}
                  >
                    {f.severity}
                  </span>
                  <span className="text-xs text-slate-500">
                    {f.tool} · {f.fileName}:{f.lineNumber}
                  </span>
                </div>
                <p className="text-white text-sm font-medium">{f.issue}</p>
                {f.explanation && (
                  <p className="text-slate-400 text-xs mt-1">{f.explanation}</p>
                )}
                {f.suggestion && (
                  <p className="text-emerald-400 text-xs mt-1">
                    Fix: {f.suggestion}
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* COMPLEXITY RESULTS */}
      {!loading && complexity && (
        <div className="grid grid-cols-2 gap-3">
          {[
            ["Classes", complexity.numberOfClasses],
            ["Methods", complexity.numberOfMethods],
            ["Lines of Code", complexity.linesOfCode],
            ["Cyclomatic Complexity", complexity.cyclomaticComplexity],
            ["Avg Method Length", complexity.averageMethodLength],
            ["Maintainability Index", complexity.maintainabilityIndex],
          ].map(([label, val]) => (
            <div
              key={label}
              className="bg-slate-900 rounded-lg p-4 border border-slate-700"
            >
              <p className="text-2xl font-bold text-emerald-400">{val}</p>
              <p className="text-slate-400 text-xs mt-1">{label}</p>
            </div>
          ))}
        </div>
      )}

      {/* DOCS RESULTS */}
      {!loading && docs && (
        <div>
          <div className="flex justify-end mb-2">
            <button
              onClick={() => {
                const win = window.open("", "_blank");
                win.document.write(`
                  <html>
                    <head>
                      <title>${project.projectName} - Documentation</title>
                      <style>
                        body { font-family: -apple-system, Segoe UI, Roboto, sans-serif;
                               max-width: 800px; margin: 40px auto; padding: 0 20px;
                               line-height: 1.6; color: #1e293b; }
                        h1 { color: #059669; border-bottom: 2px solid #059669; padding-bottom: 8px; }
                        h2 { color: #0f766e; margin-top: 28px; }
                        h3 { color: #334155; }
                        code { background: #f1f5f9; padding: 2px 6px; border-radius: 4px;
                               font-family: monospace; font-size: 90%; }
                        pre { background: #f8fafc; padding: 16px; border-radius: 8px;
                              border: 1px solid #e2e8f0; overflow-x: auto; }
                        ul { padding-left: 22px; }
                        .header { text-align: center; color: #64748b; font-size: 13px; margin-bottom: 30px; }
                      </style>
                    </head>
                    <body>
                      <div class="header">AI Code Review Assistant — Generated Documentation</div>
                      <pre style="white-space: pre-wrap; background: white; border: none; padding: 0;">${docs
                        .replace(/&/g, "&amp;")
                        .replace(/</g, "&lt;")
                        .replace(/>/g, "&gt;")}</pre>
                    </body>
                  </html>
                `);
                win.document.close();
              }}
              className="bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-medium rounded-lg px-3 py-1.5 transition"
            >
              Open / Print as PDF
            </button>
          </div>
          <pre className="text-slate-300 text-xs bg-slate-900 rounded-lg p-4 max-h-96 overflow-y-auto whitespace-pre-wrap">
            {docs}
          </pre>
        </div>
      )}

      {/* REVIEW HISTORY */}
      {history.length > 0 && (
        <div className="mt-6 pt-5 border-t border-slate-700">
          <h3 className="text-sm font-semibold text-white mb-3">
            Review History
          </h3>
          <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
            {history.map((r) => (
              <div
                key={r.id}
                className="flex items-center justify-between bg-slate-900 rounded-lg px-3 py-2 border border-slate-700"
              >
                <div>
                  <span
                    className={`text-xs px-2 py-0.5 rounded ${
                      r.reviewType === "AI"
                        ? "bg-purple-500/20 text-purple-300"
                        : "bg-emerald-500/20 text-emerald-300"
                    }`}
                  >
                    {r.reviewType}
                  </span>
                  <span className="text-slate-400 text-xs ml-2">
                    Score {r.reviewScore}/100 · {r.totalIssues} issues
                  </span>
                  <p className="text-slate-500 text-xs mt-1">
                    {new Date(r.createdAt).toLocaleString()}
                  </p>
                </div>
                <button
                  onClick={async () => {
                    try {
                      await api.delete(`/reviews/${r.id}`);
                      loadHistory();
                    } catch {
                      // ignore
                    }
                  }}
                  className="text-red-400 hover:text-red-300 text-xs"
                >
                  Delete
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default AnalysisPanel;