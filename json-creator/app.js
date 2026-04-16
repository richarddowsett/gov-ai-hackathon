const pageTypes = [
  "contentPage",
  "string",
  "datePage",
  "boolean",
  "radioButton",
  "checkbox",
  "multipleQuestionsPage",
];

const templateByType = (type) => {
  const shared = {
    type,
    title: "",
    index: 0,
    options: [""],
    optionTargets: [0],
    boolTrue: 0,
    boolFalse: 0,
    questions: ["", ""],
    validations: ["", ""],
    stringValidation: "",
  };

  switch (type) {
    case "contentPage":
      return { ...shared, title: "Information page", index: 1 };
    case "string":
      return { ...shared, title: "Enter text", index: 1, stringValidation: "^[A-Za-z\\s]{1,100}$" };
    case "datePage":
      return { ...shared, title: "Enter date", index: 1 };
    case "boolean":
      return { ...shared, title: "Yes or no question", boolTrue: 1, boolFalse: 2 };
    case "radioButton":
      return {
        ...shared,
        title: "Choose one option",
        options: ["Option A", "Option B"],
        optionTargets: [1, 2],
      };
    case "checkbox":
      return { ...shared, title: "Select all that apply", index: 1, options: ["Option 1", "Option 2"] };
    case "multipleQuestionsPage":
      return {
        ...shared,
        title: "Rate your experience",
        index: 1,
        questions: ["Question 1", "Question 2"],
        validations: ["^[1-5]$", "^[1-5]$"],
      };
    default:
      return shared;
  }
};

const blankState = () => ({ pages: [templateByType("contentPage")] });
let state = blankState();

let schemaValidator = null;
let schemaReady = false;

const addTypeEl = document.getElementById("add-type");
const pagesEl = document.getElementById("pages");
const outputEl = document.getElementById("json-output");
const messagesEl = document.getElementById("messages");
const validationSummaryEl = document.getElementById("validation-summary");
const graphEl = document.getElementById("graph");
const storageListEl = document.getElementById("storage-list");
const apiBaseEl = document.getElementById("api-base");
const serviceNameEl = document.getElementById("service-name");

for (const t of pageTypes) {
  const opt = document.createElement("option");
  opt.value = t;
  opt.textContent = `${t} template`;
  addTypeEl.appendChild(opt);
}
addTypeEl.value = "contentPage";

document.getElementById("add-page").addEventListener("click", () => {
  state.pages.push(templateByType(addTypeEl.value));
  render();
});

document.getElementById("preset-linear").addEventListener("click", () => {
  state.pages = [
    templateByType("contentPage"),
    templateByType("string"),
    templateByType("datePage"),
    templateByType("contentPage"),
  ];
  state.pages[0].title = "Welcome";
  state.pages[0].index = 1;
  state.pages[1].title = "What is your name?";
  state.pages[1].index = 2;
  state.pages[2].title = "What is your date of birth?";
  state.pages[2].index = 3;
  state.pages[3].title = "Done";
  state.pages[3].index = 99;
  render();
  showMessage("Loaded preset: Simple Linear", "info");
});

document.getElementById("preset-boolean").addEventListener("click", () => {
  state.pages = [templateByType("contentPage"), templateByType("boolean"), templateByType("contentPage"), templateByType("contentPage")];
  state.pages[0].title = "Start";
  state.pages[0].index = 1;
  state.pages[1].title = "Do you have a licence?";
  state.pages[1].boolTrue = 2;
  state.pages[1].boolFalse = 3;
  state.pages[2].title = "Licence path";
  state.pages[2].index = 99;
  state.pages[3].title = "No licence path";
  state.pages[3].index = 99;
  render();
  showMessage("Loaded preset: Boolean Branch", "info");
});

document.getElementById("preset-complex").addEventListener("click", () => {
  state.pages = [
    templateByType("contentPage"),
    templateByType("string"),
    templateByType("datePage"),
    templateByType("boolean"),
    templateByType("radioButton"),
    templateByType("checkbox"),
    templateByType("multipleQuestionsPage"),
    templateByType("contentPage"),
  ];

  state.pages[0].title = "Welcome to survey";
  state.pages[0].index = 1;
  state.pages[1].title = "What is your name?";
  state.pages[1].index = 2;
  state.pages[2].title = "What is your date of birth?";
  state.pages[2].index = 3;
  state.pages[3].title = "Do you drive?";
  state.pages[3].boolTrue = 4;
  state.pages[3].boolFalse = 6;
  state.pages[4].title = "Vehicle type";
  state.pages[4].options = ["Car", "Motorcycle", "Other"];
  state.pages[4].optionTargets = [5, 5, 6];
  state.pages[5].title = "Vehicle features";
  state.pages[5].options = ["GPS", "Cruise Control"];
  state.pages[5].index = 6;
  state.pages[6].title = "Rate experience";
  state.pages[6].questions = ["How satisfied?", "Recommend us?"];
  state.pages[6].validations = ["^[1-5]$", "^[1-5]$"];
  state.pages[6].index = 7;
  state.pages[7].title = "Thank you";
  state.pages[7].index = 99;

  render();
  showMessage("Loaded preset: Complex Survey", "info");
});

document.getElementById("preset-reset").addEventListener("click", () => {
  state = blankState();
  render();
  showMessage("Builder reset", "info");
});

document.getElementById("export-json").addEventListener("click", () => {
  const result = validateAndBuild();
  if (result.errors.length) {
    showMessage(`Cannot export: ${result.errors[0]}`, "error");
    return;
  }

  const payload = JSON.stringify(result.json, null, 2);
  outputEl.value = payload;

  const blob = new Blob([payload], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "journey.json";
  a.click();
  URL.revokeObjectURL(url);
  showMessage("JSON exported as journey.json", "success");
});

document.getElementById("copy-json").addEventListener("click", async () => {
  const result = validateAndBuild();
  if (result.errors.length) {
    showMessage(`Cannot copy: ${result.errors[0]}`, "error");
    return;
  }

  const payload = JSON.stringify(result.json, null, 2);
  outputEl.value = payload;
  await navigator.clipboard.writeText(payload);
  showMessage("JSON copied", "success");
});

document.getElementById("import-json").addEventListener("click", () => {
  const raw = outputEl.value.trim();
  if (!raw) {
    showMessage("Paste JSON into JSON Output, then click Import JSON.", "error");
    return;
  }

  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch (err) {
    const hint = parseErrorHint(raw, err);
    showMessage(`Invalid JSON. ${hint}`, "error");
    return;
  }

  if (!parsed || !Array.isArray(parsed.pages)) {
    showMessage("Imported JSON must contain a pages array.", "error");
    return;
  }

  importJourneyObject(parsed, "JSON imported and normalized.");
});

document.getElementById("save-api").addEventListener("click", async () => {
  const serviceName = getServiceName();
  if (!serviceName) {
    showMessage("Enter a service name before saving.", "error");
    return;
  }

  const result = validateAndBuild();
  if (result.errors.length) {
    showMessage(`Cannot save: ${result.errors[0]}`, "error");
    return;
  }

  const payload = {
    serviceName,
    json: JSON.stringify(result.json),
  };

  const encoded = encodeURIComponent(serviceName);
  const updateRes = await apiRequest(`/journeys/${encoded}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });

  if (updateRes.ok) {
    showMessage(`Saved '${serviceName}' (updated).`, "success");
    return;
  }

  if (updateRes.status !== 404) {
    showMessage(`Save failed (${updateRes.status}): ${updateRes.error || "unknown error"}`, "error");
    return;
  }

  const createRes = await apiRequest("/journeys", {
    method: "POST",
    body: JSON.stringify(payload),
  });

  if (createRes.ok) {
    showMessage(`Saved '${serviceName}' (created).`, "success");
    return;
  }

  showMessage(`Save failed (${createRes.status}): ${createRes.error || "unknown error"}`, "error");
});

document.getElementById("load-api").addEventListener("click", async () => {
  const serviceName = getServiceName();
  if (!serviceName) {
    showMessage("Enter a service name before loading.", "error");
    return;
  }

  const encoded = encodeURIComponent(serviceName);
  const res = await apiRequest(`/journeys/${encoded}`, { method: "GET" });
  if (!res.ok) {
    showMessage(`Load failed (${res.status}): ${res.error || "unknown error"}`, "error");
    return;
  }

  const raw = res.data?.json;
  if (typeof raw !== "string") {
    showMessage("Load failed: API response missing JSON payload string.", "error");
    return;
  }

  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch (err) {
    showMessage(`Stored JSON is invalid. ${parseErrorHint(raw, err)}`, "error");
    return;
  }

  importJourneyObject(parsed, `Loaded '${serviceName}' from API.`);
});

document.getElementById("list-api").addEventListener("click", async () => {
  const res = await apiRequest("/journeys", { method: "GET" });
  if (!res.ok) {
    showMessage(`List failed (${res.status}): ${res.error || "unknown error"}`, "error");
    return;
  }

  const items = Array.isArray(res.data) ? res.data : [];
  if (!items.length) {
    storageListEl.innerHTML = "<p class=\"govuk-body-s\">No saved journeys.</p>";
    showMessage("No saved journeys found.", "info");
    return;
  }

  storageListEl.innerHTML = items
    .map((j) => {
      const rawName = String(j.serviceName || "");
      const name = escapeHtml(rawName);
      const encoded = encodeURIComponent(rawName);
      return `
        <div class="storage-item">
          <span>${name}</span>
          <button type="button" data-load-name="${encoded}">Load</button>
        </div>
      `;
    })
    .join("");

  storageListEl.querySelectorAll("[data-load-name]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const encodedName = btn.getAttribute("data-load-name") || "";
      serviceNameEl.value = decodeURIComponent(encodedName);
      document.getElementById("load-api").click();
    });
  });

  showMessage(`Found ${items.length} saved journey(s).`, "success");
});

async function initSchemaValidation() {
  try {
    const res = await fetch("./journey.schema.json");
    const schema = await res.json();
    const AjvCtor = window.ajv7 || window.Ajv;
    const ajv = new AjvCtor({ allErrors: true, strict: false });
    schemaValidator = ajv.compile(schema);
    schemaReady = true;
  } catch {
    schemaReady = false;
    showMessage("Schema validator not loaded; running structural checks only.", "info");
  }
  render();
}

function fromExternalPage(page) {
  if (!page || typeof page !== "object") {
    throw new Error("Page must be an object.");
  }

  const type = pageTypes.includes(page.type) ? page.type : "contentPage";
  const out = templateByType(type);
  out.title = typeof page.title === "string" ? page.title : "";

  if (typeof page.index === "number") out.index = toInt(page.index);

  if (type === "boolean") {
    if (!page.index || typeof page.index !== "object") throw new Error("boolean page requires object index with true/false");
    out.boolTrue = toInt(page.index.true ?? 0);
    out.boolFalse = toInt(page.index.false ?? 0);
  }

  if (type === "radioButton") {
    const options = Array.isArray(page.options) ? page.options : [];
    if (!options.length) throw new Error("radioButton requires options array");
    out.options = options.map((x) => String(x));
    out.optionTargets = out.options.map((opt) => toInt(page.index?.[opt] ?? 0));
  }

  if (type === "checkbox") {
    const options = Array.isArray(page.options) ? page.options : [];
    if (!options.length) throw new Error("checkbox requires options array");
    out.options = options.map((x) => String(x));
  }

  if (type === "multipleQuestionsPage") {
    const questions = Array.isArray(page.questions) ? page.questions : [];
    out.questions = [questions[0]?.questionTitle || "", questions[1]?.questionTitle || ""];
    const validation = Array.isArray(page.validation) ? page.validation : ["", ""];
    out.validations = [String(validation[0] || ""), String(validation[1] || "")];
  }

  if (type === "string") {
    out.stringValidation = typeof page.validation === "string" ? page.validation : "";
  }

  return out;
}

function toInt(v) {
  const n = Number(v);
  return Number.isFinite(n) ? Math.max(0, Math.floor(n)) : 0;
}

function normalizeText(v) {
  return String(v || "").trim().toLowerCase();
}

function findDuplicateLabels(values) {
  const seen = new Set();
  const dupes = new Set();
  values.forEach((v) => {
    const key = normalizeText(v);
    if (!key) return;
    if (seen.has(key)) dupes.add(key);
    seen.add(key);
  });
  return [...dupes];
}

function regexSafetyIssue(rx) {
  const source = String(rx || "");
  if (!source) return null;
  if (source.length > 120) return "Regex is very long; simplify to reduce risk.";
  if (/\\\d/.test(source)) return "Backreferences are not allowed in validation regex.";
  if (/\((?:[^()]*[+*][^()]*)\)[+*{]/.test(source)) {
    return "Nested quantifiers can cause slow regex performance.";
  }
  if (/\((?:\.\*|\.\+)[^()]*\)[+*{]/.test(source)) {
    return "Greedy wildcard with quantifier can cause catastrophic backtracking.";
  }
  return null;
}

function importJourneyObject(parsed, successMessage) {
  if (!parsed.pages.length) {
    showMessage("Imported JSON pages array cannot be empty.", "error");
    return;
  }

  const converted = [];
  for (let i = 0; i < parsed.pages.length; i += 1) {
    const p = parsed.pages[i];
    try {
      converted.push(fromExternalPage(p));
    } catch (e) {
      showMessage(`Import issue at pages[${i}]: ${String(e)}`, "error");
      return;
    }
  }

  state.pages = converted;
  render();
  showMessage(successMessage, "success");
}

function getApiBase() {
  return (apiBaseEl.value || "").trim().replace(/\/+$/, "");
}

function getServiceName() {
  return (serviceNameEl.value || "").trim();
}

async function apiRequest(path, opts) {
  const base = getApiBase();
  if (!base) {
    return { ok: false, status: 0, error: "API Base URL is required." };
  }

  const url = `${base}${path}`;
  try {
    const res = await fetch(url, {
      ...opts,
      headers: {
        ...(opts?.headers || {}),
        ...(opts?.body ? { "Content-Type": "application/json" } : {}),
      },
    });

    const text = await res.text();
    let data = null;
    try {
      data = text ? JSON.parse(text) : null;
    } catch {
      data = null;
    }

    const error = data?.error || (text && !res.ok ? text : null);
    return { ok: res.ok, status: res.status, data, error };
  } catch (err) {
    return { ok: false, status: 0, error: String(err) };
  }
}

function parseErrorHint(raw, err) {
  const trimmed = raw.trimStart().slice(0, 120).toLowerCase();
  if (trimmed.startsWith("<!doctype") || trimmed.startsWith("<html")) {
    return "Input appears to be HTML, not JSON (for example an error page response).";
  }
  const m = String(err?.message || err).match(/position\s+(\d+)/i);
  if (!m) return String(err);
  const pos = Number(m[1]);
  const before = raw.slice(0, pos);
  const line = before.split("\n").length;
  const col = pos - before.lastIndexOf("\n");
  return `Error near line ${line}, column ${col}.`;
}

function validateAndBuild() {
  const errors = [];
  const warnings = [];
  const perPage = state.pages.map(() => ({ errors: [], warnings: [] }));
  const pages = [];

  if (!state.pages.length) {
    errors.push("Add at least one page.");
  }

  state.pages.forEach((p, i) => {
    const item = { type: p.type, title: (p.title || "").trim() };
    const pageTag = `Page ${i + 1}`;

    if (!item.title) {
      perPage[i].errors.push("Title is required.");
    }
    if (item.title.length > 100) {
      perPage[i].errors.push("Title exceeds 100 characters.");
    }

    const checkRegex = (rx, label) => {
      if (!rx) return;
      try {
        // eslint-disable-next-line no-new
        new RegExp(rx);
      } catch {
        perPage[i].errors.push(`${label} is not a valid regex.`);
        return;
      }

      const safety = regexSafetyIssue(rx);
      if (safety) {
        perPage[i].errors.push(`${label} may be unsafe: ${safety}`);
      }
    };

    if (p.type === "boolean") {
      item.index = { true: toInt(p.boolTrue), false: toInt(p.boolFalse) };
      if (item.index.true === item.index.false) {
        perPage[i].warnings.push("true/false both go to the same page.");
      }
    } else if (p.type === "radioButton") {
      const rawOptions = Array.isArray(p.options) ? p.options : [];
      const rawTargets = Array.isArray(p.optionTargets) ? p.optionTargets : [];
      const optionPairs = rawOptions
        .map((label, idx) => ({ label: (label || "").trim(), target: toInt(rawTargets[idx] ?? 0) }))
        .filter((x) => x.label);
      const options = optionPairs.map((x) => x.label);

      if (rawTargets.length !== rawOptions.length) {
        perPage[i].errors.push("Option target count must match options count.");
      }
      if (!options.length) perPage[i].errors.push("radioButton requires at least one option.");
      if (options.some((x) => x.length > 50)) perPage[i].errors.push("Option labels must be 50 chars or less.");
      const duplicates = findDuplicateLabels(options);
      if (duplicates.length) perPage[i].errors.push("Option labels must be unique.");
      item.options = options;
      item.index = {};
      optionPairs.forEach((pair) => {
        item.index[pair.label] = pair.target;
      });
    } else {
      item.index = toInt(p.index);
    }

    if (p.type === "checkbox") {
      const options = (p.options || []).map((x) => (x || "").trim()).filter(Boolean);
      if (!options.length) perPage[i].errors.push("checkbox requires at least one option.");
      if (options.some((x) => x.length > 50)) perPage[i].errors.push("Option labels must be 50 chars or less.");
      const duplicates = findDuplicateLabels(options);
      if (duplicates.length) perPage[i].errors.push("Option labels must be unique.");
      item.options = options;
    }

    if (p.type === "multipleQuestionsPage") {
      if ((p.questions || []).length !== 2) perPage[i].errors.push("Exactly two question titles are required.");
      if ((p.validations || []).length !== 2) perPage[i].errors.push("Exactly two validation regex values are required.");
      const q1 = (p.questions?.[0] || "").trim();
      const q2 = (p.questions?.[1] || "").trim();
      if (!q1 || !q2) perPage[i].errors.push("Two question titles are required.");
      if (q1.length > 100 || q2.length > 100) perPage[i].errors.push("Question titles must be 100 chars or less.");
      item.questions = [{ questionTitle: q1 }, { questionTitle: q2 }];

      const v1 = (p.validations?.[0] || "").trim();
      const v2 = (p.validations?.[1] || "").trim();
      if (v1 || v2) item.validation = [v1, v2];
      checkRegex(v1, "Validation regex 1");
      checkRegex(v2, "Validation regex 2");
    }

    if (p.type === "string") {
      const rx = (p.stringValidation || "").trim();
      if (!rx) perPage[i].errors.push("string page requires a validation regex.");
      if (rx) item.validation = rx;
      checkRegex(rx, "Validation regex");
    }

    pages.push(item);

    if (perPage[i].errors.length) {
      errors.push(`${pageTag}: ${perPage[i].errors[0]}`);
    }
  });

  const json = { pages };

  if (schemaReady && schemaValidator) {
    const ok = schemaValidator(json);
    if (!ok) {
      for (const err of schemaValidator.errors || []) {
        const path = err.instancePath || "/";
        errors.push(`Schema: ${path} ${err.message}`.trim());
      }
    }
  }

  const graph = analyzeGraph(json.pages);
  graph.errors.forEach((e) => errors.push(e));
  graph.warnings.forEach((w) => warnings.push(w));

  for (const [idx, ws] of graph.perPageWarnings.entries()) {
    ws.forEach((w) => perPage[idx].warnings.push(w));
  }

  return { json, errors, warnings, perPage, graph };
}

function analyzeGraph(pages) {
  const n = pages.length;
  const edges = pages.map((page) => {
    if (typeof page.index === "number") return [page.index];
    return Object.values(page.index || {}).map((x) => Number(x));
  });

  const errors = [];
  const warnings = [];
  const perPageWarnings = new Map();
  const inRange = (x) => Number.isFinite(x) && x >= 0 && x < n;

  const reachable = new Set([0]);
  const q = [0];
  while (q.length) {
    const cur = q.shift();
    for (const t of edges[cur] || []) {
      if (inRange(t) && !reachable.has(t)) {
        reachable.add(t);
        q.push(t);
      }
    }
  }

  for (let i = 0; i < n; i += 1) {
    if (!reachable.has(i)) {
      warnings.push(`Page ${i + 1} is unreachable from Page 1.`);
      if (!perPageWarnings.has(i)) perPageWarnings.set(i, []);
      perPageWarnings.get(i).push("Unreachable page");
    }

    const targets = edges[i] || [];
    if (targets.length && targets.every((t) => !inRange(t))) {
      if (i !== n - 1) {
        warnings.push(`Page ${i + 1} always exits to END/out-of-range.`);
        if (!perPageWarnings.has(i)) perPageWarnings.set(i, []);
        perPageWarnings.get(i).push("Dead-end to END");
      }
    }

    if (targets.some((t) => t > n + 5)) {
      warnings.push(`Page ${i + 1} has unusually large index target.`);
      if (!perPageWarnings.has(i)) perPageWarnings.set(i, []);
      perPageWarnings.get(i).push("Large target index");
    }

    if (targets.some((t) => t === i)) {
      warnings.push(`Page ${i + 1} links to itself.`);
      if (!perPageWarnings.has(i)) perPageWarnings.set(i, []);
      perPageWarnings.get(i).push("Self-loop");
    }
  }

  const visiting = new Set();
  const visited = new Set();
  let hasCycle = false;

  function dfs(node) {
    if (visiting.has(node)) {
      hasCycle = true;
      return;
    }
    if (visited.has(node)) return;
    visiting.add(node);
    for (const t of edges[node] || []) if (inRange(t)) dfs(t);
    visiting.delete(node);
    visited.add(node);
  }

  for (let i = 0; i < n; i += 1) dfs(i);
  if (hasCycle) warnings.push("Journey contains a loop/cycle.");

  const endReachMemo = new Map();
  const canReachEnd = (node, stack) => {
    if (endReachMemo.has(node)) return endReachMemo.get(node);
    if (stack.has(node)) return false;

    const targets = edges[node] || [];
    if (targets.some((t) => !inRange(t))) {
      endReachMemo.set(node, true);
      return true;
    }

    stack.add(node);
    for (const t of targets) {
      if (inRange(t) && canReachEnd(t, stack)) {
        stack.delete(node);
        endReachMemo.set(node, true);
        return true;
      }
    }
    stack.delete(node);
    endReachMemo.set(node, false);
    return false;
  };

  if (n > 0 && !canReachEnd(0, new Set())) {
    errors.push("No terminal path from Page 1 (all routes loop without reaching END).");
  }

  for (let i = 0; i < n; i += 1) {
    if (reachable.has(i) && !canReachEnd(i, new Set())) {
      if (!perPageWarnings.has(i)) perPageWarnings.set(i, []);
      perPageWarnings.get(i).push("Cannot reach END from this page");
    }
  }

  return { edges, errors, warnings, perPageWarnings, reachable };
}

function showMessage(message, type) {
  messagesEl.innerHTML = `<div class="msg ${type}">${escapeHtml(message)}</div>`;
}

function escapeHtml(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
}

function renderValidationSummary(result) {
  const errCount = result.errors.length;
  const warnCount = result.warnings.length;
  const schemaState = schemaReady ? "Schema checks enabled" : "Schema checks unavailable";

  const firstErrors = result.errors.slice(0, 5).map((e) => `<li>${escapeHtml(e)}</li>`).join("");
  const firstWarnings = result.warnings.slice(0, 5).map((w) => `<li>${escapeHtml(w)}</li>`).join("");

  validationSummaryEl.innerHTML = `
    <p class="govuk-body-s">${escapeHtml(schemaState)}</p>
    <p class="govuk-body-s ${errCount ? "summary-bad" : "summary-good"}">Errors: ${errCount}</p>
    <p class="govuk-body-s ${warnCount ? "summary-bad" : "summary-good"}">Warnings: ${warnCount}</p>
    ${firstErrors ? `<ul class="summary-list">${firstErrors}</ul>` : ""}
    ${firstWarnings ? `<ul class="summary-list">${firstWarnings}</ul>` : ""}
  `;
}

function renderGraph(result) {
  const pages = result.json.pages;
  if (!pages.length) {
    graphEl.innerHTML = "<p class=\"govuk-body-s\">No pages yet.</p>";
    return;
  }

  const w = 600;
  const yStep = 72;
  const h = Math.max(180, pages.length * yStep + 40);
  const xNode = 130;
  const xEnd = 520;

  let edgesSvg = "";
  let nodesSvg = "";

  const nodeY = (i) => 40 + i * yStep;

  pages.forEach((p, i) => {
    const y = nodeY(i);
    const status = result.perPage[i];
    const fill = status.errors.length ? "#f6d7d4" : status.warnings.length ? "#fef3d9" : "#d9f2e0";

    nodesSvg += `<rect x="20" y="${y - 16}" width="220" height="32" rx="6" fill="${fill}" stroke="#505a5f" />`;
    nodesSvg += `<text x="30" y="${y + 5}" font-size="12" fill="#0b0c0c">${escapeHtml(`P${i + 1}: ${p.title}`)}</text>`;

    const targets = typeof p.index === "number" ? ["_default", p.index] : Object.entries(p.index || {});
    targets.forEach((entry, tIdx) => {
      const key = Array.isArray(entry) ? entry[0] : "_default";
      const target = Array.isArray(entry) ? entry[1] : entry;
      const yOffset = y + (tIdx - ((targets.length - 1) / 2)) * 8;
      if (target >= 0 && target < pages.length) {
        const ty = nodeY(target);
        edgesSvg += `<line x1="240" y1="${yOffset}" x2="20" y2="${ty}" stroke="#1d70b8" stroke-width="1.6" marker-end="url(#arr)" />`;
        edgesSvg += `<text x="250" y="${yOffset - 2}" font-size="10" fill="#1d70b8">${escapeHtml(String(key))}</text>`;
      } else {
        edgesSvg += `<line x1="240" y1="${yOffset}" x2="${xEnd}" y2="${h / 2}" stroke="#505a5f" stroke-dasharray="4 3" stroke-width="1.3" marker-end="url(#arr)" />`;
      }
    });
  });

  nodesSvg += `<rect x="${xEnd}" y="${h / 2 - 20}" width="60" height="40" rx="6" fill="#f3f2f1" stroke="#505a5f" />`;
  nodesSvg += `<text x="${xEnd + 15}" y="${h / 2 + 4}" font-size="12">END</text>`;

  graphEl.innerHTML = `
    <svg viewBox="0 0 ${w} ${h}" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Journey graph preview">
      <defs>
        <marker id="arr" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto" markerUnits="strokeWidth">
          <path d="M0,0 L8,4 L0,8 Z" fill="#1d70b8" />
        </marker>
      </defs>
      ${edgesSvg}
      ${nodesSvg}
    </svg>
  `;
}

function updateOutputPreview(result) {
  outputEl.value = JSON.stringify(result.json, null, 2);
}

function statusForPage(diag) {
  if (diag.errors.length) return "invalid";
  if (diag.warnings.length) return "warning";
  return "valid";
}

function indexOptionsHtml(current, pages) {
  const count = pages.length;
  const max = Math.max(count + 5, 12, current);
  const options = [];
  for (let i = 0; i <= max; i += 1) {
    const title = i < count ? (pages[i].title || `Page ${i + 1}`).trim() : "";
    const label = i < count ? `${i} (${title || `Page ${i + 1}`})` : `${i} (END)`;
    options.push(`<option value="${i}" ${i === current ? "selected" : ""}>${label}</option>`);
  }
  return options.join("");
}

function jumpHref(target, pageCount) {
  return target >= 0 && target < pageCount ? `#page-card-${target + 1}` : "";
}

function render() {
  const result = validateAndBuild();

  pagesEl.innerHTML = "";
  state.pages.forEach((page, idx) => {
    const diag = result.perPage[idx] || { errors: [], warnings: [] };
    const status = statusForPage(diag);
    const card = document.createElement("div");
    card.className = `page-card ${status === "invalid" ? "page-invalid" : status === "warning" ? "page-warning" : ""}`;
    card.id = `page-card-${idx + 1}`;

    const statusLabel = status === "invalid" ? "invalid" : status === "warning" ? "warning" : "valid";

    card.innerHTML = `
      <h3 class="govuk-heading-s">Page ${idx + 1}<span class="page-status ${status !== "valid" ? status : ""}">${statusLabel}</span></h3>
      <div class="page-grid">
        <div class="govuk-form-group">
          <label class="govuk-label" for="type-${idx}">Type</label>
          <select id="type-${idx}" class="govuk-select"></select>
        </div>
        <div class="govuk-form-group">
          <label class="govuk-label" for="title-${idx}">Title</label>
          <input id="title-${idx}" class="govuk-input" maxlength="100" value="${escapeHtml(page.title)}" />
        </div>
      </div>
      <div class="inline-actions">
        <button type="button" class="govuk-button govuk-button--warning" data-remove="${idx}">Remove</button>
      </div>
      <div id="details-${idx}" style="margin-top: 12px;"></div>
      ${diag.errors.length ? `<ul class="govuk-list govuk-error-message">${diag.errors.map((e) => `<li>${escapeHtml(e)}</li>`).join("")}</ul>` : ""}
      ${diag.warnings.length ? `<ul class="govuk-list govuk-hint">${diag.warnings.map((w) => `<li>${escapeHtml(w)}</li>`).join("")}</ul>` : ""}
    `;

    const typeSelect = card.querySelector(`#type-${idx}`);
    pageTypes.forEach((t) => {
      const opt = document.createElement("option");
      opt.value = t;
      opt.textContent = t;
      if (t === page.type) opt.selected = true;
      typeSelect.appendChild(opt);
    });

    typeSelect.addEventListener("change", (e) => {
      state.pages[idx] = { ...templateByType(e.target.value), title: state.pages[idx].title };
      render();
    });

    card.querySelector(`#title-${idx}`).addEventListener("input", (e) => {
      state.pages[idx].title = e.target.value;
      render();
    });

    card.querySelector(`[data-remove="${idx}"]`).addEventListener("click", () => {
      state.pages.splice(idx, 1);
      if (!state.pages.length) state.pages.push(templateByType("contentPage"));
      render();
    });

    renderTypeDetails(card.querySelector(`#details-${idx}`), page, idx, state.pages.length);
    pagesEl.appendChild(card);
  });

  renderValidationSummary(result);
  renderGraph(result);
  updateOutputPreview(result);
}

function makeSelectIndexField(label, value, pages, onChange) {
  const pageCount = pages.length;
  const wrap = document.createElement("div");
  wrap.className = "govuk-form-group";

  const href = jumpHref(value, pageCount);
  wrap.innerHTML = `
    <label class="govuk-label">${label}</label>
    <div class="index-line">
      <select class="govuk-select compact-input">${indexOptionsHtml(value, pages)}</select>
      ${href ? `<a class="govuk-link jump-link" href="${href}">Jump</a>` : ""}
    </div>
  `;

  wrap.querySelector("select").addEventListener("change", (e) => {
    onChange(toInt(e.target.value));
    render();
  });

  return wrap;
}

function renderTypeDetails(container, page, idx, pageCount) {
  container.innerHTML = "";

  if (page.type === "boolean") {
    container.appendChild(makeSelectIndexField("Index when true", page.boolTrue, state.pages, (v) => (state.pages[idx].boolTrue = v)));
    container.appendChild(makeSelectIndexField("Index when false", page.boolFalse, state.pages, (v) => (state.pages[idx].boolFalse = v)));
    return;
  }

  if (page.type === "radioButton") {
    container.appendChild(optionsBlock(page, idx, true, pageCount));
    return;
  }

  if (page.type === "checkbox") {
    container.appendChild(makeSelectIndexField("Next index", page.index, state.pages, (v) => (state.pages[idx].index = v)));
    container.appendChild(optionsBlock(page, idx, false, pageCount));
    return;
  }

  if (page.type === "multipleQuestionsPage") {
    container.appendChild(makeSelectIndexField("Next index", page.index, state.pages, (v) => (state.pages[idx].index = v)));

    [0, 1].forEach((n) => {
      const qWrap = document.createElement("div");
      qWrap.className = "govuk-form-group";
      qWrap.innerHTML = `
        <label class="govuk-label">Question ${n + 1} title</label>
        <input class="govuk-input" maxlength="100" value="${escapeHtml(page.questions[n] || "")}" />
      `;
      qWrap.querySelector("input").addEventListener("input", (e) => {
        state.pages[idx].questions[n] = e.target.value;
        render();
      });
      container.appendChild(qWrap);

      const vWrap = document.createElement("div");
      vWrap.className = "govuk-form-group";
      vWrap.innerHTML = `
        <label class="govuk-label">Validation regex ${n + 1} (optional)</label>
        <input class="govuk-input" maxlength="100" value="${escapeHtml(page.validations[n] || "")}" />
      `;
      vWrap.querySelector("input").addEventListener("input", (e) => {
        state.pages[idx].validations[n] = e.target.value;
        render();
      });
      container.appendChild(vWrap);
    });

    return;
  }

  if (page.type === "string") {
    container.appendChild(makeSelectIndexField("Next index", page.index, state.pages, (v) => (state.pages[idx].index = v)));
    const vWrap = document.createElement("div");
    vWrap.className = "govuk-form-group";
    vWrap.innerHTML = `
      <label class="govuk-label">Validation regex (optional)</label>
      <input class="govuk-input" maxlength="100" value="${escapeHtml(page.stringValidation || "")}" />
    `;
    vWrap.querySelector("input").addEventListener("input", (e) => {
      state.pages[idx].stringValidation = e.target.value;
      render();
    });
    container.appendChild(vWrap);
    return;
  }

  container.appendChild(makeSelectIndexField("Next index", page.index, state.pages, (v) => (state.pages[idx].index = v)));
}

function optionsBlock(page, idx, includeTargets, pageCount) {
  const wrapper = document.createElement("div");
  wrapper.className = "govuk-form-group";

  const list = document.createElement("div");

  const title = document.createElement("label");
  title.className = "govuk-label";
  title.textContent = includeTargets ? "Options and target indexes" : "Options";

  const addBtn = document.createElement("button");
  addBtn.type = "button";
  addBtn.className = "govuk-button govuk-button--secondary";
  addBtn.textContent = "Add Option";
  addBtn.addEventListener("click", () => {
    state.pages[idx].options.push("");
    if (includeTargets) state.pages[idx].optionTargets.push(0);
    render();
  });

  (page.options || []).forEach((opt, optIdx) => {
    const row = document.createElement("div");
    row.className = "option-row";

    const targetControl = includeTargets
      ? `<select class="govuk-select compact-input target">${indexOptionsHtml(toInt((page.optionTargets || [])[optIdx] ?? 0), state.pages)}</select>`
      : "<div></div>";

    row.innerHTML = `
      <input class="govuk-input compact-input option" maxlength="50" value="${escapeHtml(opt || "")}" placeholder="Option label" />
      ${targetControl}
      <button type="button" class="govuk-button govuk-button--warning remove-opt" data-module="govuk-button">Remove</button>
    `;

    row.querySelector(".option").addEventListener("input", (e) => {
      state.pages[idx].options[optIdx] = e.target.value;
      render();
    });

    if (includeTargets) {
      row.querySelector(".target").addEventListener("change", (e) => {
        state.pages[idx].optionTargets[optIdx] = toInt(e.target.value);
        render();
      });
    }

    row.querySelector(".remove-opt").addEventListener("click", () => {
      state.pages[idx].options.splice(optIdx, 1);
      if (includeTargets) state.pages[idx].optionTargets.splice(optIdx, 1);
      if (!state.pages[idx].options.length) {
        state.pages[idx].options.push("");
        if (includeTargets) state.pages[idx].optionTargets.push(0);
      }
      render();
    });

    list.appendChild(row);
  });

  wrapper.appendChild(title);
  wrapper.appendChild(list);
  wrapper.appendChild(addBtn);
  return wrapper;
}

initSchemaValidation();
render();
