import { type Node } from '@xyflow/react';
import type { PageNodeData } from './nodes/nodeTypes';
import { PAGE_TYPE_LABELS, isBranchingType, type PageType } from '../types/journey';

interface Props {
  selectedNode: Node | null;
  onUpdateNode: (id: string, data: Partial<PageNodeData>) => void;
  onDeleteNode: (id: string) => void;
}

const TYPES_WITH_SINGLE_VALIDATION: PageType[] = ['string', 'datePage', 'checkbox', 'radioButton'];

const VALIDATION_HINTS: Partial<Record<PageType, string>> = {
  string: 'Regex for text input (e.g. ^[a-zA-Z\\s]{1,100}$)',
  datePage: 'Regex for date validation (e.g. ^\\d{2}/\\d{2}/\\d{4}$)',
  checkbox: 'Validation rule for selected options',
  radioButton: 'Validation rule for selected option',
};

export function PropertiesPanel({ selectedNode, onUpdateNode, onDeleteNode }: Props) {
  if (!selectedNode) {
    return (
      <div className="properties-panel">
        <div className="panel-empty">
          <h3>No page selected</h3>
          <p>Click a page on the canvas to edit its properties, or drag a component from the sidebar to add a new page.</p>
        </div>
      </div>
    );
  }

  const data = selectedNode.data as PageNodeData;
  const pageType = data.pageType;

  const update = (changes: Partial<PageNodeData>) => {
    onUpdateNode(selectedNode.id, changes);
  };

  const showSingleValidation = TYPES_WITH_SINGLE_VALIDATION.includes(pageType);

  return (
    <div className="properties-panel">
      <div className="panel-header">
        <h2>{PAGE_TYPE_LABELS[pageType]}</h2>
        <span style={{ fontSize: 12, color: '#505a5f' }}>Page #{data.pageNumber}</span>
      </div>
      <div className="panel-body">
        <div className="panel-field">
          <label htmlFor="page-title">Title</label>
          <input
            id="page-title"
            type="text"
            value={data.title}
            maxLength={100}
            placeholder="Enter page title..."
            onChange={(e) => update({ title: e.target.value })}
          />
          <span className="panel-field-hint">{data.title.length}/100 characters</span>
        </div>

        {(pageType === 'radioButton' || pageType === 'checkbox') && (
          <OptionsEditor
            options={data.options ?? []}
            onChange={(options) => update({ options })}
            isBranching={isBranchingType(pageType)}
          />
        )}

        {showSingleValidation && (
          <div className="panel-field">
            <label htmlFor="validation">Validation Regex</label>
            <input
              id="validation"
              type="text"
              value={(data.validation as string) ?? ''}
              maxLength={100}
              placeholder={VALIDATION_HINTS[pageType] ?? 'Optional regex pattern'}
              onChange={(e) => update({ validation: e.target.value || undefined })}
            />
            <span className="panel-field-hint">Optional regular expression for input validation</span>
          </div>
        )}

        {pageType === 'multipleQuestionsPage' && (
          <QuestionsEditor
            questions={data.questions ?? [{ questionTitle: '' }, { questionTitle: '' }]}
            validation={(data.validation as string[]) ?? undefined}
            onChange={(questions, validation) => update({ questions, validation })}
          />
        )}

        <div className="panel-delete-section">
          <button
            className="gds-btn gds-btn-danger gds-btn-small"
            onClick={() => onDeleteNode(selectedNode.id)}
          >
            Delete Page
          </button>
        </div>
      </div>
    </div>
  );
}

function OptionsEditor({
  options,
  onChange,
  isBranching,
}: {
  options: string[];
  onChange: (options: string[]) => void;
  isBranching: boolean;
}) {
  const updateOption = (index: number, value: string) => {
    const next = [...options];
    next[index] = value;
    onChange(next);
  };

  const addOption = () => {
    onChange([...options, `Option ${options.length + 1}`]);
  };

  const removeOption = (index: number) => {
    if (options.length <= 1) return;
    onChange(options.filter((_, i) => i !== index));
  };

  return (
    <div>
      <div className="panel-section-title">Options</div>
      {isBranching && (
        <span className="panel-field-hint" style={{ display: 'block', marginBottom: 8 }}>
          Each option creates a separate output connection for branching.
        </span>
      )}
      <div className="options-list">
        {options.map((opt, i) => (
          <div key={i} className="option-row">
            <input
              type="text"
              value={opt}
              maxLength={50}
              placeholder={`Option ${i + 1}`}
              onChange={(e) => updateOption(i, e.target.value)}
            />
            {options.length > 1 && (
              <button className="option-remove-btn" onClick={() => removeOption(i)}>
                &times;
              </button>
            )}
          </div>
        ))}
      </div>
      <button
        className="gds-btn gds-btn-secondary gds-btn-small"
        style={{ marginTop: 8 }}
        onClick={addOption}
      >
        + Add option
      </button>
    </div>
  );
}

function QuestionsEditor({
  questions,
  validation,
  onChange,
}: {
  questions: { questionTitle: string }[];
  validation?: string[];
  onChange: (
    questions: { questionTitle: string }[],
    validation?: string[]
  ) => void;
}) {
  const updateQuestion = (index: number, title: string) => {
    const next = [...questions];
    next[index] = { questionTitle: title };
    onChange(next, validation);
  };

  const updateValidation = (index: number, value: string) => {
    const next = validation ? [...validation] : ['', ''];
    next[index] = value;
    const hasValues = next.some((v) => v.trim() !== '');
    onChange(questions, hasValues ? (next as [string, string]) : undefined);
  };

  return (
    <div>
      <div className="panel-section-title">Questions</div>
      {questions.map((q, i) => (
        <div key={i} style={{ marginBottom: 12 }}>
          <div className="panel-field">
            <label>Question {i + 1} Title</label>
            <input
              type="text"
              value={q.questionTitle}
              maxLength={100}
              placeholder={`Enter question ${i + 1}...`}
              onChange={(e) => updateQuestion(i, e.target.value)}
            />
          </div>
          <div className="panel-field" style={{ marginTop: 4 }}>
            <label>Question {i + 1} Validation</label>
            <input
              type="text"
              value={validation?.[i] ?? ''}
              maxLength={100}
              placeholder="Optional regex"
              onChange={(e) => updateValidation(i, e.target.value)}
            />
          </div>
        </div>
      ))}
    </div>
  );
}
