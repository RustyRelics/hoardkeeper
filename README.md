# Hoardkeeper

A Hytale server mod that stacks items from the player's inventory into nearby chests that already
contain that item, on a single activation — no intermediary auto-chest, no dragging. Unlike existing
"auto sort" mods that require putting items *into* a special container which then distributes them,
Hoardkeeper pulls straight from your inventory: you walk up, activate, and your loose sticks are in
the stick chest. The `/hoardkeeper` (`/hk`) commands do this from wherever you're standing; place a
**Hoardstone** — a craftable block — for a `Use`-to-stack station that does the same thing without a
command.

## Config

Hoardkeeper writes `hoardkeeper.json` the first time it starts (never overwriting one that's already
there) with four settings:

| Field | Default | What it does |
|---|---|---|
| `DefaultRadius` | `14` | How far `/hk stack`, `/hk near`, `/hk exclude`, and the Hoardstone search when no `--radius` is given. |
| `MaxRadius` | `32` | The largest radius a `--radius` flag can push a command to; anything higher is capped, and the command says so. |
| `Particles` | `true` | Whether the Hoardstone shows a particle burst on a successful stack. |
| `Sound` | `true` | Whether the Hoardstone plays a sound on use, success or not. |

Bad values are corrected rather than rejected — an operator will see a warning in the server log
naming the field, the value that was wrong, and what got used instead, and the server keeps running.

**Where the file lives depends on how the server runs**, because the engine resolves a plugin's data
directory relative to the server process's own working directory, not to the world:

- **Singleplayer**: `saves/<world name>/mods/RustyRelic_hoardkeeper/hoardkeeper.json` — the working
  directory is the save folder, so every world effectively gets its own copy.
- **Dedicated server**: `mods/RustyRelic_hoardkeeper/hoardkeeper.json`, next to the server's own
  working directory — shared by every world that server hosts.

Status: v0.2 — config + block feedback.
