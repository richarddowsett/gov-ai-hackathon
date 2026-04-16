import { Handle, Position, type NodeProps } from '@xyflow/react';
import type { PageNodeData } from './nodeTypes';

export function StringNode({ data, selected }: NodeProps) {
  const d = data as PageNodeData;
  return (
    <div className={`journey-node${selected ? ' selected' : ''}`}>
      <Handle type="target" position={Position.Top} id="target-top" />
      <Handle type="target" position={Position.Left} id="target-left" />
      <Handle type="target" position={Position.Right} id="target-right" />
      <Handle type="target" position={Position.Bottom} id="target-bottom" />
      <div className="node-header type-string">
        <span className="node-page-number">#{d.pageNumber}</span>
        Text Input
      </div>
      <div className="node-body">
        <div className={`node-title${d.title ? '' : ' placeholder'}`}>
          {d.title || 'Untitled text input'}
        </div>
        <div className="node-preview">
          <div className="preview-input" />
          {d.validation && (
            <div style={{ fontSize: 10, color: '#505a5f', marginTop: 2 }}>
              Regex: {(d.validation as string).substring(0, 30)}
            </div>
          )}
        </div>
      </div>
      <Handle type="source" position={Position.Bottom} id="next" />
      <Handle type="source" position={Position.Right} id="next-right" className="handle-right-source" />
    </div>
  );
}
