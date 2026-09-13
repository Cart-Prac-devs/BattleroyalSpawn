# CartDrop (BattleroyalSpawn)

Spigot 1.21.11 plugin that runs the opening phase of a battle-royale match on the CartPrac server:
a clickable queue in chat, a boss-bar countdown, a flying "battle cart" made of display entities that
carries every player across the arena world, sneak-to-drop with an elytra glider, landing detection, and a
clean hand-off to the rest of the game through Bukkit events and a small API.

CartDrop owns **only** the queue and the drop. It does not do combat, loot or the zone.

## Flow

1. **IDLE** - players click `[CLICK TO JOIN]` in chat (shown on login, by `/br`, or by `/br announce`).
   They stay wherever they are. The queue is always open, even during a match.
2. **COUNTDOWN** - once `queue.min-players` are queued, queued players see a boss bar counting down
   (`countdown-seconds`, shortened to `countdown-short-seconds` when `max-players` is reached).
   The countdown cancels if the queue shrinks below the minimum.
3. **BUS** - at zero, up to `max-players` are teleported into `map.world` straight onto their seats on the cart,
   which is already flying from point A to point B at `map.altitude`. Doors open after
   `door-open-after-seconds`; sneaking (or right-click, see `jump-key`) drops the player.
   Anyone still aboard at `door-close-at` of the path is force-dropped and the cart despawns.
4. **DROP** - each dropped player glides on an unbreakable elytra that cannot be moved, dropped or boosted with
   rockets. No fall damage. Landing is confirmed after `landing-confirm-ticks` on the ground or in water, the
   original chest item comes back, and `grace-seconds` of two-way protection start.
5. **LIVE** - everyone has landed (or the `max-drop-seconds` safety net put them down). CartDrop fires
   `GameStateChangeEvent(DROP -> LIVE)` and stops managing players. The rest of the game takes over.
   The state stays LIVE until `/br stop` or `CartDropAPI.stopGame(...)` resets it to IDLE, so the next
   countdown only starts once your game plugin ends the match.

## Install

1. Build with `./mvnw -q clean package` (Windows: `mvnw.cmd -q clean package`). The jar is
   `target/CartDrop-1.0.0.jar`. Java 21 is required to build; the server needs Java 21+.
2. Drop the jar into `plugins/`, start the server once, stop it, and edit `plugins/CartDrop/config.yml`.
3. Make sure the arena world named in `map.world` is loaded (Multiverse, a world-management plugin, or a
   small plugin that calls `WorldCreator`). CartDrop does not create or load worlds.
4. Stand at the two ends of the flight line in the arena world and run `/br setpath a` and `/br setpath b`
   (this also sets `map.world`). Optionally `/br altitude 180` and `/br setreturn`.
5. `/br reload`, then `/br join` on two accounts (or `/br start force` with one) to test.

## Config walkthrough (`config.yml`)

Every value is validated on load; a bad value logs
`config.yml: <path> must be ..., got <value>. Using default <default>.` and the plugin keeps running.

| Section | What to look at |
|---|---|
| `queue` | `min-players`, `max-players`, the two countdown lengths, `announce-*` for the clickable message, and `return-mode` (`PREVIOUS` = back to where they were picked up, `LOCATION` = `return-location`). |
| `map` | `world`, `point-a`/`point-b`, or `random-bearing: true` with `center` + `radius` for a different straight line each match. `altitude` is clamped to the world height minus 10. |
| `bus` | `speed` in blocks per tick, door timings, `jump-key` (`SNEAK` or `RIGHT_CLICK`), jump velocity, chunk window, seat layout (`seats.*`) and the visual rig (`rig.*`). |
| `bus.rig` | Data-driven: an optional scaled item (the minecart sprite), an optional banner item, and any number of block boxes (`block`, `offset` = centre of the bottom face, `size`). Local axes: x sideways, y up, z forward. Restyle freely. |
| `drop` | `max-drop-seconds` safety net, `landing-confirm-ticks`, `grace-seconds`, `cancel-wall-damage`, and the off-by-default `glide-speed-cap`. |
| `hud` | Boss bar title/colour/style per phase, action bar toggle, title timings, particle bursts. |
| `sounds` | Any 1.21 sound as `BLOCK_IRON_DOOR_OPEN` or `block.iron_door.open`; `NONE` disables. Unknown names are reported on load. |
| `messages` | Every player-facing string, `&` colour codes, `{placeholder}` values. |

Seat layout: seats are laid out `seats.per-row` wide with `seats.spacing` between seats and
`seats.row-spacing` between rows, centred on the cart origin. The default rig floor is 11 x 8 blocks, enough for
10 seats per row and 6 rows (60 players). If you change the seat layout, resize the floor to match.
`seats.y-offset` moves the players up or down relative to the floor; tune it until they sit on the planks.

## Commands (`/battleroyale`, alias `/br`)

| Command | Permission | Effect |
|---|---|---|
| `/br` | `cartdrop.play` | Show the clickable join message |
| `/br join` | `cartdrop.play` | Join the queue (what the chat button runs) |
| `/br leave` | `cartdrop.play` | Leave the queue; on the cart with doors open this is a jump; while gliding/landed it returns you |
| `/br start [force]` | `cartdrop.admin` | Start the countdown; `force` skips the minimum check, or skips a running countdown to zero |
| `/br stop` | `cartdrop.admin` | Abort, remove every cart entity, return everyone, back to IDLE (queue is kept) |
| `/br setreturn` | `cartdrop.admin` | Set `return-location` to your position |
| `/br setpath <a\|b>` | `cartdrop.admin` | Set a path point (and `map.world`) from your position |
| `/br setcenter` | `cartdrop.admin` | Set the random-bearing centre (and `map.world`) from your position |
| `/br altitude <n>` | `cartdrop.admin` | Set flight altitude |
| `/br forcejump <player\|all>` | `cartdrop.admin` | Eject from the cart |
| `/br announce` | `cartdrop.admin` | Broadcast the clickable join message to everyone not yet queued |
| `/br state` | `cartdrop.admin` | Debug: state, queue, per-player phases, entity and chunk-ticket counts |
| `/br reload` | `cartdrop.admin` | Reload config.yml; refused while a match is running |

`cartdrop.play` defaults to everyone, `cartdrop.admin` to ops.

## API

Declare `depend: [CartDrop]` in your plugin.yml. Events (all in `com.cartprac.battleroyalspawn.api.events`):

- `GameStateChangeEvent(oldState, newState)` - cancellable for IDLE->COUNTDOWN and COUNTDOWN->BUS.
- `BusDepartEvent(path, participants)`
- `PlayerJumpFromBusEvent(player, location, forced)` - cancellable unless forced.
- `PlayerLandEvent(player, location, airtimeTicks, safetyNet)`
- `GameResetEvent(reason)`

```java
import com.cartprac.battleroyalspawn.api.CartDropAPI;
import com.cartprac.battleroyalspawn.api.GameState;
import com.cartprac.battleroyalspawn.api.events.GameStateChangeEvent;
import com.cartprac.battleroyalspawn.api.events.PlayerLandEvent;

public final class GameHook implements Listener {

    @EventHandler
    public void onLand(PlayerLandEvent event) {
        Player player = event.getPlayer();
        // give the starter kit, enable the zone for this player, etc.
        player.sendMessage("You landed after " + event.getAirtimeTicks() / 20 + "s");
    }

    @EventHandler
    public void onState(GameStateChangeEvent event) {
        if (event.getNewState() == GameState.LIVE) {
            startZone(CartDropAPI.get().getParticipants());
        }
    }

    // When your match ends:
    void endMatch() {
        CartDropAPI.get().stopGame(false); // false = leave players where they are
    }
}
```

`CartDropAPI` also has `getState()`, `getQueue()`, `isQueued`, `isAboard`, `isInDrop`, `isGraced`,
`startGame()` (forced countdown), `forceJump(Player)`, `joinQueue`, `leaveQueue`.

## Assumptions

- Package `com.cartprac.battleroyalspawn`, plugin name `CartDrop`, artifact `battleroyalspawn`.
- Participation is opt-in through the queue; players who never clicked are never touched.
- The arena is a different world from wherever players queue. Players keep their inventory; only the chest
  slot is swapped for the glider and restored on landing (or dropped/restored on death).
- One match at a time. Players who queue during a match wait for the next one.
- The Maven wrapper is committed because the build machine had no Maven installed.
- `LIVE` persists until your game plugin (or `/br stop`) resets. CartDrop never resets itself after LIVE.
- A player who disconnects mid-flight is returned to their return location on their next login
  (`returns.yml`); cross-world teleports during the quit event are not safe.
- Death mid-drop removes the player from the match. Their respawn is your game plugin's business.

## Spigot-specific decisions

- **Seats are moved with velocity, not teleport.** Spigot's `Entity#teleport` returns false for any entity that
  has passengers, so a seat cannot be teleported with its player mounted. Each seat is an invisible small armor
  stand with gravity on and marker off (armor stands with either flag set ignore velocity), and each tick its
  velocity is set to exactly the delta to its target position. Paper users could switch to
  `teleport(loc, TeleportFlag.EntityState.RETAIN_PASSENGERS)`.
- **The rig is teleported with interpolation.** Display entities have no passengers, so they are teleported
  every tick with `teleportDuration = 1`; the client slides them smoothly. All parts sit at the cart origin and
  carry their offset in their transformation, so the entity yaw rotates the rig as one piece.
- **`jump-key: JUMP` is not possible on Spigot.** A seated player's jump key arrives as an input packet that
  Bukkit does not expose (Paper has `PlayerInputEvent`). JUMP falls back to SNEAK with a warning.
- **Chunks use plugin chunk tickets** (`World#addPluginChunkTicket`) in a sliding window, released per plugin on
  every reset, so nothing can stay force-loaded.
- Deprecated APIs avoided: `Sound.valueOf` (sounds are resolved through `Registry.SOUNDS`),
  `TextComponent.fromLegacyText` (uses `fromLegacy`), `Player#isOnGround` (called through `Entity`, backed by
  block checks).

## Known limitations

- The minecart `ItemDisplay` is a flat sprite (that is what the item model is). The block rig gives the 3D cart;
  disable the item or replace it with a custom-model item if you have a resource pack.
- Every seat sends a velocity packet each tick to every viewer. With 60 players clustered on the cart that is a
  few thousand small packets per tick server-wide, which modern servers handle, but it is the main cost.
- Landing on the highest block after the safety net can put a player on a tree or on water.
- `Player#isOnGround` is client-reported; block checks back it up, but a hacked client could delay its landing.
- The countdown boss bar is only shown to queued players; non-queued players only get the chat message.
- Right-click jump mode also blocks sneaking from dismounting; sneak mode cannot be turned off client-side.

## Manual test plan

Setup: a Spigot 1.21.11 server with the arena world loaded, `min-players: 2`, two accounts (A, B), A is op.
Run `/br setpath a` and `/br setpath b` roughly 400 blocks apart in the arena, `/br altitude 150`.

1. **Happy path**
   1. Both accounts stand in the hub world. Log in: after 2 seconds both see `[CLICK TO JOIN]`.
   2. A clicks. Expect `You joined the queue (1/2 needed)` and a `[click to leave]` line.
   3. B clicks. Expect `Enough players! The cart leaves in 30s`, a yellow boss bar counting down, chat lines and
      titles at 30, 20, 10, 5..1 with sounds.
   4. At 0 both are in the arena world sitting on the cart at Y=150, the cart is moving, a red boss bar shows
      `Forced drop in Ns`, the action bar reads `Doors open in 3s`, then `Hold SHIFT to drop - 2 still on the cart`.
   5. A sneaks. Expect `DROP!` title, elytra on the chest, gliding, no fall damage, action bar with altitude and
      distance. `/br state` shows A `DROPPING glider`, B `ABOARD`.
   6. A lands. Expect `LANDED`, the elytra gone and the previous chest item back, 3 s where A cannot take or deal
      damage. Console logs nothing per tick.
   7. B sneaks and lands. Expect `Everyone has landed. Fight!`, `/br state` shows `state=LIVE`, no boss bar,
      `seatEntities=0 rigParts=0 chunkTickets=0`.
   8. `/br stop`. Expect both back where they stood in step 1, `state=IDLE`.
2. **Disconnect mid-bus**
   1. Start a match; while both are seated, B disconnects.
   2. Expect no error, `/br state` no longer lists B, A's action bar shows `1 still on the cart`.
   3. B reconnects. Expect B to be teleported back to their pre-match spot with `You were returned...`.
3. **Force-drop at the end of the path**
   1. Start a match; nobody sneaks. Wait until the boss bar reaches 0.
   2. Expect `End of the line, everyone out!`, an explosion sound, both gliding, the cart gone
      (`/br state`: `rigParts=0 seatEntities=0`).
4. **Landing in water**
   1. Glide into a lake. Expect the landing to confirm within a second of touching water, elytra removed, no damage.
5. **Mountain vs void**
   1. Glide into a steep mountain face. Expect no wall-crash damage, landing confirmed once on the ground.
   2. Configure `map.point-b` beyond the world border / over a void hole (or `/br altitude -60` on a void
      map) and drop into the void. Expect `You fell out of the world and were lifted back up`, a console warning
      naming the coordinates, and gliding again from altitude. Falling in again puts you on the nearest
      highest block or sends you back if the column is pure void.
6. **`/br stop` mid-flight**
   1. Start a match, let A jump, leave B seated. Run `/br stop` from console.
   2. Expect both players returned, A without elytra and with the original chest item, no armor stands or
      displays left in the arena (`/kill @e[type=armor_stand]` style checks find nothing, or use `/br state`).
7. **`/reload` mid-flight**
   1. Start a match with both seated. Run `/reload confirm`.
   2. Expect the console to log `Match reset (disable)`, both players back at their return spots, no leftover
      entities, and after the reload the plugin enabled with state IDLE and `0 orphaned` (or a line saying how
      many were cleaned up).
   3. Queue again and confirm a fresh match starts normally.
8. **Config validation**
   1. Set `bus.speed: -1` and `sounds.jump: NOT_A_SOUND`, `/br reload`.
   2. Expect two console warnings in the `config.yml: ... Using default ...` format and the plugin still working.
