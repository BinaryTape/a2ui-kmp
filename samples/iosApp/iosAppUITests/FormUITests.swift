import XCTest

/// The Form tab on iOS, as a user would use it: press Book, and the event for the agent appears.
final class FormUITests: XCTestCase {
    func testBookSendsTheFormToTheAgent() throws {
        let app = XCUIApplication()
        app.launchArguments = ["-tab", "Form"]
        app.launch()

        // Generous: a CI simulator on a busy machine can take a minute to show the first frame.
        let book = app.buttons["Book"]
        XCTAssertTrue(book.waitForExistence(timeout: 90), "The Form tab did not show its Book button")
        book.tap()
        let event = app.staticTexts["book_table from submit on booking: {guest=Ada, party=[2]}"]
        XCTAssertTrue(event.waitForExistence(timeout: 30), "No event for the agent after pressing Book")

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
