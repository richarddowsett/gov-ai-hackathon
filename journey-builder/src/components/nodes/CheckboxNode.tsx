import { Handle, Position, type NodeProps } from '@xyflow/react';
import type { PageNodeData } from './nodeTypes';

export function CheckboxNode({ data, selected }: NodeProps) {
  const d = data as PageNodeData;
  const options: string[] = (d.options as string[]) ?? ['Option 1', 'Option 2'];
  return (
    <div className={`journey-node${selected ? ' selected' : ''}`}>
      <Handle type="target" position={Position.Top} id="target-top" />
      <Handle type="target" position={Position.Left} id="target-left" />
      <Handle type="target" position={Position.Right} id="target-right" />
      <Handle type="target" position={Position.Bottom} id="target-bottom" />
      <div className="node-header type-checkbox">
        <span className="node-page-number">#{d.pageNumber}</span>
        Checkboxes
      </div>
      <div className="node-body">
        <div className={`node-title${d.title ? '' : ' placeholder'}`}>
          {d.title || 'Untitled checkboxes'}
        </div>
        <div className="node-preview">
          {options.slice(0, 3).map((opt, i) => (
            <div key={i} className="preview-checkbox">
              <div className="preview-checkbox-box" /> {opt}
            </div>
          ))}
          {options.length > 3 && (
            <div style={{ fontSize: 10, color: '#505a5f' }}>+{options.length - 3} more</div>
          )}
        </div>
      </div>
      <Handle type="source" position={Position.Bottom} id="next" />
      <Handle type="source" position={Position.Right} id="next-right" className="handle-right-source" />
    </div>
  );
}
