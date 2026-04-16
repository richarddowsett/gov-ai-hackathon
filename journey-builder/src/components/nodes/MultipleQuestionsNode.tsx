import { Handle, Position, type NodeProps } from '@xyflow/react';
import type { PageNodeData } from './nodeTypes';

interface QuestionData {
  questionTitle: string;
}

export function MultipleQuestionsNode({ data, selected }: NodeProps) {
  const d = data as PageNodeData;
  const questions: QuestionData[] = (d.questions as QuestionData[]) ?? [
    { questionTitle: '' },
    { questionTitle: '' },
  ];
  return (
    <div className={`journey-node${selected ? ' selected' : ''}`}>
      <Handle type="target" position={Position.Top} id="target-top" />
      <Handle type="target" position={Position.Left} id="target-left" />
      <Handle type="target" position={Position.Right} id="target-right" />
      <Handle type="target" position={Position.Bottom} id="target-bottom" />
      <div className="node-header type-multipleQuestionsPage">
        <span className="node-page-number">#{d.pageNumber}</span>
        Multiple Questions
      </div>
      <div className="node-body">
        <div className={`node-title${d.title ? '' : ' placeholder'}`}>
          {d.title || 'Untitled multiple questions'}
        </div>
        <div className="node-preview">
          {questions.map((q, i) => (
            <div key={i} className="preview-question">
              Q{i + 1}: {q.questionTitle || 'Untitled question'}
            </div>
          ))}
        </div>
      </div>
      <Handle type="source" position={Position.Bottom} id="next" />
      <Handle type="source" position={Position.Right} id="next-right" className="handle-right-source" />
    </div>
  );
}
