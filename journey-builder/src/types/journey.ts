export type PageType =
  | 'contentPage'
  | 'string'
  | 'datePage'
  | 'boolean'
  | 'radioButton'
  | 'checkbox'
  | 'multipleQuestionsPage';

export interface Question {
  questionTitle: string;
}

export interface BasePage {
  type: PageType;
  title: string;
}

export interface ContentPage extends BasePage {
  type: 'contentPage';
  index: number;
}

export interface StringPage extends BasePage {
  type: 'string';
  index: number;
  validation?: string;
}

export interface DatePage extends BasePage {
  type: 'datePage';
  index: number;
}

export interface BooleanPage extends BasePage {
  type: 'boolean';
  index: { true: number; false: number };
}

export interface RadioButtonPage extends BasePage {
  type: 'radioButton';
  index: Record<string, number>;
  options: string[];
}

export interface CheckboxPage extends BasePage {
  type: 'checkbox';
  index: number;
  options: string[];
}

export interface MultipleQuestionsPage extends BasePage {
  type: 'multipleQuestionsPage';
  index: number;
  questions: [Question, Question];
  validation?: [string, string];
}

export type Page =
  | ContentPage
  | StringPage
  | DatePage
  | BooleanPage
  | RadioButtonPage
  | CheckboxPage
  | MultipleQuestionsPage;

export interface Journey {
  pages: Page[];
}

export const PAGE_TYPE_LABELS: Record<PageType, string> = {
  contentPage: 'Content Page',
  string: 'Text Input',
  datePage: 'Date Input',
  boolean: 'Yes / No',
  radioButton: 'Radio Buttons',
  checkbox: 'Checkboxes',
  multipleQuestionsPage: 'Multiple Questions',
};

export const PAGE_TYPE_DESCRIPTIONS: Record<PageType, string> = {
  contentPage: 'Static content with no form inputs',
  string: 'Single text input with optional validation',
  datePage: 'Day / month / year date input',
  boolean: 'Yes or No question with branching',
  radioButton: 'Single choice from options with branching',
  checkbox: 'Multiple choice from options',
  multipleQuestionsPage: 'Two sub-questions on one page',
};

export function isBranchingType(type: PageType): boolean {
  return type === 'boolean' || type === 'radioButton';
}

export interface DefaultPageFields {
  type: PageType;
  title: string;
  options?: string[];
  questions?: { questionTitle: string }[];
}

export function defaultPageData(type: PageType): DefaultPageFields {
  switch (type) {
    case 'contentPage':
      return { type, title: '' };
    case 'string':
      return { type, title: '' };
    case 'datePage':
      return { type, title: '' };
    case 'boolean':
      return { type, title: '' };
    case 'radioButton':
      return { type, title: '', options: ['Option 1', 'Option 2'] };
    case 'checkbox':
      return { type, title: '', options: ['Option 1', 'Option 2'] };
    case 'multipleQuestionsPage':
      return {
        type,
        title: '',
        questions: [{ questionTitle: '' }, { questionTitle: '' }],
      };
  }
}
