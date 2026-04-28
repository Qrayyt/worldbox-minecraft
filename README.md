# WorldBox Fabric 1.20.1

Fabric 1.20.1 prototype for a WorldBox/MineFortress/MineColonies/Towny-style autonomous civilization mod.

This repo is now treated as a real long-term mod base, not a fake one-file demo.

## Current prototype features

- `/worldbox create <city> [nation]` — create a city.
- `/worldbox list` — list cities.
- `/worldbox info <city>` — inspect population, resources, buildings, chunks and active construction.
- `/worldbox war <attacker> <defender>` — test city attack/capture.
- Persistent city state.
- City claims by chunks.
- Role-tagged citizens.
- Real-ish resource gathering: wood from logs, stone from stone/cobble, food from farmers.
- Builders must stand near a plot and consume delivered resources before placing phases.
- War can capture a city into the attacker's nation.

## Important design direction

The mod should follow this loop:

```text
city need -> task board -> worker chooses task -> worker moves -> worker performs world action -> city state changes
```

Not this:

```text
city tick -> magically add resources/building/soldiers
```

See `WORLDBOX_IMPLEMENTATION_PLAN.md` for the actual architecture.

## Build

```bash
gradle build --no-daemon
```

GitHub Actions is included in `.github/workflows/build.yml`.

## What still needs real implementation

- Custom `WorldBoxCitizenEntity` instead of vanilla villagers.
- Custom raider/soldier entity instead of vanilla pillagers.
- JSON/NBT blueprint loader.
- Task board and hauler system.
- Nation model and diplomacy relation matrix.
- Optional JourneyMap overlay with chunk polygons.
