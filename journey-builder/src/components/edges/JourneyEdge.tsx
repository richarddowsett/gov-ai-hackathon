import {
  BaseEdge,
  EdgeLabelRenderer,
  getBezierPath,
  type EdgeProps,
} from '@xyflow/react';

const BRANCH_COLORS: Record<string, string> = {
  true: '#00703c',
  True: '#00703c',
  Yes: '#00703c',
  false: '#d4351c',
  False: '#d4351c',
  No: '#d4351c',
};

const DISPLAY_LABELS: Record<string, string> = {
  true: 'Yes',
  True: 'Yes',
  false: 'No',
  False: 'No',
};

export function JourneyEdge({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  label,
  selected,
  markerEnd,
}: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourcePosition,
    targetPosition,
    curvature: 0.4,
  });

  const rawLabel = typeof label === 'string' ? label : undefined;
  const labelStr = rawLabel ? (DISPLAY_LABELS[rawLabel] ?? rawLabel) : undefined;
  const edgeColor = rawLabel ? (BRANCH_COLORS[rawLabel] ?? '#1d70b8') : '#0b0c0c';
  const strokeWidth = selected ? 3.5 : 2.5;

  return (
    <>
      {/* Wider invisible hit area for easier selection */}
      <path
        d={edgePath}
        fill="none"
        stroke="transparent"
        strokeWidth={20}
        className="react-flow__edge-interaction"
      />
      {/* Glow behind the path for contrast */}
      <path
        d={edgePath}
        fill="none"
        stroke={selected ? '#1d70b8' : edgeColor}
        strokeWidth={strokeWidth + 4}
        strokeOpacity={0.12}
        strokeLinecap="round"
      />
      <BaseEdge
        id={id}
        path={edgePath}
        markerEnd={markerEnd}
        style={{
          stroke: selected ? '#1d70b8' : edgeColor,
          strokeWidth,
          strokeLinecap: 'round',
        }}
      />
      {/* Animated flow dots */}
      <circle r="3" fill={edgeColor} opacity={0.6}>
        <animateMotion dur="2.5s" repeatCount="indefinite" path={edgePath} />
      </circle>
      {labelStr && (
        <EdgeLabelRenderer>
          <div
            className="journey-edge-label"
            style={{
              position: 'absolute',
              transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
              pointerEvents: 'all',
              borderColor: edgeColor,
              color: edgeColor,
            }}
          >
            {labelStr}
          </div>
        </EdgeLabelRenderer>
      )}
    </>
  );
}

export const edgeTypes = {
  journey: JourneyEdge,
};
