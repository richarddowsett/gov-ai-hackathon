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
      <Handle type="target" position={Position.Top} />
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
    </div>
  );
}
