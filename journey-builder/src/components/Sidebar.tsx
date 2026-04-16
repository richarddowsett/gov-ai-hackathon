import { type DragEvent } from 'react';
import { PAGE_TYPE_LABELS, PAGE_TYPE_DESCRIPTIONS, type PageType } from '../types/journey';

const PAGE_TYPES: PageType[] = [
  'contentPage',
  'string',
  'datePage',
  'boolean',
  'radioButton',
  'checkbox',
  'multipleQuestionsPage',
];

const TYPE_ICONS: Record<PageType, string> = {
  contentPage: 'Aa',
  string: 'T',
  datePage: 'D',
  boolean: 'Y/N',
  radioButton: 'R',
  checkbox: 'C',
  multipleQuestionsPage: 'MQ',
};

export function Sidebar() {
  const onDragStart = (event: DragEvent, pageType: PageType) => {
    event.dataTransfer.setData('application/journeyPageType', pageType);
    event.dataTransfer.effectAllowed = 'move';
  };

  return (
    <div className="sidebar">
      <div className="sidebar-header">
        <h2>GDS Components</h2>
        <p>Drag a component onto the canvas to add a page to your journey.</p>
      </div>
      <div className="sidebar-items">
        {PAGE_TYPES.map((type) => (
          <div
            key={type}
            className="sidebar-item"
            draggable
            onDragStart={(e) => onDragStart(e, type)}
          >
            <div className="sidebar-item-header">
              <div className={`sidebar-item-icon type-${type}`}>{TYPE_ICONS[type]}</div>
              <span className="sidebar-item-name">{PAGE_TYPE_LABELS[type]}</span>
            </div>
            <div className="sidebar-item-desc">{PAGE_TYPE_DESCRIPTIONS[type]}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
