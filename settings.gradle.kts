// The whole repository as one composite build, for the IDE: open this directory to work on both
// builds and the sample app together. In it, the sample app uses the packages built from source
// here instead of published artifacts. tests/integration is not included: it must use only the
// published artifacts, so it is opened and run on its own (scripts/verify.sh runs every build on
// its own too).
rootProject.name = "a2ui-kmp-repository"
includeBuild("target/build-logic")
includeBuild("target/a2ui-kmp")
includeBuild("target/material3-a2ui-kmp")
includeBuild("samples")
