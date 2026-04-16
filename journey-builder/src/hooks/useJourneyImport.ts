import { useCallback } from 'react';
import type { Node, Edge } from '@xyflow/react';
import type { Journey, Page, PageType } from '../types/journey';
import { isBranchingType } from '../types/journey';
import type { PageNodeData } from '../components/nodes/nodeTypes';
import { getLayoutedElements } from '../utils/autoLayout';

let nodeIdCounter = 0;

export function useJourneyImport() {
  const importJourney = useCallback(
    (journey: Journey): { nodes: Node[]; edges: Edge[]; nextId: number } => {
      const nodes: Node[] = [];
      const edges: Edge[] = [];

      const pageNumberToNodeId = new Map<number, string>();

      journey.pages.forEach((page, arrayIndex) => {
        const nodeId = `node-${++nodeIdCounter}`;
        const pageNumber = arrayIndex + 1;
        pageNumberToNodeId.set(pageNumber, nodeId);

        const data: PageNodeData = {
          pageType: page.type as PageType,
          title: page.title,
          pageNumber,
        };

        if ('options' in page && page.options) {
          data.options = page.options;
        }

        if ('questions' in page && page.questions) {
          data.questions = page.questions;
        }

        if ('validation' in page && page.validation !== undefined) {
          data.validation = page.validation;
        }

        nodes.push({
          id: nodeId,
          type: page.type,
          position: { x: 0, y: 0 },
          data,
        });
      });

      journey.pages.forEach((page, arrayIndex) => {
        const pageNumber = arrayIndex + 1;
        const sourceId = pageNumberToNodeId.get(pageNumber)!;

        const markerEnd = { type: 'arrowclosed' as const, width: 16, height: 16, color: '#0b0c0c' };

        if (page.type === 'boolean') {
          const boolPage = page as { index: { true: number; false: number } };
          const trueTargetId = pageNumberToNodeId.get(boolPage.index.true);
          const falseTargetId = pageNumberToNodeId.get(boolPage.index.false);

          if (trueTargetId) {
            edges.push({
              id: `edge-${sourceId}-true`,
              source: sourceId,
              target: trueTargetId,
              sourceHandle: 'branch-true',
              label: 'true',
              type: 'journey',
              markerEnd,
            });
          }
          if (falseTargetId) {
            edges.push({
              id: `edge-${sourceId}-false`,
              source: sourceId,
              target: falseTargetId,
              sourceHandle: 'branch-false',
              label: 'false',
              type: 'journey',
              markerEnd,
            });
          }
        } else if (page.type === 'radioButton') {
          const radioPage = page as { index: Record<string, number>; options: string[] };
          Object.entries(radioPage.index).forEach(([option, targetPageNum]) => {
            const targetId = pageNumberToNodeId.get(targetPageNum);
            if (targetId) {
              edges.push({
                id: `edge-${sourceId}-${option}`,
                source: sourceId,
                target: targetId,
                sourceHandle: `branch-${option}`,
                label: option,
                type: 'journey',
                markerEnd,
              });
            }
          });
        } else {
          const linearPage = page as { index: number };
          const targetId = pageNumberToNodeId.get(linearPage.index);
          if (targetId) {
            edges.push({
              id: `edge-${sourceId}-next`,
              source: sourceId,
              target: targetId,
              sourceHandle: 'next',
              type: 'journey',
              markerEnd,
            });
          }
        }
      });

      const laid = getLayoutedElements(nodes, edges);
      return { nodes: laid.nodes, edges: laid.edges, nextId: nodeIdCounter };
    },
    []
  );

  return { importJourney };
}

export function resetNodeIdCounter(val: number = 0) {
  nodeIdCounter = val;
}

export function getNextNodeId(): string {
  return `node-${++nodeIdCounter}`;
}
