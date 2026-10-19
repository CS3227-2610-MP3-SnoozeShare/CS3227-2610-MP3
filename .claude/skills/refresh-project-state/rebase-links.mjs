#!/usr/bin/env node
// Rebase relative markdown links in a file that is moving, so they still resolve.
// Usage: node rebase-links.mjs <old-path> <new-path>   (both repo-relative, forward slashes)
// Reads <new-path> (already moved), rewrites `](target)` links in place, prints a summary.
// Absolute URLs, #anchors and /rooted paths are left alone. Code spans and fences are skipped.
import { readFileSync, writeFileSync } from 'node:fs'
import { posix } from 'node:path'

const [oldPath, newPath] = process.argv.slice(2)
if (!oldPath || !newPath) {
  console.error('usage: rebase-links.mjs <old-path> <new-path>')
  process.exit(2)
}

const oldDir = posix.dirname(oldPath.replaceAll('\\', '/'))
const newDir = posix.dirname(newPath.replaceAll('\\', '/'))
const external = /^([a-z][a-z0-9+.-]*:|#|\/)/i

let changed = 0
let inFence = false
const out = readFileSync(newPath, 'utf8')
  .split('\n')
  .map((line) => {
    if (/^\s*(```|~~~)/.test(line)) inFence = !inFence
    if (inFence) return line
    // split on code spans so `[x](y)` inside backticks is untouched
    return line
      .split(/(`[^`]*`)/)
      .map((part) =>
        part.startsWith('`')
          ? part
          : part.replace(/\]\(([^)\s]+)((?:\s+"[^"]*")?)\)/g, (m, target, title) => {
              if (external.test(target)) return m
              const [file, anchor] = target.split('#')
              const resolved = posix.normalize(posix.join(oldDir, file))
              const rebased = posix.relative(newDir, resolved) || '.'
              changed++
              return `](${rebased}${anchor ? '#' + anchor : ''}${title})`
            }),
      )
      .join('')
  })
  .join('\n')

writeFileSync(newPath, out)
console.log(`${newPath}: ${changed} link(s) rebased from ${oldDir || '.'} to ${newDir || '.'}`)
