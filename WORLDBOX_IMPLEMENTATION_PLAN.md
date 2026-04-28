# WorldBox-in-Minecraft Implementation Plan

This mod cannot be built as a pile of random villager goals. It needs a simulation core first, then rendering, then combat polish.

## References studied

- WorldBox: civilizations contain villages, villages belong to kingdoms, kingdoms can wage war, form alliances, and influence citizens. Citizens have jobs, jobs define behaviour tasks, and units are affected by stats, age, traits, and culture.
- MineFortress RTS: the important loop is workers gathering resources, assigning roles, constructing buildings through blueprints, training armies, and expanding territory.
- MineColonies: the important parts are builder/courier/warehouse automation, worker buildings, research trees, and request-based logistics instead of free resources.
- Towny: town/nation/claims/alliance/war layer. Territory is chunk/plot based, towns can belong to nations, and wars should operate over claimed plots/chunks.

## Non-negotiable design rules

1. No resources from air. Every resource must be produced by a worker action, looted in war, traded, or harvested from the world.
2. No instant buildings. Buildings are construction sites with materials, phases, and worker actions.
3. No fake wars. War must have attackers, defenders, objectives, casualties, capture rules, and post-war state changes.
4. No dependency lock-in. JourneyMap stays optional. If absent, simulation still works.
5. No one-tick city AI. City decisions should run on low-frequency ticks, while entity movement/animation runs normally.

## Core architecture

### Simulation layer

- `WorldBoxState`: persistent world state.
- `Nation`: name, capital city, culture, relations, war list, alliance list.
- `City`: center, claimed chunks, population, jobs, inventory, construction queue, military strength.
- `CitizenProfile`: UUID, age, role, home city, skill stats, inventory, hunger, current task.
- `Task`: gather, haul, build, farm, patrol, attack, defend, flee, trade.

The city must decide goals. Citizens only execute tasks.

### Economy loop

1. City planner detects needs: housing, food, storage, defense, roads.
2. It creates task requests.
3. Workers pick tasks by role.
4. Gatherers harvest real blocks/entities.
5. Haulers move items to city storage or construction site.
6. Builders consume construction-site inventory and place blueprint blocks.
7. City grows only after infrastructure exists.

### Building system

Buildings should be blueprints, not Java hardcoded cubes.

Planned format:

```json
{
  "id": "worldbox:wood_house_t1",
  "size": [7, 5, 7],
  "cost": {"wood": 42, "stone": 8},
  "phases": [
    {"name": "foundation", "blocks": [...]},
    {"name": "walls", "blocks": [...]},
    {"name": "roof", "blocks": [...]},
    {"name": "interior", "blocks": [...]}
  ]
}
```

Later this should support `.nbt` structures exported from Minecraft structure blocks.

### City planning

Use a district model:

- core: town hall, storage, market;
- residential ring: houses;
- production ring: farms, lumber yards, mines;
- defense ring: walls, towers, barracks;
- roads: connect functional buildings, not square spam.

Plot search must check:

- not on trees/leaves;
- not on water/lava;
- height difference <= 2;
- enough walkable space;
- no overlap with existing buildings;
- path to town center exists.

### War system

War should be a state machine:

1. Tension rises between nations.
2. War declaration creates objectives.
3. Attacker forms army from city/nation population.
4. Army marches in groups.
5. Defender creates defensive rally tasks.
6. Siege starts only inside claimed chunks.
7. Capture happens after objective score: kill defenders, hold center, destroy/occupy town hall.
8. Result: annex, vassalize, liberate, truce, or raze.

### Diplomacy

Relations should be numbers, not random booleans:

- -100 to -60: enemy;
- -59 to -20: hostile;
- -19 to 19: neutral;
- 20 to 59: friendly;
- 60 to 100: alliance.

Modifiers:

- border friction;
- trade;
- culture similarity;
- king personality;
- previous wars;
- population pressure;
- resource scarcity.

### JourneyMap integration

Optional module:

- detect if JourneyMap exists;
- render claimed chunks as polygons;
- color by nation;
- hover/click shows city info;
- no compile-time hard dependency until the API jar/repository is locked.

## Required dependencies

Base:

- Fabric Loader
- Fabric API
- Cardinal Components API or native PersistentState for world/city/citizen data
- GeckoLib only if custom animated entities are added
- Cloth Config + ModMenu later for config screens

Optional:

- JourneyMap API for overlays
- Architectury only if porting to Forge is planned later

## Implementation milestones

### M1: Simulation rewrite

- Add Nation model.
- Add relation matrix.
- Add construction queue.
- Add explicit storage.
- Add task board per city.

### M2: Real workers

- Replace vanilla villagers/pillagers with custom entities.
- Add role renderer/skins.
- Add citizen task execution.
- Add basic hunger/rest/work cycle.

### M3: Real building

- JSON/NBT blueprints.
- Construction sites.
- Hauler delivery.
- Builder placement animation.
- No instant structures.

### M4: War and capture

- Armies as groups.
- Defenders rally.
- Siege objective.
- Annex/vassal/truce outcomes.

### M5: Map/UI

- City info screen.
- Nation info screen.
- JourneyMap overlays.
- Debug commands.

## Why previous versions felt bad

The earlier code mixed city decisions and villager actions directly. That creates fake behaviour: buildings appear because city tick says so, soldiers walk because they have a target tag, resources appear because a counter increments. The correct architecture is task-driven: the city plans, workers execute, and the world changes only when an entity physically performs an action.
