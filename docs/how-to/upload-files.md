# How-to: Upload Files

DocTester supports multipart file uploads via `addFileToUpload(String param, File file)`.

## Upload a single file

```java
import java.io.File;

File avatar = new File("src/test/resources/test-avatar.png");

Response response = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/users/42/avatar"))
        .addFileToUpload("avatar", avatar));

sayAndAssertThat("Upload accepted", 200, equalTo(response.httpStatus()));
```

## Upload a file with additional form fields

Combine `addFileToUpload` with `addFormParameter`:

```java
File document = new File("src/test/resources/test-report.pdf");

Response response = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/documents"))
        .addFormParameter("title", "Q4 Report")
        .addFormParameter("category", "finance")
        .addFileToUpload("file", document));

sayAndAssertThat("Document uploaded", 201, equalTo(response.httpStatus()));
```

## Upload multiple files

Chain multiple `addFileToUpload` calls:

```java
File photo1 = new File("src/test/resources/photo1.jpg");
File photo2 = new File("src/test/resources/photo2.jpg");

Response response = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/gallery"))
        .addFileToUpload("photos", photo1)
        .addFileToUpload("photos", photo2));

sayAndAssertThat("Both photos uploaded", 201, equalTo(response.httpStatus()));
```

## Complete example with documentation

```java
@Test
public void testFileUpload() {

    sayNextSection("File Upload API");

    say("The /api/uploads endpoint accepts multipart form data. "
        + "The file field name is `file`. "
        + "Supported formats: JPEG, PNG, PDF (max 10MB).");

    File testFile = new File("src/test/resources/sample.pdf");

    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/uploads"))
            .addFormParameter("description", "Sample document")
            .addFileToUpload("file", testFile));

    sayAndAssertThat("Upload returns 201 Created", 201, equalTo(response.httpStatus()));

    say("The response body contains the upload metadata including the assigned ID "
        + "and the URL to retrieve the file:");

    record UploadResult(Long id, String url, String filename, Long size) {}
    UploadResult result = response.payloadAs(UploadResult.class);

    sayAndAssertThat("Upload has an ID", result.id(), notNullValue());
    sayAndAssertThat("Download URL provided", result.url(), notNullValue());
}
```

## Notes

- `addFileToUpload` automatically sets `Content-Type: multipart/form-data`
- Do not set `contentTypeApplicationJson()` or `contentTypeApplicationXml()` when uploading files
- The file must exist at test runtime; use `src/test/resources/` for test fixtures
