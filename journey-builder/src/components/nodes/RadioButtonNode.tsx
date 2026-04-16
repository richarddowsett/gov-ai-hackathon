import { Handle, Position, type NodeProps } from '@xyflow/react';
import type { PageNodeData } from './nodeTypes';

export function RadioButtonNode({ data, selected }: NodeProps) {
  const d = data as PageNodeData;
  const options: string[] = (d.options as string[]) ?? ['Option 1', 'Option 2'];
  return (
    <div className={`journey-node${selected ? ' selected' : ''}`}>
      <Handle type="target" position={Position.Top} />
      <div className="node-header type-radioButton">
        <span className="node-page-number">#{d.pageNumber}</span>
        Radio Buttons
      </div>
      <div className="node-body">
        <div className={`node-title${d.title ? '' : ' placeholder'}`}>
          {d.title || 'Untitled radio buttons'}
        </div>
        <div className="node-preview">
          {options.slice(0, 3).map((opt, i) => (
            <div key={i} className="preview-radio">
              <div className="preview-radio-dot" /> {opt}
            </div>
          ))}
          {options.length > 3 && (
            <div style={{ fontSize: 10, color: '#505a5f' }}>+{options.length - 3} more</div>
          )}
        </div>
      </div>
      {options.map((opt, i) => (
        <div key={i} className="branch-handle-row">
          <span className="branch-handle-label">{opt}</span>
          <Handle
            type="source"
            position={Position.Bottom}
            id={`branch-${opt}`}
            style={{ position: 'relative', left: 0, bottom: 0, transform: 'none' }}
          />
        </div>
      ))}
    </div>
  );
}
