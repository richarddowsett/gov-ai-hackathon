import { Handle, Position, type NodeProps } from '@xyflow/react';
import type { PageNodeData } from './nodeTypes';

export function BooleanNode({ data, selected }: NodeProps) {
  const d = data as PageNodeData;
  return (
    <div className={`journey-node${selected ? ' selected' : ''}`}>
      <Handle type="target" position={Position.Top} id="target-top" />
      <Handle type="target" position={Position.Left} id="target-left" />
      <Handle type="target" position={Position.Right} id="target-right" />
      <Handle type="target" position={Position.Bottom} id="target-bottom" />
      <div className="node-header type-boolean">
        <span className="node-page-number">#{d.pageNumber}</span>
        Yes / No
      </div>
      <div className="node-body">
        <div className={`node-title${d.title ? '' : ' placeholder'}`}>
          {d.title || 'Untitled yes/no question'}
        </div>
        <div className="node-preview">
          <div className="preview-radio"><div className="preview-radio-dot" /> Yes</div>
          <div className="preview-radio"><div className="preview-radio-dot" /> No</div>
        </div>
      </div>
      <div className="branch-handles-section">
        <div className="branch-handle-row">
          <span className="branch-handle-label branch-label-true">True</span>
          <Handle
            type="source"
            position={Position.Bottom}
            id="branch-true"
            style={{ position: 'relative', left: 0, bottom: 0, transform: 'none' }}
          />
        </div>
        <div className="branch-handle-row">
          <span className="branch-handle-label branch-label-false">False</span>
          <Handle
            type="source"
            position={Position.Bottom}
            id="branch-false"
            style={{ position: 'relative', left: 0, bottom: 0, transform: 'none' }}
          />
        </div>
      </div>
      {/* Right-side branch handles for horizontal connections */}
      <Handle type="source" position={Position.Right} id="branch-true-right" className="handle-branch-right" style={{ top: '60%' }} />
      <Handle type="source" position={Position.Right} id="branch-false-right" className="handle-branch-right" style={{ top: '80%' }} />
    </div>
  );
}
