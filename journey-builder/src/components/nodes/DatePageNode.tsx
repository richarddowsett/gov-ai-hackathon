import { Handle, Position, type NodeProps } from '@xyflow/react';
import type { PageNodeData } from './nodeTypes';

export function DatePageNode({ data, selected }: NodeProps) {
  const d = data as PageNodeData;
  return (
    <div className={`journey-node${selected ? ' selected' : ''}`}>
      <Handle type="target" position={Position.Top} id="target-top" />
      <Handle type="target" position={Position.Left} id="target-left" />
      <Handle type="target" position={Position.Right} id="target-right" />
      <Handle type="target" position={Position.Bottom} id="target-bottom" />
      <div className="node-header type-datePage">
        <span className="node-page-number">#{d.pageNumber}</span>
        Date Input
      </div>
      <div className="node-body">
        <div className={`node-title${d.title ? '' : ' placeholder'}`}>
          {d.title || 'Untitled date input'}
        </div>
        <div className="node-preview">
          <div className="preview-date-group">
            <div className="preview-date-field">
              <label>Day</label>
              <div className="preview-input" />
            </div>
            <div className="preview-date-field">
              <label>Month</label>
              <div className="preview-input" />
            </div>
            <div className="preview-date-field">
              <label>Year</label>
              <div className="preview-input" style={{ flex: 1.5 }} />
            </div>
          </div>
        </div>
      </div>
      <Handle type="source" position={Position.Bottom} id="next" />
      <Handle type="source" position={Position.Right} id="next-right" className="handle-right-source" />
    </div>
  );
}
