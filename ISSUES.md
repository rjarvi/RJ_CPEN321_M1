# M1

## List of issues

### Issue 1: Profile Photo Upload

**Description**: Not implemented
The existing upload function only modified the local state, and did not connect to the backend endpoint.

**How it was fixed?**: Functions were added to upload the given image to the uploads/images folder, then linked to each account after the upload in mongodb.  

### Issue 2: Profile Delete

**Description**: The delete profile did not delete accounts, merely logging out the user based on local token storage

**How it was fixed?**: The backend endpoints were mostly functional already - small modifications were made for incorrect file path calculations. On the frontend, new interfaces and calls were added to properly use the endpoints, deleting users from the database and their associated profile photos. State was similarly cleared on logout, and i added a logout button that matched the original delete functionality.

### Issue 3: Bio box not editable

**Description**: The biography box on the profile manage page was not interactable.

**How it was fixed?**: 2 boolean flags just needed to be modified to allow the box to be interactable.

### Issue 4: File strucure

**Description**: Backend project was poorly structure with no file hiearchy and all files within the src folder

**How it was fixed?**: I sorted the backend folders according to their role, using folders to group controllers, middleware, routes, services, types, and utils. The rest of the files were left as they fulfilled individual unique roles.

...
