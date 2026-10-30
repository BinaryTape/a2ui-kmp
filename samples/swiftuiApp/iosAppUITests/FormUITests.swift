import XCTest

final class FormUITests: XCTestCase {
    func testAllSamples() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.staticTexts["Hello from your agent"].waitForExistence(timeout: 20))
        app.buttons["tab-Form"].tap()
        XCTAssertTrue(app.staticTexts["Book a table"].waitForExistence(timeout: 10))
        let name = app.textFields["name"]
        XCTAssertTrue(name.waitForExistence(timeout: 5))
        name.tap()
        name.typeText(" Lovelace")
        app.buttons["choice-3"].tap()
        app.buttons["submit"].tap()
        let event = app.staticTexts["event"]
        XCTAssertTrue(event.waitForExistence(timeout: 10))
        XCTAssertTrue(event.label.contains("book_table"))
        XCTAssertTrue(event.label.contains("Ada Lovelace"))
        XCTAssertTrue(event.label.contains("\"party\":[\"3\"]"))
        app.buttons["tab-Form"].tap()
        XCTAssertTrue(app.staticTexts["event"].exists)
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "SwiftUI Form"
        screenshot.lifetime = .keepAlways
        add(screenshot)
        app.buttons["tab-Updates"].tap()
        XCTAssertTrue(app.staticTexts["On its way"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["Arrives in about 10 minutes"].waitForExistence(timeout: 5))
        app.buttons["Replay"].tap()
        XCTAssertTrue(app.staticTexts["Order received"].waitForExistence(timeout: 5))
        app.buttons["tab-Custom"].tap()
        XCTAssertTrue(app.staticTexts["Ready now, at the counter"].waitForExistence(timeout: 10))
        app.buttons["confirm"].tap()
        XCTAssertTrue(app.staticTexts["event"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["event"].label.contains("confirm_order"))
        app.buttons["tab-Hello"].tap()
        XCTAssertTrue(app.staticTexts["Hello from your agent"].waitForExistence(timeout: 5))
    }
}
