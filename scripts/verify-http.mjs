/** Verify independent whole-word counts over real HTTP; remove all temporary resources on completion. */
const base = (process.env.DLP_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');
const keywords = ['password', 'secret', 'confidential', 'token', 'credential', 'apikey', 'privatekey', 'classified'];

/** Send one bounded request and fail clearly on unexpected HTTP responses. */
async function request(path, method = 'GET', body) {
  const response = await fetch(base + path, {
    method,
    headers: body ? {'Content-Type': 'application/json'} : {},
    body: body ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(15000)
  });
  if (!response.ok) throw new Error(`${method} ${path}: HTTP ${response.status}`);
  return response.status === 204 ? null : response.json();
}
let rule, policy;
try {
  rule = await request('/api/datatypes', 'POST', {name:'Parallel HTTP verification',description:'Temporary',type:'keywords',content:keywords,threshold:3});
  policy = await request('/api/datasets', 'POST', {name:'Parallel HTTP policy',data_type_ids:[rule.id]});
  const durations = [];
  for (let start = 0; start < 120; start += 8) {
    // Settle the entire batch before cleanup, even when one request fails.
    const batch = await Promise.allSettled(Array.from({length:8}, async (_, offset) => {
      const i = start + offset;
      const count = i % 17;
      const begin = performance.now();
      // Cycle through all eight keywords; repeated occurrences each count separately.
      const words = Array.from({length:count}, (_, index) => {
        const word = keywords[(index + i) % keywords.length];
        return index % 2 === 0 ? word.toUpperCase() : word;
      });
      const text = words.join('! ') + ' secretary passwords confidentiality tokens credentials apikeys privatekeys unclassified _secret';
      const result = await request('/api/scan', 'POST', {data_set_id:policy.id,text});
      durations.push(performance.now() - begin);
      if (result.status !== (count >= 3 ? 'match' : 'not matched')) throw new Error(`Incorrect status for case ${i}`);
      if (count >= 3 && (result.detected_objects.length !== 1 || result.detected_objects[0].match_count !== count)) throw new Error(`Incorrect count for case ${i}`);
      if (count < 3 && Object.keys(result).length !== 1) throw new Error(`Unexpected no-match response for case ${i}`);
    }));
    const failure = batch.find(result => result.status === 'rejected');
    if (failure) throw failure.reason;
  }
  durations.sort((a,b) => a-b);
  const report = {passed:120,failed:0,concurrency:8,medianMs:Math.round(durations[60]),p95Ms:Math.round(durations[114]),note:'Local correctness smoke test, not a capacity benchmark'};
  console.log(JSON.stringify(report));
} finally {
  if (policy) await request('/api/datasets/' + policy.id,'DELETE');
  if (rule) await request('/api/datatypes/' + rule.id,'DELETE');
}
