# ID-Preserving Wayline Overwrite Implementation Plan

**Goal:** Replace an edited KMZ without changing its existing `wayline_id`.

**Boundary:** New route creation and Save As continue using the current import endpoint. Only the editor's Overwrite action uses the new backend replacement endpoint.

**Implementation:** Add a multipart `PUT` endpoint, upload the edited file to a unique versioned object key, update the existing `wayline_file` record in place, and remove the prior object after the database update. Replace the frontend delete-then-import sequence with this endpoint.

**Verification:** Unit-test that the service calls update but never insert/delete, compile the backend module, build the frontend, and inspect the final diff for API-boundary regressions.
