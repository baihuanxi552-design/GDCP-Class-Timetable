# Haze 1.5.4

Official Android artifact, bundled for reproducible offline builds:
https://repo.maven.apache.org/maven2/dev/chrisbanes/haze/haze-android/1.5.4/haze-android-1.5.4.aar

Project: https://github.com/chrisbanes/haze
Copyright Christopher Banes and the Haze project contributors.
License: Apache-2.0; see HAZE-LICENSE.txt.

Its Compose, Kotlin and AndroidX Activity dependencies are supplied by the existing app dependencies. The app keeps Compose 1.8.2 and Miuix 0.5.1.

The top effect uses hazeSource and hazeEffect with HazeProgressive.verticalGradient. Scroll content is the only blur source; the gradient tint, title and navigation controls are separate foreground layers.
