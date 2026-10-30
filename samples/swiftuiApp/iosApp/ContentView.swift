import SwiftUI
import SwiftA2UI

@MainActor
final class SurfaceStore: ObservableObject {
    @Published var root: SwiftNode?
    @Published var events: [String] = []
    private var bridge: SwiftBridge?
    private(set) var surfaceId = ""

    func start(_ sample: String) {
        stop()
        surfaceId = ["Hello": "hello", "Form": "booking", "Updates": "order", "Custom": "order"][sample] ?? ""
        events = []
        let instance = SwiftBridge()
        bridge = instance
        instance.start(render: { [weak self] root in self?.root = root },
                       event: { [weak self] event in
                           self?.events.append("\(event.type) from \(event.componentId) on \(event.surfaceId): \(event.context)")
                       },
                       error: { [weak self] error in self?.events.append(error) })
        instance.startSample(name: sample)
    }
    func tap(_ id: String) { bridge?.tap(surfaceId: surfaceId, componentId: id) }
    func setString(_ path: String, _ value: String) { bridge?.setString(surfaceId: surfaceId, path: path, value: value) }
    func setChoice(_ path: String, _ value: String) { bridge?.setChoice(surfaceId: surfaceId, path: path, value: value) }
    func stop() { bridge?.close(); bridge = nil; root = nil }
}

struct NodeView: View {
    let node: SwiftNode
    @ObservedObject var store: SurfaceStore
    var body: some View {
        switch node.type {
        case "Card":
            VStack(alignment: .leading) { children }
                .padding(20).frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
        case "Column":
            VStack(alignment: .leading, spacing: 12) { children }
        case "Panel":
            VStack(alignment: .leading, spacing: 12) { children }
                .padding(20).frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(red: 0.965, green: 0.937, blue: 0.902), in: RoundedRectangle(cornerRadius: 16))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.brown.opacity(0.3)))
        case "Text", "Heading", "Note":
            Text(node.text).font(node.type == "Heading" || node.variant == "h3" ? .title.bold() : node.variant == "caption" ? .caption : .body)
                .accessibilityIdentifier(node.id)
        case "TextField":
            BoundTextField(node: node, store: store)
        case "ChoicePicker":
            VStack(alignment: .leading) {
                Text(node.text)
                HStack {
                    ForEach(node.options, id: \.self) { option in
                        Button(option) { store.setChoice(node.path, option) }
                            .buttonStyle(.bordered).tint(node.value == option ? .accentColor : .gray)
                            .accessibilityIdentifier("choice-\(option)")
                    }
                }
            }
        case "Button":
            Button(action: { store.tap(node.id) }) { children }
                .buttonStyle(.borderedProminent).accessibilityIdentifier(node.id)
        case "Action":
            Button(node.text) { store.tap(node.id) }
                .buttonStyle(.borderedProminent).tint(.brown).accessibilityIdentifier(node.id)
        default: EmptyView()
        }
    }
    private var children: some View {
        ForEach(node.children, id: \.id) { NodeView(node: $0, store: store) }
    }
}

struct BoundTextField: View {
    let node: SwiftNode
    @ObservedObject var store: SurfaceStore
    @State private var text: String
    @FocusState private var focused: Bool

    init(node: SwiftNode, store: SurfaceStore) {
        self.node = node
        self.store = store
        _text = State(initialValue: node.value)
    }
    var body: some View {
        TextField(node.text, text: $text)
            .focused($focused)
            .onChange(of: text) { store.setString(node.path, $0) }
            .onChange(of: node.value) { if !focused { text = $0 } }
            .textFieldStyle(.roundedBorder).accessibilityIdentifier(node.id)
    }
}

struct ContentView: View {
    private let tabs = ["Hello", "Form", "Updates", "Custom"]
    @State private var selected = "Hello"
    @StateObject private var store = SurfaceStore()
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                ForEach(tabs, id: \.self) { tab in
                    Button(tab) {
                        if selected != tab { selected = tab; store.start(tab) }
                    }
                        .frame(maxWidth: .infinity).padding(.vertical, 12)
                        .foregroundStyle(selected == tab ? Color.accentColor : Color.secondary)
                        .accessibilityIdentifier("tab-\(tab)")
                }
            }.overlay(alignment: .bottom) { Divider() }
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if selected == "Updates" { Button("Replay") { store.start(selected) }.buttonStyle(.bordered) }
                    if let root = store.root { NodeView(node: root, store: store) }
                    if selected == "Form" || selected == "Custom" {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Sent to the agent").font(.headline)
                            if store.events.isEmpty { Text("Nothing yet").foregroundStyle(.secondary) }
                            ForEach(Array(store.events.enumerated()), id: \.offset) { _, event in
                                Text(event).font(.system(.caption, design: .monospaced)).accessibilityIdentifier("event")
                            }
                        }.padding().frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(.tertiarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
                    }
                }.padding(16).frame(maxWidth: .infinity, alignment: .leading)
            }
        }.onAppear { store.start(selected) }.onDisappear { store.stop() }
    }
}
