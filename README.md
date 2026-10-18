# SleepVote

A lightweight sleep-voting plugin for Minecraft Paper servers.

SleepVote allows online players to vote to skip the night without requiring everyone to sleep.

## Features

- **Sleep voting:** Players can vote to skip the night using `/snvote`.
- **One vote per player:** Each player can only vote once per voting session.
- **30-second voting period:** Each vote lasts for 30 seconds.
- **Majority required:** More than half of the eligible players must vote in favor to skip the night.
- **Fixed voter list:** Eligible players are determined when the vote starts.
- **Weather clearing:** Clears rain and thunderstorms when the vote succeeds.
- **Fake player exclusion:** Excludes fake players whose UUIDs start with `fb07`, can be customized by modifying the source code.
- **Multi-world support:** Resets the time in all worlds when the vote succeeds.

## Commands

| Command | Description |
|---|---|
| `/snvote` | Start a vote to skip the night |

## How It Works

1. A player executes `/snvote`.
2. A 30-second voting session begins.
3. Eligible online players can cast their votes.
4. Players cannot vote more than once in the same session.
5. If more than half of the eligible players vote in favor, the night is skipped.
6. If the vote fails or times out, the world remains unchanged.

## Configuration

Drop chances, voting duration, and other settings depend on the implementation and configuration file included in your build.

## Requirements

- Minecraft server running Paper
- A compatible Java runtime for your server and plugin build

## Installation

1. Download or build the plugin JAR.
2. Place `SleepVote.jar` in the server's `plugins` directory.
3. Restart the server.
4. Run `/snvote` in-game to start a vote.

## Building from Source

Clone the repository:

```bash
git clone <repository-url>
cd SleepVote
```

Build with Gradle:

```bash
./gradlew build
```

The compiled plugin JAR will be available in:

```text
build/libs/
```

## License

Specify your preferred license in the repository before distributing the plugin.

---

*SleepVote: Less waiting, more playing.*