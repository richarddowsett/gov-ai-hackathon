import { useState, useCallback, useRef, useEffect } from 'react';
import { useNodesState, useEdgesState, type Node, type Edge } from '@xyflow/react';
import { Sidebar } from './components/Sidebar';
import { Canvas } from './components/Canvas';
import { PropertiesPanel } from './components/PropertiesPanel';
import type { PageNodeData } from './components/nodes/nodeTypes';
import { useJourneyExport, getWarnings } from './hooks/useJourneyExport';
import { useJourneyImport } from './hooks/useJourneyImport';
import { validateJourney } from './utils/schemaValidator';
import { getLayoutedElements } from './utils/autoLayout';
import type { Journey } from './types/journey';

export default function App() {
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { exportJourney, downloadJson } = useJourneyExport();
  const { importJourney } = useJourneyImport();

  useEffect(() => {
    setWarnings(getWarnings(nodes, edges));
  }, [nodes, edges]);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  };

  const handleNodeSelect = useCallback(
    (node: Node | null) => {
      setSelectedNode(node);
    },
    []
  );

  const handleUpdateNode = useCallback(
    (id: string, changes: Partial<PageNodeData>) => {
      setNodes((nds: Node[]) =>
        nds.map((n: Node) => {
          if (n.id !== id) return n;
          const updatedData = { ...n.data, ...changes } as PageNodeData;

          if (changes.options && (n.data as PageNodeData).pageType === 'radioButton') {
            setEdges((eds: Edge[]) =>
              eds.filter(
                (e: Edge) =>
                  e.source !== id ||
                  !e.sourceHandle?.startsWith('branch-') ||
                  changes.options!.includes(e.sourceHandle!.replace('branch-', ''))
              )
            );
          }

          return { ...n, data: updatedData };
        })
      );
      setSelectedNode((prev) =>
        prev?.id === id ? { ...prev, data: { ...prev.data, ...changes } } : prev
      );
    },
    [setNodes, setEdges]
  );

  const handleDeleteNode = useCallback(
    (id: string) => {
      setNodes((nds: Node[]) => nds.filter((n: Node) => n.id !== id));
      setEdges((eds: Edge[]) => eds.filter((e: Edge) => e.source !== id && e.target !== id));
      setSelectedNode(null);
    },
    [setNodes, setEdges]
  );

  const handleExport = useCallback(() => {
    if (nodes.length === 0) {
      showToast('Add at least one page before exporting');
      return;
    }
    const journey = exportJourney(nodes, edges);
    const result = validateJourney(journey);
    if (!result.valid) {
      showToast(`Schema validation failed: ${result.errors[0]}`);
    }
    downloadJson(journey);
    showToast('Journey JSON downloaded');
  }, [nodes, edges, exportJourney, downloadJson]);

  const handleImport = useCallback(() => {
    fileInputRef.current?.click();
  }, []);

  const onFileSelected = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = (ev) => {
        try {
          const journey: Journey = JSON.parse(ev.target?.result as string);
          if (!journey.pages || !Array.isArray(journey.pages)) {
            showToast('Invalid journey JSON: missing pages array');
            return;
          }
          const result = importJourney(journey);
          setNodes(result.nodes);
          setEdges(result.edges);
          setSelectedNode(null);
          showToast(`Imported ${journey.pages.length} pages`);
        } catch {
          showToast('Failed to parse JSON file');
        }
      };
      reader.readAsText(file);
      e.target.value = '';
    },
    [importJourney, setNodes, setEdges]
  );

  const handleAutoLayout = useCallback(() => {
    const { nodes: laid, edges: laidEdges } = getLayoutedElements(nodes, edges);
    setNodes(laid);
    setEdges(laidEdges);
    showToast('Layout applied');
  }, [nodes, edges, setNodes, setEdges]);

  const handleClear = useCallback(() => {
    if (nodes.length > 0 && !window.confirm('Clear all pages? This cannot be undone.')) return;
    setNodes([]);
    setEdges([]);
    setSelectedNode(null);
  }, [nodes, setNodes, setEdges]);

  const validationStatus = nodes.length === 0 ? 'empty' : warnings.length === 0 ? 'valid' : 'invalid';

  return (
    <div className="app-main">
      <div className="app-header">
        <h1>Journey Builder</h1>
        <div className="app-header-actions">
          <button className="gds-btn gds-btn-header" onClick={handleImport}>
            Import JSON
          </button>
          <button className="gds-btn gds-btn-header" onClick={handleExport}>
            Export JSON
          </button>
        </div>
      </div>

      <div className="toolbar">
        <button className="gds-btn gds-btn-secondary gds-btn-small" onClick={handleAutoLayout}>
          Auto Layout
        </button>
        <button className="gds-btn gds-btn-outline gds-btn-small" onClick={handleClear}>
          Clear All
        </button>
        <div className="toolbar-divider" />
        <span style={{ fontSize: 12, color: '#505a5f' }}>
          {nodes.length} page{nodes.length !== 1 ? 's' : ''}
        </span>
        <div className="validation-status">
          <div className={`validation-dot ${validationStatus}`} />
          {validationStatus === 'valid' && 'Valid'}
          {validationStatus === 'invalid' && `${warnings.length} warning${warnings.length > 1 ? 's' : ''}`}
          {validationStatus === 'empty' && 'Empty journey'}
        </div>
      </div>

      <div className="app-canvas-area">
        <Sidebar />
        <Canvas
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          setNodes={setNodes}
          setEdges={setEdges}
          onNodeSelect={handleNodeSelect}
        />
        <PropertiesPanel
          selectedNode={selectedNode}
          onUpdateNode={handleUpdateNode}
          onDeleteNode={handleDeleteNode}
        />
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept=".json"
        style={{ display: 'none' }}
        onChange={onFileSelected}
      />
      {toast && <div className="toast">{toast}</div>}
    </div>
  );
}
