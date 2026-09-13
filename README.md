# BattleroyalSpawn / CartDrop

Handles the start of a battle royale round on the cartprac server: queue in chat, countdown, flying cart
over the arena, elytra drop, then hands off to the actual game. Spigot 1.21.11, Java 21.

It doesn't do combat, loot or the zone. Hook `PlayerLandEvent` / `GameStateChangeEvent` for that.

## How a round goes

- Players get a `[CLICK TO JOIN]` message on login (or `/br`). Clicking puts them in the queue, they stay
  where they are.
- Once `min-players` are queued, everyone in the queue gets a bossbar countdown. Hits 0 -> they get teleported
  into the arena world already sitting on the cart.
- Cart flies from point A to B. After `door-open-after-seconds` you can sneak to drop. Whoever is still on it
  at `door-close-at` (90% of the path by default) gets kicked off.
- You glide with an elytra you can't take off / drop / boost. No fall damage. When you're on the ground
  for a few ticks you get your chestplate back and a few seconds of grace.
- When everyone is down the state goes `LIVE` and the plugin stops caring. Stays LIVE until `/br stop`
  or `CartDropAPI.stopGame()`, so the game plugin has to end the round.

Queue is open the whole time, people who join mid-round go into the next one.

## Setup

```
./mvnw clean package        (mvnw.cmd on windows)
```

jar ends up in `target/`. Put it in plugins, start once, edit `plugins/CartDrop/config.yml`.

The arena world has to be loaded by something else (multiverse etc), the plugin doesn't create worlds.
Then in the arena:

```
/br setpath a
/br setpath b      (also sets map.world)
/br altitude 150
/br setreturn      (optional, see return-mode)
/br reload
```

## Config

Everything is in config.yml with comments. The bits you'll actually touch:

- `queue.*` – min/max players, countdown lengths, whether the join message shows on login,
  `return-mode` (`PREVIOUS` = back where they were, `LOCATION` = fixed spot)
- `map.*` – world, path points, altitude. `random-bearing: true` picks a random line through `center` each round
- `bus.*` – speed (blocks/tick), door timings, `jump-key` (SNEAK or RIGHT_CLICK), seat layout, the rig
- `bus.rig.blocks` – the visible cart, list of block boxes in cart-local coords (x sideways, y up, z forward).
  Default floor is 11x8, fits 10 seats per row x 6 rows. Change the seats, change the floor.
- `bus.seats.y-offset` – you will need to tune this until people sit on the floor and not in it
- `drop.*` – safety timer, landing confirm ticks, grace seconds
- `sounds.*` – `BLOCK_IRON_DOOR_OPEN` style names or `block.iron_door.open`, `NONE` to turn off
- `messages.*` – all the text

## Commands

`/battleroyale` or `/br`

| | |
|---|---|
| `/br` | show the join message |
| `/br join` / `/br leave` | queue. leave while seated = jump, while gliding = you get sent back |
| `/br start [force]` | start countdown, force ignores min players / skips a running countdown |
| `/br stop` | kill the round, remove everything, send everyone back |
| `/br setpath <a\|b>` `/br setcenter` `/br setreturn` `/br altitude <n>` | config from where you stand |
| `/br forcejump <player\|all>` | kick off the cart |
| `/br announce` | broadcast the join message |
| `/br state` | debug dump |
| `/br reload` | only works while idle |

`cartdrop.play` (default true), `cartdrop.admin` (default op).

## API

`depend: [CartDrop]` in your plugin.yml.

Events in `com.cartprac.battleroyalspawn.api.events`: `GameStateChangeEvent`, `BusDepartEvent`,
`PlayerJumpFromBusEvent`, `PlayerLandEvent`, `GameResetEvent`.

```java
@EventHandler
public void onLand(PlayerLandEvent e) {
    Player p = e.getPlayer();
    // give kit, mark as alive, whatever
}

@EventHandler
public void onState(GameStateChangeEvent e) {
    if (e.getNewState() == GameState.LIVE) {
        startZone(CartDropAPI.get().getParticipants());
    }
}

// when the round ends
CartDropAPI.get().stopGame(false); // false = don't teleport people back
```

`CartDropAPI` also has `getState()`, `getQueue()`, `isAboard`, `isInDrop`, `isGraced`, `forceJump`, `startGame`.

## Notes / gotchas

- Seats are armor stands moved with velocity every tick, not teleported. Spigot's `teleport()` just returns
  false when the entity has a passenger. Gravity has to stay on and marker off or they ignore velocity.
  (Paper has a RETAIN_PASSENGERS teleport flag if this ever moves to Paper.)
- The cart itself is display entities teleported with `teleportDuration=1` so the client interpolates.
- `jump-key: JUMP` isn't doable on Spigot, no input event for seated players. Falls back to SNEAK.
- Chunks ahead of the cart are kept loaded with plugin chunk tickets, released on every reset.
- The minecart ItemDisplay is a flat sprite because that's what the item model is. The block rig is the actual
  cart shape, disable the item if it looks dumb.
- Someone who disconnects mid-flight gets teleported back on next login (`returns.yml`). Can't cross-world
  teleport inside the quit event.
- Dying mid-drop takes you out of the round, respawn is up to the game plugin.
- 60 players = 60 velocity packets per tick per viewer. Fine on a normal server but it's the main cost.
- Right-click jump mode also blocks sneak dismount, nothing we can do about the client sending it.

## Testing checklist

2 accounts, `min-players: 2`, path points ~400 blocks apart.

1. **normal round** – both click join, countdown shows for both, at 0 both are on the cart in the arena.
   `Doors open in 3s` on the action bar, then `Hold SHIFT to drop`. Sneak -> `DROP!`, elytra on, glide, no fall
   damage. Land -> `LANDED`, chestplate back, can't be hit for 3s. Second player lands -> `LIVE` in `/br state`,
   `seatEntities=0 rigParts=0 chunkTickets=0`. `/br stop` -> both back where they started.
2. **disconnect on the cart** – B disconnects while seated. No errors, `/br state` doesn't list B. B rejoins ->
   gets teleported back to their spot.
3. **end of path** – nobody sneaks, bossbar hits 0 -> `End of the line`, both kicked off gliding, cart gone.
4. **water landing** – glide into a lake, should confirm landing within a second, elytra removed.
5. **mountain / void** – into a cliff = no wall damage, lands normally. Into the void = lifted back up once with
   a console warning, second time gets put on the nearest ground or sent back.
6. **/br stop mid-flight** – one gliding, one seated, `/br stop` from console. Both back, no elytra, no armor
   stands or displays left in the arena.
7. **/reload mid-flight** – both seated, `/reload confirm`. Console says `Match reset (disable)`, both back, plugin
   comes back idle, queue again and it works.
8. **bad config** – `bus.speed: -1`, `sounds.jump: NOPE`, `/br reload` -> two warnings, still works.
