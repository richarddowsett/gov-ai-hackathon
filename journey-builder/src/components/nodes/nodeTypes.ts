import type { PageType } from '../../types/journey';
import { ContentPageNode } from './ContentPageNode';
import { StringNode } from './StringNode';
import { DatePageNode } from './DatePageNode';
import { BooleanNode } from './BooleanNode';
import { RadioButtonNode } from './RadioButtonNode';
import { CheckboxNode } from './CheckboxNode';
import { MultipleQuestionsNode } from './MultipleQuestionsNode';

export interface PageNodeData extends Record<string, unknown> {
  pageType: PageType;
  title: string;
  pageNumber: number;
  options?: string[];
  questions?: { questionTitle: string }[];
  validation?: string | string[];
}

export const nodeTypes = {
  contentPage: ContentPageNode,
  string: StringNode,
  datePage: DatePageNode,
  boolean: BooleanNode,
  radioButton: RadioButtonNode,
  checkbox: CheckboxNode,
  multipleQuestionsPage: MultipleQuestionsNode,
};
