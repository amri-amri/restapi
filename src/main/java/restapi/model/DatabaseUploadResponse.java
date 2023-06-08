package restapi.model;

import org.springframework.hateoas.EntityModel;

import java.util.List;

/**
 * When traces are uploaded into the database, a DatabaseUploadResponse is returned to inform about
 * successful and unsuccessful uploads.
 * @param successes list of traces where upload succeeded
 * @param fails list of traces where upload failed
 */
public record DatabaseUploadResponse(List<EntityModel<Trace>> successes, List<DatabaseFailedUploadResponse> fails) {
    public record DatabaseFailedUploadResponse(String trace, String failMessage){}
}
