import { Handle, Position, type NodeProps } from '@xyflow/react';
import type { PageNodeData } from './nodeTypes';

export function ContentPageNode({ data, selected }: NodeProps) {
  const d = data as PageNodeData;
  return (
    <div className={`journey-node${selected ? ' selected' : ''}`}>
      <Handle type="target" position={Position.Top} />
      <div className="node-header type-contentPage">
        <span className="node-page-number">#{d.pageNumber}</span>
        Content Page
      </div>
      <div className="node-body">
        <div className={`node-title${d.title ? '' : ' placeholder'}`}>
          {d.title || 'Untitled content page'}
        </div>
        <div className="node-preview">
          <div className="preview-content-text">Static content / information page</div>
        </div>
      </div>
      <Handle type="source" position={Position.Bottom} id="next" />
    </div>
  );
}
