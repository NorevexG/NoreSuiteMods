# Nore Mods Suite

Source workspace for NoreTeams and NoreQuest.

## Modules

- `NoreTeams`: OPAC-backed teams and alliances used by the Nore modpack.
- `NoreQuest`: FTB Quests integration that adds Solo, Team, and Allied completion modes using NoreTeams, while suppressing FTB Teams gameplay surfaces.

## Dependencies

NoreTeams requires:

- Minecraft
- NeoForge
- Open Parties and Claims

NoreQuest requires:

- Minecraft
- NeoForge
- NoreTeams
- FTB Quests

FTB Quests provides its own dependency chain, including FTB Teams, FTB Library, and Architectury.

## Notes

Local dependency jars and run folders are intentionally not included in this repository. Add the required mod jars to the local development folders when building or running the workspace.
