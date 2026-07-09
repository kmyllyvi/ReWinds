/**
 * Personal ops utility — Linear housekeeping. NOT part of the app build
 * (this is a Kotlin/Compose Multiplatform project); it's a standalone
 * Node/TypeScript script run by hand. Deps live in the repo-root
 * package.json (tsx + dotenv); run via `npm run export-linear-done`.
 *
 * Archives Linear issues to local markdown files, for safekeeping before
 * deleting them from Linear (e.g. to free up a workspace's issue-count
 * ceiling). Talks to Linear's GraphQL API directly — no MCP connection
 * needed, so it's fast (one paginated query + writes) instead of one
 * tool-call round-trip per issue.
 *
 * Usage:
 *   npm run export-linear-done
 *   npx tsx scripts/export-linear-done.ts --project "ReWinds app" --state Done --out docs/human/linear-archive
 *
 * Flags (all optional, defaults shown):
 *   --project "ReWinds app"
 *   --state   Done
 *   --out     docs/human/linear-archive
 *
 * Requires LINEAR_API_KEY in .env or the environment — a personal API key
 * from Linear Settings → API. This does NOT delete anything from Linear;
 * deletion stays a manual, deliberate step for a human.
 */
import { config } from 'dotenv';
import { resolve, dirname, join } from 'path';
import { fileURLToPath } from 'url';
import { mkdirSync, writeFileSync } from 'fs';
config({ path: resolve(dirname(fileURLToPath(import.meta.url)), '../.env') });

const LINEAR_API_URL = 'https://api.linear.app/graphql';

interface CliArgs {
  project: string;
  state: string;
  out: string;
}

function parseArgs(argv: string[]): CliArgs {
  const args: CliArgs = { project: 'ReWinds app', state: 'Done', out: 'docs/human/linear-archive' };
  for (let i = 0; i < argv.length; i++) {
    const flag = argv[i];
    const value = argv[i + 1];
    if (flag === '--project' && value) { args.project = value; i++; }
    else if (flag === '--state' && value) { args.state = value; i++; }
    else if (flag === '--out' && value) { args.out = value; i++; }
  }
  return args;
}

interface LinearUser {
  name: string;
}

interface LinearComment {
  body: string;
  createdAt: string;
  user: LinearUser | null;
}

interface LinearRelation {
  type: string;
  relatedIssue: { identifier: string; title: string };
}

interface LinearIssue {
  identifier: string;
  title: string;
  description: string | null;
  url: string;
  priorityLabel: string;
  createdAt: string;
  completedAt: string | null;
  state: { name: string };
  labels: { nodes: { name: string }[] };
  parent: { identifier: string; title: string } | null;
  relations: { nodes: LinearRelation[] };
  comments: { nodes: LinearComment[] };
}

const ISSUES_QUERY = `
  query Issues($after: String, $project: String!, $state: String!) {
    issues(
      first: 50
      after: $after
      filter: { project: { name: { eq: $project } }, state: { name: { eq: $state } } }
      orderBy: createdAt
    ) {
      nodes {
        identifier
        title
        description
        url
        priorityLabel
        createdAt
        completedAt
        state { name }
        labels { nodes { name } }
        parent { identifier title }
        relations { nodes { type relatedIssue { identifier title } } }
        comments { nodes { body createdAt user { name } } }
      }
      pageInfo { hasNextPage endCursor }
    }
  }
`;

async function fetchAllIssues(apiKey: string, project: string, state: string): Promise<LinearIssue[]> {
  const issues: LinearIssue[] = [];
  let after: string | undefined;
  for (;;) {
    const res = await fetch(LINEAR_API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: apiKey },
      body: JSON.stringify({ query: ISSUES_QUERY, variables: { after, project, state } }),
    });
    if (!res.ok) {
      throw new Error(`Linear API returned ${res.status}: ${await res.text()}`);
    }
    const json = await res.json() as { data?: { issues: { nodes: LinearIssue[]; pageInfo: { hasNextPage: boolean; endCursor: string } } }; errors?: unknown };
    if (json.errors) {
      throw new Error(`Linear API errors: ${JSON.stringify(json.errors)}`);
    }
    const page = json.data!.issues;
    issues.push(...page.nodes);
    if (!page.pageInfo.hasNextPage) break;
    after = page.pageInfo.endCursor;
  }
  return issues;
}

function slugify(title: string): string {
  return title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 80);
}

function renderIssue(issue: LinearIssue): string {
  const labels = issue.labels.nodes.map(l => l.name).join(', ') || '_none_';
  const lines = [
    `# ${issue.identifier}: ${issue.title}`,
    '',
    `**Status:** ${issue.state.name} · **Priority:** ${issue.priorityLabel} · **Labels:** ${labels}`,
    `**Created:** ${issue.createdAt} · **Completed:** ${issue.completedAt ?? '_not set_'}`,
    `**Linear URL:** ${issue.url}`,
  ];
  if (issue.parent) {
    lines.push(`**Parent:** ${issue.parent.identifier} — ${issue.parent.title}`);
  }
  if (issue.relations.nodes.length > 0) {
    const related = issue.relations.nodes.map(r => `${r.type}: ${r.relatedIssue.identifier} — ${r.relatedIssue.title}`).join('; ');
    lines.push(`**Related:** ${related}`);
  }
  lines.push('', '## Description', '', issue.description?.trim() || '_No description._', '', '## Comments', '');
  if (issue.comments.nodes.length === 0) {
    lines.push('_No comments._');
  } else {
    for (const c of issue.comments.nodes) {
      lines.push(`### ${c.user?.name ?? 'Unknown'} — ${c.createdAt}`, '', c.body.trim(), '');
    }
  }
  return lines.join('\n') + '\n';
}

function renderIndex(issues: LinearIssue[], project: string, state: string): string {
  const rows = issues.map(i => {
    const file = `${i.identifier}-${slugify(i.title)}.md`;
    return `| ${i.identifier} | [${i.title}](./${file}) | ${i.state.name} | ${i.completedAt ?? '_not set_'} |`;
  });
  return [
    `# Linear archive: ${project} — ${state}`,
    '',
    `Exported ${issues.length} issues via \`scripts/export-linear-done.ts\`. Read-only snapshot for safekeeping before deleting these issues from Linear.`,
    '',
    '| ID | Title | Status | Completed |',
    '|---|---|---|---|',
    ...rows,
    '',
  ].join('\n');
}

async function main() {
  const apiKey = process.env.LINEAR_API_KEY;
  if (!apiKey) {
    console.error('LINEAR_API_KEY not set (add it to .env — Linear Settings → API → Personal API keys).');
    process.exit(1);
  }
  const { project, state, out } = parseArgs(process.argv.slice(2));

  console.log(`Fetching "${state}" issues in project "${project}"...`);
  const issues = await fetchAllIssues(apiKey, project, state);
  console.log(`Found ${issues.length} issues.`);
  if (issues.length === 0) return;

  const outDir = resolve(process.cwd(), out);
  mkdirSync(outDir, { recursive: true });

  for (const issue of issues) {
    const file = join(outDir, `${issue.identifier}-${slugify(issue.title)}.md`);
    writeFileSync(file, renderIssue(issue));
    console.log(`  wrote ${file}`);
  }

  const indexFile = join(outDir, 'README.md');
  writeFileSync(indexFile, renderIndex(issues, project, state));
  console.log(`Wrote index ${indexFile}`);
  console.log(`\nDone. ${issues.length} issues archived to ${outDir}. Nothing was deleted from Linear.`);
}

main().catch(err => { console.error(err); process.exit(1); });
