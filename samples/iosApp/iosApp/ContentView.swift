import ComposeApp
import SwiftUI
import UIKit

/// The shared Compose UI (composeApp, MainViewController.kt) inside a SwiftUI app.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // `-tab Form` as a launch argument opens that tab first.
        MainViewControllerKt.MainViewController(initialTab: UserDefaults.standard.string(forKey: "tab") ?? "Hello")
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea()
    }
}
