import { Handle, Position, type NodeProps } from '@xyflow/react';
import type { PageNodeData } from './nodeTypes';

const OPTION_COLORS = [
  'handle-branch-blue',
  'handle-branch-orange',
  'handle-branch-purple',
  'handle-branch-teal',
  'handle-branch-pink',
  'handle-branch-green',
  'handle-branch-red',
];

export function RadioButtonNode({ data, selected }: NodeProps) {
  const d = data as PageNodeData;
  const options: string[] = (d.options as string[]) ?? ['Option 1', 'Option 2'];
  return (
    <div className={`journey-node${selected ? ' selected' : ''}`}>
      <Handle type="target" position={Position.Top} id="target-top" />
      <Handle type="target" position={Position.Left} id="target-left" />
      <Handle type="target" position={Position.Right} id="target-right" />
      <Handle type="target" position={Position.Bottom} id="target-bottom" />
      <div className="node-header type-radioButton">
        <span className="node-page-number">#{d.pageNumber}</span>
        Radio Buttons
      </div>
      <div className="node-body">
        <div className={`node-title${d.title ? '' : ' placeholder'}`}>
          {d.title || 'Untitled radio buttons'}
        </div>
        <div className="node-preview">
          {options.map((opt, i) => (
            <div key={i} className="preview-radio branch-option-row">
              <div className="preview-radio-dot" /> {opt}
              <Handle
                type="source"
                position={Position.Right}
                id={`branch-${opt}`}
                className={`handle-branch-inline ${OPTION_COLORS[i % OPTION_COLORS.length]}`}
              />
            </div>
          ))}
        </div>
      </div>
      <Handle type="source" position={Position.Bottom} id="next" />
    </div>
  );
}
