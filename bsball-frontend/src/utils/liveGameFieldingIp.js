import { inningsToOuts, outsToInnings } from './inningsFormat.js';

/** Return innings pitched after recording one out using baseball's .0/.1/.2 notation. */
export function addOutToInnings(ip) {
  return outsToInnings(inningsToOuts(Number(ip)) + 1);
}

/** DH and other non-fielding lineup slots must not receive defensive innings. */
export function activeFielders(players, validPositions, normalizePosition = (position) => String(position ?? '').trim().toUpperCase()) {
  if (!Array.isArray(players)) return [];
  return players.filter((player) => {
    const position = normalizePosition(player?.position);
    return position !== 'DH' && validPositions.has(position);
  });
}
