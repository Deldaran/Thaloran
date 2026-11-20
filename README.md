# Thaloran

Un monde naît sous tes yeux : plaques, montagnes, fleuves, climats. Puis cinq mille ans d'histoire s'y écrivent sans toi : peuples, villes, royaumes, guerres et dynasties. Alors tu y nais, dans une vraie famille et une vraie ville. Paysan, marchand, soldat ou futur roi, tu vis parmi des habitants qui travaillent, aiment, se souviennent et se vengent. Le passé, tu le découvres par les livres et les récits. Un RPG de vie dark fantasy en pixel art, où chaque monde est unique.

Jeu en Java 21 avec [libGDX](https://libgdx.com/). La conception du gameplay est décrite dans [docs/conception-gameplay.md](docs/conception-gameplay.md).

Les mondes du joueur sont rangés dans `~/Thaloran/mondes/`.

Projet créé à partir d'un modèle [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
