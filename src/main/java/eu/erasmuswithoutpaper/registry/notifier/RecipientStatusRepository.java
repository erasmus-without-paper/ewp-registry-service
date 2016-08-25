package eu.erasmuswithoutpaper.registry.notifier;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * A simple repository for our {@link RecipientStatus}es.
 */
@Repository
interface RecipientStatusRepository extends CrudRepository<RecipientStatus, String> {

}
