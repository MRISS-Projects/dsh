# Welcome to DSH REST API

## DSH Rest API

This REST API has three main methods:

1. **`submit`**: Upload a PDF or HTML file.
   - Receives the file.
   - Stores the file at MongoDB, creating a document in the collection with:
     - Token
     - Unique hash for the file
     - File contents
     - Keywords field
     - Most relevant sentences field
     - Success/Error message field
   - Queues a request for processing the file at RabbitMQ.
   - Returns the request token to the user.

2. **`getStatus`**: Probe the system using the token to get the processing status, returning:
   - **Status**: `InProgress`, `Success`, or `Error`
   - **Message**: error/success message (empty when status is `InProgress`)

3. **`getResult`**: When processing result is success, retrieves the resulting processing data:
   - List of keywords.
   - List of most relevant sentences associated with their paragraphs.

