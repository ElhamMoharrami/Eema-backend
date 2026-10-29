// Isolated local HTTP verification only. Load with NODE_OPTIONS=--require=<this file>.
// All outgoing fetch calls are either a synthetic Cala reply or fail closed.
globalThis.fetch = async (url) => {
  const target = new URL(String(url));
  if (target.origin !== 'https://api.cala.ai') throw new Error('QA forbids outbound request');
  const hit = { id: 'synthetic-company', name: 'Different Synthetic Company', entity_type: 'Company' };
  return new Response(JSON.stringify(target.pathname.endsWith('/entities')
    ? { entities: [hit] }
    : { ...hit, unnecessaryData: 'SYNTHETIC_RAW_MARKER' }),
  { status: 200, headers: { 'content-type': 'application/json' } });
};
