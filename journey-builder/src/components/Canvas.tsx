import { useCallback, useRef, type DragEvent, type Dispatch, type SetStateAction } from 'react';
import {
  ReactFlow,
  MiniMap,
  Controls,
  Background,
  BackgroundVariant,
  addEdge,
  type Connection,
  type Node,
  type Edge,
  type OnNodesChange,
  type OnEdgesChange,
  type ReactFlowInstance,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { nodeTypes } from './nodes/nodeTypes';
import type { PageNodeData } from './nodes/nodeTypes';
import { defaultPageData, type PageType } from '../types/journey';
import { getNextNodeId } from '../hooks/useJourneyImport';

interface Props {
  nodes: Node[];
  edges: Edge[];
  onNodesChange: OnNodesChange<Node>;
  onEdgesChange: OnEdgesChange<Edge>;
  setNodes: Dispatch<SetStateAction<Node[]>>;
  setEdges: Dispatch<SetStateAction<Edge[]>>;
  onNodeSelect: (node: Node | null) => void;
}

export function Canvas({
  nodes,
  edges,
  onNodesChange,
  onEdgesChange,
  setNodes,
  setEdges,
  onNodeSelect,
}: Props) {
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const reactFlowInstance = useRef<ReactFlowInstance | null>(null);

  const onConnect = useCallback(
    (connection: Connection) => {
      const label = connection.sourceHandle?.startsWith('branch-')
        ? connection.sourceHandle.replace('branch-', '')
        : undefined;
      setEdges((eds: Edge[]) =>
        addEdge(
          {
            ...connection,
            id: `edge-${connection.source}-${connection.sourceHandle}-${connection.target}`,
            label,
            type: 'default',
          },
          eds
        )
      );
    },
    [setEdges]
  );

  const onDragOver = useCallback((event: DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  const onDrop = useCallback(
    (event: DragEvent) => {
      event.preventDefault();
      const pageType = event.dataTransfer.getData('application/journeyPageType') as PageType;
      if (!pageType) return;

      const bounds = reactFlowWrapper.current?.getBoundingClientRect();
      if (!bounds || !reactFlowInstance.current) return;

      const position = reactFlowInstance.current.screenToFlowPosition({
        x: event.clientX - bounds.left,
        y: event.clientY - bounds.top,
      });

      const pageNumber =
        nodes.reduce((max, n) => Math.max(max, (n.data as PageNodeData).pageNumber), 0) + 1;

      const pageData = defaultPageData(pageType);

      const newNode: Node = {
        id: getNextNodeId(),
        type: pageType,
        position,
        data: {
          pageType,
          pageNumber,
          title: pageData.title,
          options: pageData.options,
          questions: pageData.questions,
        } as PageNodeData,
      };

      setNodes((nds: Node[]) => [...nds, newNode]);
    },
    [nodes, setNodes]
  );

  const onNodeClick = useCallback(
    (_: React.MouseEvent, node: Node) => {
      onNodeSelect(node);
    },
    [onNodeSelect]
  );

  const onPaneClick = useCallback(() => {
    onNodeSelect(null);
  }, [onNodeSelect]);

  return (
    <div className="canvas-wrapper" ref={reactFlowWrapper}>
      {nodes.length === 0 && (
        <div className="empty-canvas">
          <h2>Start building your journey</h2>
          <p>
            Drag a GDS component from the sidebar
            <br />
            and drop it here to create your first page.
          </p>
        </div>
      )}
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onDrop={onDrop}
        onDragOver={onDragOver}
        onNodeClick={onNodeClick}
        onPaneClick={onPaneClick}
        onInit={(instance) => {
          reactFlowInstance.current = instance;
        }}
        nodeTypes={nodeTypes}
        fitView
        deleteKeyCode={['Backspace', 'Delete']}
        snapToGrid
        snapGrid={[20, 20]}
      >
        <Controls />
        <MiniMap zoomable pannable />
        <Background variant={BackgroundVariant.Dots} gap={20} size={1} />
      </ReactFlow>
    </div>
  );
}
