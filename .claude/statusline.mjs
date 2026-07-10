#!/usr/bin/env node
// Custom Claude Code statusline: 5h/weekly usage %, reset time, context %.
// Anthropic doesn't publish exact token caps for Pro, so the limits below are
// rough estimates you should tune to match what you actually observe hitting a wall.
import { readFileSync } from 'node:fs';
import { execSync } from 'node:child_process';

const LIMITS = {
	blockTokens: 26_400_000, // calibrated from /usage (10% at 2.64M tokens used)
	weeklyTokens: 302_000_000, // calibrated from /usage (~5% at 15.1M tokens used)
};

// ccusage rounds the 5h block boundary to the top of the hour, which runs
// ahead of Anthropic's real reset clock. Observed offset vs /usage: ~10min.
const RESET_OFFSET_MS = 10 * 60 * 1000;

function readStdin() {
	try {
		return JSON.parse(readFileSync(0, 'utf8'));
	} catch {
		return {};
	}
}

function run(cmd) {
	try {
		return execSync(cmd, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] });
	} catch {
		return null;
	}
}

function pct(used, limit) {
	return Math.min(100, Math.round((used / limit) * 100));
}

function fmtTokens(n) {
	if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
	if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
	return `${n}`;
}

function fmtMin(ms) {
	const totalMin = Math.max(0, Math.round(ms / 60000));
	const h = Math.floor(totalMin / 60);
	const m = totalMin % 60;
	return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

function sundayOfThisWeek() {
	const now = new Date();
	const day = now.getDay();
	const sunday = new Date(now);
	sunday.setDate(now.getDate() - day);
	const y = sunday.getFullYear();
	const m = String(sunday.getMonth() + 1).padStart(2, '0');
	const d = String(sunday.getDate()).padStart(2, '0');
	return `${y}${m}${d}`;
}

function getContextUsage(transcriptPath, contextWindow) {
	if (!transcriptPath) return null;
	try {
		const lines = readFileSync(transcriptPath, 'utf8').trim().split('\n');
		for (let i = lines.length - 1; i >= 0; i--) {
			const line = lines[i];
			if (!line) continue;
			const entry = JSON.parse(line);
			const usage = entry?.message?.usage;
			if (usage) {
				const tokens =
					(usage.input_tokens || 0) +
					(usage.cache_read_input_tokens || 0) +
					(usage.cache_creation_input_tokens || 0);
				return { tokens, percent: pct(tokens, contextWindow) };
			}
		}
	} catch {}
	return null;
}

function getActiveBlock() {
	const raw = run('bunx ccusage claude blocks --active --json');
	if (!raw) return null;
	try {
		const data = JSON.parse(raw);
		const block = data.blocks?.find((b) => b.isActive);
		if (!block) return null;
		const remainingMs = new Date(block.endTime).getTime() - Date.now() - RESET_OFFSET_MS; // 10분 오차 하드코딩한 한 부분 (Reset_ooffset_ms)
		return {
			tokens: fmtTokens(block.totalTokens),
			percent: pct(block.totalTokens, LIMITS.blockTokens),
			resetIn: fmtMin(remainingMs),
		};
	} catch {
		return null;
	}
}

function getWeeklyUsage() {
	const since = sundayOfThisWeek();
	const raw = run(`bunx ccusage claude weekly --json -s ${since}`);
	if (!raw) return null;
	try {
		const data = JSON.parse(raw);
		const week = data.weekly?.find((w) => w.period === `${since.slice(0, 4)}-${since.slice(4, 6)}-${since.slice(6, 8)}`);
		const tokens = week ? week.totalTokens : data.totals?.totalTokens ?? 0;
		return { tokens: fmtTokens(tokens), percent: pct(tokens, LIMITS.weeklyTokens) };
	} catch {
		return null;
	}
}

const input = readStdin();
const modelName = input?.model?.display_name ?? 'Claude';
const contextWindow = 967_000; // matches /context's auto-compact window for this setup

const ctx = getContextUsage(input?.transcript_path, contextWindow);
const block = getActiveBlock();
const week = getWeeklyUsage();

const parts = [`🤖 ${modelName}`];
if (block) parts.push(`⏱ 5h: ${block.tokens} [${block.percent}%] (reset: ${block.resetIn})`);
if (week) parts.push(`📅 Week: ${week.tokens} [${week.percent}%]`);
if (ctx) parts.push(`🧠 Context: ${ctx.percent}%`);

process.stdout.write(parts.join(' | '));
