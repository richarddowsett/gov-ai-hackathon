import {
  BaseEdge,
  EdgeLabelRenderer,
  getBezierPath,
  type EdgeProps,
} from '@xyflow/react';

const BRANCH_COLORS: Record<string, string> = {
  True: '#00703c',
  true: '#00703c',
  False: '#d4351c',
  false: '#d4351c',
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

  const labelStr = typeof label === 'string' ? label : undefined;
  const edgeColor = labelStr ? (BRANCH_COLORS[labelStr] ?? '#1d70b8') : '#0b0c0c';
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
