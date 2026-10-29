import XCTest

/// The Form tab on iOS, as a user would use it: press Book, and the event for the agent appears.
final class FormUITests: XCTestCase {
    func testBookSendsTheFormToTheAgent() throws {
        let app = XCUIApplication()
        app.launchArguments = ["-tab", "Form"]
        app.launch()

        let book = app.buttons["Book"]
        XCTAssertTrue(book.waitForExistence(timeout: 30))
        book.tap()
        let event = app.staticTexts["book_table from submit on booking: {guest=Ada, party=[2]}"]
        XCTAssertTrue(event.waitForExistence(timeout: 10))

        let screenshot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.lifetime = .keepAlways
        add(attachment)
        // TEST_RUNNER_A2UI_SCREENSHOT_DIR=<dir> xcodebuild test ... also writes it there (README image).
        if let dir = ProcessInfo.processInfo.environment["A2UI_SCREENSHOT_DIR"] {
            try screenshot.pngRepresentation.write(to: URL(fileURLWithPath: dir).appendingPathComponent("ios-form.png"))
        }
    }
}
