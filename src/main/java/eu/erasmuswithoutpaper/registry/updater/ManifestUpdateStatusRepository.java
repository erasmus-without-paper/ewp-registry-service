package eu.erasmuswithoutpaper.registry.updater;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * A simple repository for our {@link ManifestUpdateStatus} objects.
 */
@Repository
public interface ManifestUpdateStatusRepository
    extends CrudRepository<ManifestUpdateStatus, String> {

}
