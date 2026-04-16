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
          <div className="preview-radio branch-option-row">
            <div className="preview-radio-dot" /> Yes
            <Handle
              type="source"
              position={Position.Right}
              id="branch-true"
              className="handle-branch-inline handle-branch-green"
            />
          </div>
          <div className="preview-radio branch-option-row">
            <div className="preview-radio-dot" /> No
            <Handle
              type="source"
              position={Position.Right}
              id="branch-false"
              className="handle-branch-inline handle-branch-red"
            />
          </div>
        </div>
      </div>
      <Handle type="source" position={Position.Bottom} id="next" />
    </div>
  );
}
