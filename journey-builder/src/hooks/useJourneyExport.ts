import { useCallback } from 'react';
import type { Node, Edge } from '@xyflow/react';
import type { PageNodeData } from '../components/nodes/nodeTypes';
import type { Journey, Page } from '../types/journey';
import { isBranchingType } from '../types/journey';

export function useJourneyExport() {
  const exportJourney = useCallback(
    (nodes: Node[], edges: Edge[]): Journey => {
      const sortedNodes = [...nodes].sort((a, b) => {
        const aNum = (a.data as PageNodeData).pageNumber;
        const bNum = (b.data as PageNodeData).pageNumber;
        return aNum - bNum;
      });

      const nodeIdToPageNumber = new Map<string, number>();
      sortedNodes.forEach((node) => {
        const d = node.data as PageNodeData;
        nodeIdToPageNumber.set(node.id, d.pageNumber);
      });

      const pages: Page[] = sortedNodes.map((node) => {
        const d = node.data as PageNodeData;
        const outgoingEdges = edges.filter((e) => e.source === node.id);

        if (d.pageType === 'boolean') {
          const trueEdge = outgoingEdges.find((e) => e.sourceHandle === 'branch-true');
          const falseEdge = outgoingEdges.find((e) => e.sourceHandle === 'branch-false');
          const trueTarget = trueEdge ? nodeIdToPageNumber.get(trueEdge.target) ?? 0 : 0;
          const falseTarget = falseEdge ? nodeIdToPageNumber.get(falseEdge.target) ?? 0 : 0;

          return {
            type: 'boolean' as const,
            title: d.title,
            index: { true: trueTarget, false: falseTarget },
          };
        }

        if (d.pageType === 'radioButton') {
          const options = d.options ?? [];
          const indexMap: Record<string, number> = {};
          options.forEach((opt) => {
            const edge = outgoingEdges.find((e) => e.sourceHandle === `branch-${opt}`);
            const target = edge ? nodeIdToPageNumber.get(edge.target) ?? 0 : 0;
            indexMap[opt] = target;
          });

          return {
            type: 'radioButton' as const,
            title: d.title,
            index: indexMap,
            options,
          };
        }

        const nextEdge = outgoingEdges.find((e) => e.sourceHandle === 'next');
        const nextIndex = nextEdge ? nodeIdToPageNumber.get(nextEdge.target) ?? 0 : 0;

        const base = { title: d.title, index: nextIndex };

        switch (d.pageType) {
          case 'contentPage':
            return { type: 'contentPage' as const, ...base };
          case 'string': {
            const page: Page = { type: 'string' as const, ...base };
            if (d.validation && typeof d.validation === 'string') {
              (page as { validation?: string }).validation = d.validation;
            }
            return page;
          }
          case 'datePage':
            return { type: 'datePage' as const, ...base };
          case 'checkbox':
            return { type: 'checkbox' as const, ...base, options: d.options ?? [] };
          case 'multipleQuestionsPage': {
            const qs = d.questions ?? [{ questionTitle: '' }, { questionTitle: '' }];
            const page: Page = {
              type: 'multipleQuestionsPage' as const,
              ...base,
              questions: [qs[0], qs[1]] as [{ questionTitle: string }, { questionTitle: string }],
            };
            if (d.validation && Array.isArray(d.validation)) {
              (page as { validation?: [string, string] }).validation = d.validation as [string, string];
            }
            return page;
          }
          default:
            return { type: d.pageType, ...base } as Page;
        }
      });

      return { pages };
    },
    []
  );

  const downloadJson = useCallback((journey: Journey) => {
    const json = JSON.stringify(journey, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'journey.json';
    a.click();
    URL.revokeObjectURL(url);
  }, []);

  return { exportJourney, downloadJson };
}

export function getWarnings(nodes: Node[], edges: Edge[]): string[] {
  const warnings: string[] = [];

  if (nodes.length === 0) {
    return ['Add at least one page to your journey'];
  }

  nodes.forEach((node) => {
    const d = node.data as PageNodeData;
    if (!d.title.trim()) {
      warnings.push(`Page #${d.pageNumber}: title is empty`);
    }

    if ((d.pageType === 'radioButton' || d.pageType === 'checkbox') && (!d.options || d.options.length === 0)) {
      warnings.push(`Page #${d.pageNumber}: needs at least one option`);
    }

    if (d.pageType === 'multipleQuestionsPage') {
      const qs = d.questions ?? [];
      qs.forEach((q, i) => {
        if (!q.questionTitle.trim()) {
          warnings.push(`Page #${d.pageNumber}: question ${i + 1} title is empty`);
        }
      });
    }

    const outgoing = edges.filter((e) => e.source === node.id);
    if (isBranchingType(d.pageType)) {
      if (d.pageType === 'boolean') {
        if (!outgoing.find((e) => e.sourceHandle === 'branch-true')) {
          warnings.push(`Page #${d.pageNumber}: missing "True" connection`);
        }
        if (!outgoing.find((e) => e.sourceHandle === 'branch-false')) {
          warnings.push(`Page #${d.pageNumber}: missing "False" connection`);
        }
      }
      if (d.pageType === 'radioButton') {
        (d.options ?? []).forEach((opt) => {
          if (!outgoing.find((e) => e.sourceHandle === `branch-${opt}`)) {
            warnings.push(`Page #${d.pageNumber}: missing connection for "${opt}"`);
          }
        });
      }
    } else {
      if (outgoing.length === 0) {
        warnings.push(`Page #${d.pageNumber}: no outgoing connection`);
      }
    }
  });

  const incoming = new Set(edges.map((e) => e.target));
  nodes.forEach((node) => {
    const d = node.data as PageNodeData;
    if (d.pageNumber !== 1 && !incoming.has(node.id)) {
      warnings.push(`Page #${d.pageNumber}: unreachable (no incoming connections)`);
    }
  });

  return warnings;
}
