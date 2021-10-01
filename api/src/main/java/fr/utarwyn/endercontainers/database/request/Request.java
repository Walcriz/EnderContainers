package fr.utarwyn.endercontainers.database.request;

/**
 * Request interface.
 *
 * @author Utarwyn
 * @since 2.2.0
 */
public interface Request {

    /**
     * Generate the final request to be executed on the database
     *
     * @return generated request
     */
    String getRequest();

    /**
     * Return all attributes added to this request
     *
     * @return List of attributes objects
     */
    Object[] getAttributes();

}
