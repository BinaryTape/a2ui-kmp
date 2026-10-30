# SwiftUI sample

This experimental app reproduces the repository's four Compose sample flows with native SwiftUI
views and the existing KMP `a2ui-model` and `a2ui-engine`: Hello, Form, Updates (including Replay),
and Custom. Form edits write to the engine data model and outgoing events include the timestamp, resolved JSON context, and optional client data model.
The fixed A2UI v0.9 messages are copied from `samples/composeApp` and should be kept in sync.

Open `iosApp.xcodeproj` and run the `iosApp` scheme on an Apple Silicon iOS simulator. Xcode invokes
Gradle to build the `SwiftA2UI` framework from source. Java 21, Xcode, and the repository's Gradle
dependencies are required. The scheme's UI test exercises all four pages; a captured form is
[here](screenshots/ios-form.png). Run the bridge tests with:

```sh
./gradlew -p target/a2ui-kmp :swiftui-bridge:desktopTest
```

The bridge implements the components and top-level data values used by these examples, not the
entire A2UI basic catalog. Its schemas are permissive, it renders the first surface, and it is for
these fixed trusted messages rather than arbitrary agent input. The SwiftUI controls use native
styling; they do not reproduce Material 3 pixel for pixel. The bridge is main-thread confined and
must be closed when its screen disappears; switching tabs creates a new bridge. Further work includes
validated catalog schemas, complete data paths, multiple surfaces, and the rest of the catalog.
